/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.sse;

import com.agent4j.api.Agent;
import com.agent4j.api.AgentRunner;
import com.agent4j.api.AgentStreamEvent;
import com.agent4j.api.RunConfig;
import com.agent4j.api.RunEvent;
import com.agent4j.api.RunRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Spring MVC adapter that exposes agent runner events as Server-Sent Events.
 */
public class AgentSseService {

    private static final long DEFAULT_TIMEOUT_MILLIS = 0L;

    private final AgentRunner agentRunner;

    public AgentSseService(AgentRunner agentRunner) {
        this.agentRunner = agentRunner;
    }

    public SseEmitter stream(Agent agent, RunRequest request) {
        return stream(agent, request, RunConfig.defaults());
    }

    public SseEmitter stream(Agent agent, RunRequest request, RunConfig config) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
        RunConfig streamConfig = toStreamConfig(config, event -> send(emitter, event));

        CompletableFuture.runAsync(() -> {
            try {
                agentRunner.run(agent, request, streamConfig);
                emitter.complete();
            } catch (Exception e) {
                send(emitter, RunEvent.of(RunEvent.Type.RUN_FAILED, agent, "run", e, 0));
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private RunConfig toStreamConfig(RunConfig config, Consumer<RunEvent> sseConsumer) {
        RunConfig base = config != null ? config : RunConfig.defaults();
        Consumer<RunEvent> existingConsumer = base.getEventConsumer();
        Consumer<RunEvent> combinedConsumer = event -> {
            if (existingConsumer != null) {
                existingConsumer.accept(event);
            }
            sseConsumer.accept(event);
        };

        return RunConfig.builder()
                .modelSettings(base.getModelSettings())
                .outputType(base.getOutputType())
                .hooks(base.getHooks())
                .toolExecutionConfig(base.getToolExecutionConfig())
                .eventConsumer(combinedConsumer)
                .streamModel(true)
                .build();
    }

    private void send(SseEmitter emitter, RunEvent event) {
        AgentStreamEvent streamEvent = AgentStreamEvent.fromRunEvent(event);
        try {
            emitter.send(SseEmitter.event()
                    .name(streamEvent.getType())
                    .data(streamEvent));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send agent SSE event", e);
        }
    }
}
