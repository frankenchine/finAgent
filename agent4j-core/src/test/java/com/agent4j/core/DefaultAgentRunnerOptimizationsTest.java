/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.core;

import com.agent4j.api.Agent;
import com.agent4j.api.ModelSettings;
import com.agent4j.api.OutputGuardrail;
import com.agent4j.api.RunConfig;
import com.agent4j.api.RunEvent;
import com.agent4j.api.RunHooks;
import com.agent4j.api.RunRequest;
import com.agent4j.api.RunResult;
import com.agent4j.api.Tool;
import com.agent4j.api.ToolExecutionConfig;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;
import com.agent4j.model.ModelStreamEvent;
import com.agent4j.tools.FunctionToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAgentRunnerOptimizationsTest {

    @Test
    void passesRunConfigModelSettingsToInvoker() {
        CapturingInvoker invoker = new CapturingInvoker(new ModelInvocationResponse("ok", List.of()));
        DefaultAgentRunner runner = new DefaultAgentRunner(invoker);
        Agent agent = new AgentDefinition().setName("assistant").build();

        runner.run(agent, RunRequest.builder().input("hello").build(), RunConfig.builder()
                .modelSettings(ModelSettings.builder()
                        .model("test-model")
                        .temperature(0.2)
                        .maxTokens(64)
                        .toolChoice("auto")
                        .build())
                .build());

        assertThat(invoker.requests).hasSize(1);
        assertThat(invoker.requests.get(0).getModelSettings().getModel()).isEqualTo("test-model");
        assertThat(invoker.requests.get(0).getModelSettings().getTemperature()).isEqualTo(0.2);
        assertThat(invoker.requests.get(0).getModelSettings().getMaxTokens()).isEqualTo(64);
        assertThat(invoker.requests.get(0).getModelSettings().getToolChoice()).isEqualTo("auto");
    }

    @Test
    void parsesStructuredOutputFromAgentOutputType() {
        CapturingInvoker invoker = new CapturingInvoker(new ModelInvocationResponse("{\"answer\":\"yes\"}", List.of()));
        DefaultAgentRunner runner = new DefaultAgentRunner(invoker);
        Agent agent = new AgentDefinition().setName("assistant").setOutputType(Answer.class).build();

        RunResult result = runner.run(agent, RunRequest.builder().input("question").build());

        assertThat(result.getFinalOutput()).isInstanceOf(Answer.class);
        assertThat(((Answer) result.getFinalOutput()).answer).isEqualTo("yes");
    }

    @Test
    void emitsEventsToStreamConsumerAndHooks() {
        CapturingInvoker invoker = new CapturingInvoker(new ModelInvocationResponse("done", List.of()));
        DefaultAgentRunner runner = new DefaultAgentRunner(invoker);
        Agent agent = new AgentDefinition().setName("assistant").build();
        List<RunEvent.Type> streamEvents = new ArrayList<>();
        List<RunEvent.Type> hookEvents = new ArrayList<>();

        runner.run(agent, RunRequest.builder().input("hello").build(), RunConfig.builder()
                .eventConsumer(event -> streamEvents.add(event.getType()))
                .hooks(new RunHooks() {
                    @Override
                    public void onEvent(RunEvent event) {
                        hookEvents.add(event.getType());
                    }
                })
                .build());

        assertThat(streamEvents).contains(
                RunEvent.Type.RUN_STARTED,
                RunEvent.Type.MODEL_STARTED,
                RunEvent.Type.MODEL_COMPLETED,
                RunEvent.Type.RUN_COMPLETED
        );
        assertThat(hookEvents).contains(RunEvent.Type.MODEL_STARTED, RunEvent.Type.MODEL_COMPLETED);
    }

    @Test
    void runStreamUsesEventConsumer() {
        CapturingInvoker invoker = new CapturingInvoker(new ModelInvocationResponse("done", List.of()));
        DefaultAgentRunner runner = new DefaultAgentRunner(invoker);
        Agent agent = new AgentDefinition().setName("assistant").build();
        List<RunEvent.Type> events = new ArrayList<>();

        RunResult result = runner.runStream(agent, RunRequest.builder().input("hello").build(),
                event -> events.add(event.getType()));

        assertThat(result.getFinalOutput()).isEqualTo("done");
        assertThat(events).contains(RunEvent.Type.RUN_STARTED, RunEvent.Type.RUN_COMPLETED);
    }

    @Test
    void runStreamEmitsModelDeltaEvents() {
        CapturingInvoker invoker = new CapturingInvoker();
        invoker.streamDeltas = List.of("he", "llo");
        DefaultAgentRunner runner = new DefaultAgentRunner(invoker);
        Agent agent = new AgentDefinition().setName("assistant").build();
        List<Object> deltas = new ArrayList<>();

        RunResult result = runner.runStream(agent, RunRequest.builder().input("hello").build(), event -> {
            if (event.getType() == RunEvent.Type.MODEL_DELTA) {
                deltas.add(event.getPayload());
            }
        });

        assertThat(invoker.streamInvocations).isEqualTo(1);
        assertThat(deltas).containsExactly("he", "llo");
        assertThat(result.getFinalOutput()).isEqualTo("hello");
    }

    @Test
    void runStreamExecutesToolCallsReturnedFromStream() {
        CapturingInvoker invoker = new CapturingInvoker(
                new ModelInvocationResponse("", List.of(new ModelInvocationResponse.ToolCall("call-1", "ping", "{}"))),
                new ModelInvocationResponse("done", List.of())
        );
        DefaultAgentRunner runner = new DefaultAgentRunner(invoker);
        Tool tool = FunctionToolRegistry.noArgTool("ping", "ping", () -> "pong");
        Agent agent = new AgentDefinition().setName("assistant").addTool(tool).build();
        List<RunEvent.Type> events = new ArrayList<>();

        RunResult result = runner.runStream(agent, RunRequest.builder().input("use tool").build(), event -> events.add(event.getType()));

        assertThat(invoker.streamInvocations).isEqualTo(2);
        assertThat(result.getFinalOutput()).isEqualTo("done");
        assertThat(events).contains(RunEvent.Type.MODEL_DELTA, RunEvent.Type.TOOL_STARTED, RunEvent.Type.TOOL_COMPLETED);
    }

    @Test
    void runStreamEmitsFailureEventsAndRethrowsModelErrors() {
        CapturingInvoker invoker = new CapturingInvoker();
        invoker.streamError = new IllegalStateException("model down");
        DefaultAgentRunner runner = new DefaultAgentRunner(invoker);
        Agent agent = new AgentDefinition().setName("assistant").build();
        List<RunEvent.Type> events = new ArrayList<>();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                runner.runStream(agent, RunRequest.builder().input("hello").build(), event -> events.add(event.getType())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("model down");

        assertThat(events).contains(RunEvent.Type.MODEL_FAILED, RunEvent.Type.RUN_FAILED);
    }

    @Test
    void outputGuardrailRejectionStillCompletesRun() {
        CapturingInvoker invoker = new CapturingInvoker(new ModelInvocationResponse("bad", List.of()));
        DefaultAgentRunner runner = new DefaultAgentRunner(invoker);
        Agent agent = new AgentDefinition().setName("assistant")
                .addOutputGuardrail((output, context) -> OutputGuardrail.OutputGuardrailResult.reject("nope"))
                .build();
        List<RunEvent.Type> events = new ArrayList<>();

        RunResult result = runner.runStream(agent, RunRequest.builder().input("hello").build(), event -> events.add(event.getType()));

        assertThat(result.getFinalOutput()).isEqualTo("Output rejected: nope");
        assertThat(events).contains(RunEvent.Type.GUARDRAIL, RunEvent.Type.RUN_COMPLETED);
    }

    @Test
    void formatsToolErrorsWithRunConfig() {
        CapturingInvoker invoker = new CapturingInvoker(
                new ModelInvocationResponse("", List.of(new ModelInvocationResponse.ToolCall("call-1", "fail", "{}"))),
                new ModelInvocationResponse("done", List.of())
        );
        DefaultAgentRunner runner = new DefaultAgentRunner(invoker);
        Tool tool = FunctionToolRegistry.noArgTool("fail", "fail", () -> {
            throw new IllegalStateException("boom");
        });
        Agent agent = new AgentDefinition().setName("assistant").addTool(tool).build();

        runner.run(agent, RunRequest.builder().input("use tool").build(), RunConfig.builder()
                .toolExecutionConfig(ToolExecutionConfig.builder()
                        .errorFormatter((error, context) -> "TOOL_ERROR:" + error.getToolName() + ":" + error.getMessage())
                        .build())
                .build());

        List<Message> secondTurnMessages = invoker.requests.get(1).getMessages();
        assertThat(secondTurnMessages.get(secondTurnMessages.size() - 1).getText()).isEqualTo("TOOL_ERROR:fail:boom");
    }

    static final class Answer {
        public String answer;
    }

    private static final class CapturingInvoker implements ModelInvoker {
        private final Queue<ModelInvocationResponse> responses = new ArrayDeque<>();
        private final List<ModelInvocationRequest> requests = new ArrayList<>();
        private List<String> streamDeltas = List.of();
        private RuntimeException streamError;
        private int streamInvocations;

        private CapturingInvoker(ModelInvocationResponse... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public ModelInvocationResponse invoke(ModelInvocationRequest request) {
            requests.add(request);
            return responses.remove();
        }

        @Override
        public ModelInvocationResponse invokeStream(ModelInvocationRequest request, Consumer<ModelStreamEvent> consumer) {
            requests.add(request);
            streamInvocations++;
            if (streamError != null) {
                throw streamError;
            }
            StringBuilder text = new StringBuilder();
            for (String delta : streamDeltas) {
                text.append(delta);
                consumer.accept(ModelStreamEvent.delta(delta));
            }
            ModelInvocationResponse response;
            if (text.length() > 0) {
                response = new ModelInvocationResponse(text.toString(), List.of());
            } else {
                response = responses.isEmpty() ? new ModelInvocationResponse("", List.of()) : responses.remove();
                if (!response.hasToolCalls() && response.getAssistantText() != null && !response.getAssistantText().isEmpty()) {
                    consumer.accept(ModelStreamEvent.delta(response.getAssistantText()));
                }
            }
            consumer.accept(ModelStreamEvent.completed(response));
            return response;
        }
    }
}
