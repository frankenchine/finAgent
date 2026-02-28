package com.agent4j.tools;

import com.agent4j.api.Tool;
import com.agent4j.model.ModelInvocationRequest;

/**
 * Adapter to convert Tool into ModelInvocationRequest.ToolSpec.
 */
public final class ToolSchema {

    private ToolSchema() {
    }

    public static ModelInvocationRequest.ToolSpec fromTool(Tool tool) {
        return new ModelInvocationRequest.ToolSpec(
                tool.getName(),
                tool.getDescription(),
                tool.getParameterSchema()
        );
    }
}

