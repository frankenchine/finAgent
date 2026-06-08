/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Runs an agent with the given request. Executes the agent loop until final output or max turns.
 */
public interface AgentRunner {

    RunResult run(Agent agent, RunRequest request);

    default RunResult run(Agent agent, RunRequest request, RunConfig config) {
        return run(agent, request);
    }

    default RunResult runStream(Agent agent, RunRequest request, Consumer<RunEvent> eventConsumer) {
        return run(agent, request, RunConfig.builder()
                .eventConsumer(eventConsumer)
                .streamModel(true)
                .build());
    }

    default CompletableFuture<RunResult> runAsync(Agent agent, RunRequest request) {
        return CompletableFuture.supplyAsync(() -> run(agent, request));
    }

    default CompletableFuture<RunResult> runAsync(Agent agent, RunRequest request, RunConfig config) {
        return CompletableFuture.supplyAsync(() -> run(agent, request, config));
    }
}

