/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.handoffs;

import com.agent4j.api.Agent;
import com.agent4j.api.Handoff;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a tool call name to a Handoff (target agent). Used by the runner to switch agents.
 */
public class HandoffResolver {

    private final List<Handoff> handoffs;

    public HandoffResolver(List<Handoff> handoffs) {
        this.handoffs = handoffs != null ? List.copyOf(handoffs) : List.of();
    }

    public Optional<Agent> resolveByToolName(String toolName) {
        return handoffs.stream()
                .filter(h -> toolName != null && toolName.equals(h.getToolName()))
                .map(Handoff::getTargetAgent)
                .findFirst();
    }

    public boolean isHandoffTool(String toolName) {
        return resolveByToolName(toolName).isPresent();
    }
}

