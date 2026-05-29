/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.handoffs;

import com.agent4j.api.Handoff;
import com.agent4j.model.ModelInvocationRequest;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Adapts Handoff definitions into ToolSpecs that the model can call.
 */
public final class HandoffToolAdapter {

    private HandoffToolAdapter() {
    }

    public static List<ModelInvocationRequest.ToolSpec> toToolSpecs(List<Handoff> handoffs) {
        if (handoffs == null || handoffs.isEmpty()) {
            return Collections.emptyList();
        }
        return handoffs.stream()
                .map(h -> new ModelInvocationRequest.ToolSpec(
                        h.getToolName(),
                        h.getToolDescription(),
                        new HashMap<String, Object>() // no parameters
                ))
                .collect(Collectors.toList());
    }
}

