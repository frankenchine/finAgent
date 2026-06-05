/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.core;

import com.agent4j.api.Agent;
import com.agent4j.api.AgentRunner;
import com.agent4j.api.InputGuardrail;
import com.agent4j.api.OutputGuardrail;
import com.agent4j.api.RunConfig;
import com.agent4j.api.RunEvent;
import com.agent4j.api.RunHooks;
import com.agent4j.api.RunRequest;
import com.agent4j.api.RunResult;
import com.agent4j.handoffs.HandoffResolver;
import com.agent4j.handoffs.HandoffToolAdapter;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;
import com.agent4j.model.ModelStreamEvent;
import com.agent4j.tools.ToolExecutor;
import com.agent4j.tools.ToolInvocation;
import com.agent4j.tools.ToolSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of AgentRunner. Runs the loop: LLM call -> final output / handoff / tool calls.
 */
public class DefaultAgentRunner implements AgentRunner {

    private final ModelInvoker modelInvoker;
    private final StructuredOutputParser outputParser = new StructuredOutputParser();

    public DefaultAgentRunner(ModelInvoker modelInvoker) {
        this.modelInvoker = modelInvoker;
    }

    @Override
    public RunResult run(Agent agent, RunRequest request) {
        return run(agent, request, RunConfig.defaults());
    }

