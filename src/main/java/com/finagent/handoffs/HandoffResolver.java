package com.finagent.handoffs;

import com.finagent.api.Agent;
import com.finagent.api.Handoff;

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
