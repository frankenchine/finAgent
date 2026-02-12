package com.finagent.core;

import com.finagent.api.*;
import com.finagent.handoffs.HandoffResolver;
import com.finagent.handoffs.HandoffToolAdapter;
import com.finagent.model.Message;
import com.finagent.model.ModelInvocationRequest;
import com.finagent.model.ModelInvocationResponse;
import com.finagent.tools.ToolExecutor;
import com.finagent.tools.ToolInvocation;
import com.finagent.tools.ToolSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of AgentRunner. Runs the agent loop: LLM call -> final output / handoff / tool calls.
 */
public class DefaultAgentRunner implements AgentRunner {

    private final ModelInvoker modelInvoker;

    public DefaultAgentRunner(ModelInvoker modelInvoker) {
        this.modelInvoker = modelInvoker;
    }

    @Override
    public RunResult run(Agent agent, RunRequest request) {
        int maxTurns = request.getMaxTurns();
        List<Message> messages = new ArrayList<>(request.getInputAsMessages());

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

        if (currentTurn == 0 && !currentAgent.getInputGuardrails().isEmpty()) {
            for (InputGuardrail g : currentAgent.getInputGuardrails()) {
                InputGuardrail.InputGuardrailResult result = g.process(messages, runContext);
                inputGuardrailResults.add(result);
                if (!result.isPassed()) {
                    return RunResult.builder()
                            .input(request.getInput())
                            .lastAgent(currentAgent)
                            .rawResponses(rawResponses)
                            .finalOutput("Input rejected: " + result.getRejectReason())
                            .inputGuardrailResults(inputGuardrailResults)
                            .maxTurns(maxTurns)
                            .currentTurn(0)
                            .build();
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

            ModelInvocationRequest invRequest = new ModelInvocationRequest(systemPrompt, messages, toolSpecs);
            ModelInvocationResponse invResponse = modelInvoker.invoke(invRequest);

            // Save assistant message with tool_calls if present
            if (invResponse.hasToolCalls()) {
                List<Message.ToolCallInfo> toolCallInfos = invResponse.getToolCalls().stream()
                        .map(tc -> new Message.ToolCallInfo(tc.getId(), tc.getName(), tc.getArgumentsJson()))
                        .collect(Collectors.toList());
                rawResponses.add(Message.assistant(invResponse.getAssistantText(), toolCallInfos));
            } else {
                rawResponses.add(Message.assistant(invResponse.getAssistantText()));
            }

            // 如果模型返回的工具调用列表为空，则认为模型返回的是最终输出
            if (!invResponse.hasToolCalls()) {
                Object finalOutput = invResponse.getAssistantText();
                List<Object> outputGuardrailResults = new ArrayList<>();
                for (OutputGuardrail g : currentAgent.getOutputGuardrails()) {
                    OutputGuardrail.OutputGuardrailResult result = g.process(finalOutput, runContext);
                    outputGuardrailResults.add(result);
                    if (!result.isPassed()) {
                        finalOutput = "Output rejected: " + result.getRejectReason();
                    } else if (result.getOutput() != null) {
                        finalOutput = result.getOutput();
                    }
                }

                if (request.getSession() != null) {
                    List<Message> toAdd = new ArrayList<>(request.getInputAsMessages());
                    toAdd.add(Message.assistant(invResponse.getAssistantText()));
                    request.getSession().addItems(toAdd);
                }

                return RunResult.builder()
                        .input(request.getInput())
                        .lastAgent(currentAgent)
                        .rawResponses(rawResponses)
                        .finalOutput(finalOutput)
                        .inputGuardrailResults(inputGuardrailResults)
                        .outputGuardrailResults(outputGuardrailResults)
                        .maxTurns(maxTurns)
                        .currentTurn(currentTurn)
                        .build();
            }

            // 如果模型返回的工具调用列表不为空，则认为模型返回的是工具调用（包括handoff工具调用）
            // 需要先判断是否是handoff工具调用，如果是则切换到目标agent，否则执行工具调用
            HandoffResolver handoffResolver = new HandoffResolver(currentAgent.getHandoffs());
            ModelInvocationResponse.ToolCall firstCall = invResponse.getToolCalls().get(0);

            if (handoffResolver.isHandoffTool(firstCall.getName())) {
                Agent nextAgent = handoffResolver.resolveByToolName(firstCall.getName()).orElseThrow();
                currentAgent = nextAgent;
                // Save assistant message with tool_calls for handoff
                List<Message.ToolCallInfo> toolCallInfos = invResponse.getToolCalls().stream()
                        .map(tc -> new Message.ToolCallInfo(tc.getId(), tc.getName(), tc.getArgumentsJson()))
                        .collect(Collectors.toList());
                messages.add(Message.assistant(invResponse.getAssistantText(), toolCallInfos));
                // Add tool messages for handoff (required by OpenAI API)
                // Handoff tool calls need tool responses even though they don't execute actual tools
                for (ModelInvocationResponse.ToolCall tc : invResponse.getToolCalls()) {
                    // For handoff, we add a tool message indicating the handoff was successful
                    messages.add(Message.tool("handoff_completed", tc.getId()));
                }
                continue;
            }

            ToolExecutor executor = new ToolExecutor(currentAgent.getTools());
            // Save assistant message with tool_calls
            List<Message.ToolCallInfo> toolCallInfos = invResponse.getToolCalls().stream()
                    .map(tc -> new Message.ToolCallInfo(tc.getId(), tc.getName(), tc.getArgumentsJson()))
                    .collect(Collectors.toList());
            messages.add(Message.assistant(invResponse.getAssistantText(), toolCallInfos));

            for (ModelInvocationResponse.ToolCall tc : invResponse.getToolCalls()) {
                ToolInvocation inv = new ToolInvocation(tc.getId(), tc.getName(), tc.getArgumentsJson());
                Object toolResult = executor.execute(inv, runContext);
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

        return RunResult.builder()
                .input(request.getInput())
                .lastAgent(currentAgent)
                .rawResponses(rawResponses)
                .finalOutput(lastText)
                .inputGuardrailResults(inputGuardrailResults)
                .maxTurns(maxTurns)
                .currentTurn(currentTurn)
                .build();
    }
}