    @Override
    public RunResult run(Agent agent, RunRequest request, RunConfig config) {
        RunConfig runConfig = config != null ? config : RunConfig.defaults();
        int maxTurns = request.getMaxTurns();
        List<Message> messages = new ArrayList<>(request.getInputAsMessages());
        emit(runConfig, RunEvent.of(RunEvent.Type.RUN_STARTED, agent, "run", request.getInput(), 0));

        if (request.getSession() != null) {
            List<Message> history = request.getSession().getItems(null);
            if (!history.isEmpty()) {
                messages = new ArrayList<>(history);
                messages.addAll(request.getInputAsMessages());
            }
        }

        Agent currentAgent = agent;
        List<Message> rawResponses = new ArrayList<>();
        List<Object> inputGuardrailResults = new ArrayList<>();
        int currentTurn = 0;
        Object runContext = request.getContext();
        emit(runConfig, RunEvent.of(RunEvent.Type.AGENT_STARTED, currentAgent, currentAgent.getName(), null, 0));

        if (!currentAgent.getInputGuardrails().isEmpty()) {
            for (InputGuardrail g : currentAgent.getInputGuardrails()) {
                InputGuardrail.InputGuardrailResult result = g.process(messages, runContext);
                inputGuardrailResults.add(result);
                emitGuardrail(runConfig, currentAgent, "input", result, currentTurn);
                if (!result.isPassed()) {
                    RunResult runResult = RunResult.builder()
                            .input(request.getInput())
                            .lastAgent(currentAgent)
                            .rawResponses(rawResponses)
                            .finalOutput("Input rejected: " + result.getRejectReason())
                            .inputGuardrailResults(inputGuardrailResults)
                            .maxTurns(maxTurns)
                            .currentTurn(0)
                            .build();
                    emit(runConfig, RunEvent.of(RunEvent.Type.RUN_COMPLETED, currentAgent, "run", runResult, 0));
                    return runResult;
                }
                messages = new ArrayList<>(result.getMessages());
            }
        }

        while (currentTurn < maxTurns) {
            currentTurn++;

            List<ModelInvocationRequest.ToolSpec> toolSpecs = new ArrayList<>();
            toolSpecs.addAll(currentAgent.getTools().stream().map(ToolSchema::fromTool).collect(Collectors.toList()));
            toolSpecs.addAll(HandoffToolAdapter.toToolSpecs(currentAgent.getHandoffs()));

            String systemPrompt = currentAgent.getInstructions() != null ? currentAgent.getInstructions() : "";
            ModelInvocationRequest invRequest = new ModelInvocationRequest(
                    systemPrompt,
                    messages,
                    toolSpecs,
                    runConfig.getModelSettings()
            );

            emit(runConfig, RunEvent.of(RunEvent.Type.MODEL_STARTED, currentAgent, "model", invRequest, currentTurn));
            ModelInvocationResponse invResponse = invokeModel(currentAgent, runConfig, invRequest, currentTurn);
            emit(runConfig, RunEvent.of(RunEvent.Type.MODEL_COMPLETED, currentAgent, "model", invResponse, currentTurn));

            if (invResponse.hasToolCalls()) {
                List<Message.ToolCallInfo> toolCallInfos = invResponse.getToolCalls().stream()
                        .map(tc -> new Message.ToolCallInfo(tc.getId(), tc.getName(), tc.getArgumentsJson()))
                        .collect(Collectors.toList());
                rawResponses.add(Message.assistant(invResponse.getAssistantText(), toolCallInfos));
            } else {
                rawResponses.add(Message.assistant(invResponse.getAssistantText()));
            }

            if (!invResponse.hasToolCalls()) {
                List<Object> outputGuardrailResults = new ArrayList<>();
                Object finalOutput = processFinalOutput(
                        currentAgent,
                        runConfig,
                        invResponse,
                        runContext,
                        currentTurn,
                        outputGuardrailResults
                );

                if (request.getSession() != null) {
                    List<Message> toAdd = new ArrayList<>(request.getInputAsMessages());
                    toAdd.add(Message.assistant(invResponse.getAssistantText()));
                    request.getSession().addItems(toAdd);
                }

                RunResult runResult = RunResult.builder()
                        .input(request.getInput())
                        .lastAgent(currentAgent)
                        .rawResponses(rawResponses)
                        .finalOutput(finalOutput)
                        .inputGuardrailResults(inputGuardrailResults)
                        .outputGuardrailResults(outputGuardrailResults)
                        .maxTurns(maxTurns)
                        .currentTurn(currentTurn)
                        .build();
                emit(runConfig, RunEvent.of(RunEvent.Type.RUN_COMPLETED, currentAgent, "run", runResult, currentTurn));
                return runResult;
            }

            HandoffResolver handoffResolver = new HandoffResolver(currentAgent.getHandoffs());
            ModelInvocationResponse.ToolCall firstCall = invResponse.getToolCalls().get(0);

            if (handoffResolver.isHandoffTool(firstCall.getName())) {
                Agent nextAgent = handoffResolver.resolveByToolName(firstCall.getName()).orElseThrow();
                emit(runConfig, RunEvent.of(RunEvent.Type.HANDOFF, currentAgent, nextAgent.getName(), nextAgent, currentTurn));
                currentAgent = nextAgent;
                emit(runConfig, RunEvent.of(RunEvent.Type.AGENT_STARTED, currentAgent, currentAgent.getName(), null, currentTurn));

                List<Message.ToolCallInfo> toolCallInfos = invResponse.getToolCalls().stream()
                        .map(tc -> new Message.ToolCallInfo(tc.getId(), tc.getName(), tc.getArgumentsJson()))
                        .collect(Collectors.toList());
                messages.add(Message.assistant(invResponse.getAssistantText(), toolCallInfos));
                for (ModelInvocationResponse.ToolCall tc : invResponse.getToolCalls()) {
                    messages.add(Message.tool("handoff_completed", tc.getId()));
                }
                continue;
            }

            ToolExecutor executor = new ToolExecutor(currentAgent.getTools(), runConfig.getToolExecutionConfig());
            List<Message.ToolCallInfo> toolCallInfos = invResponse.getToolCalls().stream()
                    .map(tc -> new Message.ToolCallInfo(tc.getId(), tc.getName(), tc.getArgumentsJson()))
                    .collect(Collectors.toList());
            messages.add(Message.assistant(invResponse.getAssistantText(), toolCallInfos));

            for (ModelInvocationResponse.ToolCall tc : invResponse.getToolCalls()) {
                ToolInvocation inv = new ToolInvocation(tc.getId(), tc.getName(), tc.getArgumentsJson());
                emit(runConfig, RunEvent.of(RunEvent.Type.TOOL_STARTED, currentAgent, tc.getName(), inv, currentTurn));
                Object toolResult = executor.execute(inv, runContext);
                emit(runConfig, RunEvent.of(RunEvent.Type.TOOL_COMPLETED, currentAgent, tc.getName(), toolResult, currentTurn));
                String resultStr = toolResult != null ? toolResult.toString() : "";
                messages.add(Message.tool(resultStr, tc.getId()));
            }
        }

        String lastText = rawResponses.isEmpty() ? "" : rawResponses.get(rawResponses.size() - 1).getText();
        if (request.getSession() != null) {
            List<Message> toAdd = new ArrayList<>(request.getInputAsMessages());
            toAdd.add(Message.assistant(lastText));
            request.getSession().addItems(toAdd);
        }

        Object finalOutput = outputParser.parse(lastText, resolveOutputType(currentAgent, runConfig));
        RunResult runResult = RunResult.builder()
                .input(request.getInput())
                .lastAgent(currentAgent)
                .rawResponses(rawResponses)
                .finalOutput(finalOutput)
                .inputGuardrailResults(inputGuardrailResults)
                .maxTurns(maxTurns)
                .currentTurn(currentTurn)
                .build();
        emit(runConfig, RunEvent.of(RunEvent.Type.RUN_COMPLETED, currentAgent, "run", runResult, currentTurn));
        return runResult;
    }

