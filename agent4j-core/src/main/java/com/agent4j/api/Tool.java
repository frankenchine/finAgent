/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.api;

import java.util.Map;

/**
 * A tool that an agent can invoke. Exposes name, description, and parameter schema for the LLM.
 */
public interface Tool {

    String getName();

    String getDescription();

    /**
     * JSON Schema-like map for tool parameters. Can be empty if no parameters.
     */
    Map<String, Object> getParameterSchema();

    /**
     * Execute the tool with the given arguments (parsed from LLM tool call).
     *
     * @param context tool invocation context (e.g. tool call id, raw arguments)
     * @return result to send back to the LLM (typically string)
     */
    Object invoke(ToolContext context);

    interface ToolContext {
        String getToolCallId();

        String getToolName();

        String getArgumentsJson();

        Object getContext();
    }
}

