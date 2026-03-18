/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

/**
 * Strategy interface that decides which model(s) should handle a given request.
 */
public interface LlmRoutingStrategy {

    /**
     * Decide which model(s) to use for this invocation based on the routing context.
     */
    LlmRoutingDecision decide(RoutingContext context);
}

