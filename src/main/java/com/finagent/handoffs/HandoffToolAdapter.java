package com.finagent.handoffs;

import com.finagent.api.Handoff;
import com.finagent.model.ModelInvocationRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts handoffs to ToolSpec list for the LLM (so the model can "call" a handoff as a tool).
 */
public final class HandoffToolAdapter {

    private HandoffToolAdapter() {
    }

    public static List<ModelInvocationRequest.ToolSpec> toToolSpecs(List<Handoff> handoffs) {
        if (handoffs == null || handoffs.isEmpty()) {
            return List.of();
        }
        List<ModelInvocationRequest.ToolSpec> specs = new ArrayList<>();
        for (Handoff h : handoffs) {
            specs.add(new ModelInvocationRequest.ToolSpec(
                    h.getToolName(),
                    h.getToolDescription(),
                    Map.of("type", "object", "properties", Map.of())
            ));
        }
        return specs;
    }
}
