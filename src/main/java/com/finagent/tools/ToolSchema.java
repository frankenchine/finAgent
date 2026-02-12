package com.finagent.tools;

import com.finagent.api.Tool;
import com.finagent.model.ModelInvocationRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds ToolSpec list from Agent's tools and handoffs (handoffs are added by HandoffToolAdapter).
 */
public final class ToolSchema {

    private ToolSchema() {
    }

    public static ModelInvocationRequest.ToolSpec fromTool(Tool tool) {
        Map<String, Object> params = tool.getParameterSchema();
        if (params == null) {
            params = new HashMap<>();
        }
        return new ModelInvocationRequest.ToolSpec(
                tool.getName(),
                tool.getDescription(),
                params
        );
    }
}