    private Object processFinalOutput(Agent agent, RunConfig config, ModelInvocationResponse response,
                                      Object runContext, int turn, List<Object> outputGuardrailResults) {
        Object finalOutput = response.getAssistantText();
        boolean rejected = false;
        for (OutputGuardrail g : agent.getOutputGuardrails()) {
            OutputGuardrail.OutputGuardrailResult result = g.process(finalOutput, runContext);
            outputGuardrailResults.add(result);
            emitGuardrail(config, agent, "output", result, turn);
            if (!result.isPassed()) {
                finalOutput = "Output rejected: " + result.getRejectReason();
                rejected = true;
            } else if (result.getOutput() != null) {
                finalOutput = result.getOutput();
            }
        }
        if (rejected) {
            return finalOutput;
        }
        return outputParser.parse(finalOutput, resolveOutputType(agent, config));
    }

    private ModelInvocationResponse invokeModel(Agent agent, RunConfig config, ModelInvocationRequest request, int turn) {
        if (!config.isStreamModel() || !request.getToolSpecs().isEmpty()) {
            return modelInvoker.invoke(request);
        }
        return modelInvoker.invokeStream(request, event -> {
            if (event.getType() == ModelStreamEvent.Type.DELTA) {
                emit(config, RunEvent.of(RunEvent.Type.MODEL_DELTA, agent, "model", event.getDelta(), turn));
            }
        });
    }

    private Class<?> resolveOutputType(Agent agent, RunConfig config) {
        if (config.getOutputType() != null) {
            return config.getOutputType();
        }
        return agent.getOutputType();
    }

    private void emitGuardrail(RunConfig config, Agent agent, String name, Object result, int turn) {
        emit(config, RunEvent.of(RunEvent.Type.GUARDRAIL, agent, name, result, turn));
    }

    private void emit(RunConfig config, RunEvent event) {
        if (config.getEventConsumer() != null) {
            config.getEventConsumer().accept(event);
        }
        RunHooks hooks = config.getHooks();
        if (hooks == null) {
            return;
        }
        switch (event.getType()) {
            case RUN_STARTED:
                hooks.onRunStart(event);
                break;
            case RUN_COMPLETED:
                hooks.onRunComplete(event);
                break;
            case MODEL_STARTED:
                hooks.onModelStart(event);
                break;
            case MODEL_COMPLETED:
                hooks.onModelComplete(event);
                break;
            case MODEL_DELTA:
                hooks.onModelDelta(event);
                break;
            case TOOL_STARTED:
                hooks.onToolStart(event);
                break;
            case TOOL_COMPLETED:
                hooks.onToolComplete(event);
                break;
            case HANDOFF:
                hooks.onHandoff(event);
                break;
            case GUARDRAIL:
                hooks.onGuardrail(event);
                break;
            default:
                hooks.onEvent(event);
                break;
        }
    }
}
