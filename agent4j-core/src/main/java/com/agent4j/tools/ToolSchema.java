/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
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

