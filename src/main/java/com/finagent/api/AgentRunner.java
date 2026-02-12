package com.finagent.api;

import java.util.concurrent.CompletableFuture;

/**
 * Runs an agent with the given request. Executes the agent loop until final output or max turns.
 */
public interface AgentRunner {

    RunResult run(Agent agent, RunRequest request);

    default CompletableFuture<RunResult> runAsync(Agent agent, RunRequest request) {
        return CompletableFuture.supplyAsync(() -> run(agent, request));
    }
}
