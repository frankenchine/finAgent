/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.sse;

import com.agent4j.api.Agent;
import com.agent4j.api.AgentRunner;
import com.agent4j.api.AgentStreamEvent;
import com.agent4j.api.RunConfig;
import com.agent4j.api.RunEvent;
import com.agent4j.api.RunRequest;
import com.agent4j.api.RunResult;
import com.agent4j.core.AgentDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSseServiceTest {

    @Test
    void streamReturnsEmitterAndEnablesModelStreaming() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RunConfig> capturedConfig = new AtomicReference<>();
        Agent agent = new AgentDefinition().setName("assistant").build();
        AgentRunner runner = new AgentRunner() {
            @Override
            public RunResult run(Agent agent, RunRequest request) {
                throw new UnsupportedOperationException();
            }

            @Override
            public RunResult run(Agent agent, RunRequest request, RunConfig config) {
                capturedConfig.set(config);
                config.getEventConsumer().accept(RunEvent.of(RunEvent.Type.MODEL_DELTA, agent, "model", "hi", 1));
                RunResult result = RunResult.builder()
                        .input(request.getInput())
                        .lastAgent(agent)
                        .rawResponses(List.of())
                        .finalOutput("hi")
                        .maxTurns(request.getMaxTurns())
                        .currentTurn(1)
                        .build();
                config.getEventConsumer().accept(RunEvent.of(RunEvent.Type.RUN_COMPLETED, agent, "run", result, 1));
                latch.countDown();
                return result;
            }
        };

        SseEmitter emitter = new AgentSseService(runner).stream(agent, RunRequest.builder().input("hello").build());

        assertThat(emitter).isNotNull();
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedConfig.get().isStreamModel()).isTrue();
    }

    @Test
    void agentStreamEventMapsModelDeltaAndFailureNames() {
        Agent agent = new AgentDefinition().setName("assistant").build();

        AgentStreamEvent delta = AgentStreamEvent.fromRunEvent(
                RunEvent.of(RunEvent.Type.MODEL_DELTA, agent, "model", "hi", 1));
        AgentStreamEvent failed = AgentStreamEvent.fromRunEvent(
                RunEvent.of(RunEvent.Type.RUN_FAILED, agent, "run", new IllegalStateException("boom"), 1));

        assertThat(delta.getType()).isEqualTo("model_delta");
        assertThat(delta.getDelta()).isEqualTo("hi");
        assertThat(failed.getType()).isEqualTo("run_failed");
        assertThat(failed.getError()).isEqualTo("boom");
    }
}
