package com.agent4j.handoffs;

import com.agent4j.api.Handoff;
import com.agent4j.model.ModelInvocationRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapts Handoff definitions into ToolSpecs that the model can call.
 */
public final class HandoffToolAdapter {

    private HandoffToolAdapter() {
    }

    public static List<ModelInvocationRequest.ToolSpec> toToolSpecs(List<Handoff> handoffs) {
        if (handoffs == null || handoffs.isEmpty()) {
            return List.of();
        }
        return handoffs.stream()
                .map(h -> new ModelInvocationRequest.ToolSpec(
                        h.getToolName(),
                        h.getToolDescription(),
                        java.util.Map.of() // no parameters
                ))
                .collect(Collectors.toList());
    }
}

