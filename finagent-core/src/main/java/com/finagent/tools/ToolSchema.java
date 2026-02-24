package com.finagent.tools;

import com.finagent.api.Tool;
import com.finagent.model.ModelInvocationRequest;

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

