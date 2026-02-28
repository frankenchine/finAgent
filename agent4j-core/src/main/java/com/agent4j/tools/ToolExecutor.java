/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.tools;

import com.agent4j.api.Tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Executes tool invocations by dispatching to the matching Tool from the agent's tool list.
 */
public class ToolExecutor {

    private final Map<String, Tool> toolsByName;

    public ToolExecutor(List<Tool> tools) {
        this.toolsByName = tools.stream()
                .collect(java.util.stream.Collectors.toMap(Tool::getName, t -> t, (a, b) -> a));
    }

    /**
     * Execute a single tool invocation. Returns the result string to send back to the LLM.
     */
    public Object execute(ToolInvocation invocation, Object runContext) {
        Tool tool = toolsByName.get(invocation.getName());
        if (tool == null) {
            return "Error: unknown tool '" + invocation.getName() + "'";
        }
        Tool.ToolContext ctx = new ToolContextImpl(
                invocation.getId(),
                invocation.getName(),
                invocation.getArgumentsJson(),
                runContext
        );
        try {
            return tool.invoke(ctx);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    private static final class ToolContextImpl implements Tool.ToolContext {
        private final String toolCallId;
        private final String toolName;
        private final String argumentsJson;
        private final Object context;

        ToolContextImpl(String toolCallId, String toolName, String argumentsJson, Object context) {
            this.toolCallId = toolCallId;
            this.toolName = toolName;
            this.argumentsJson = argumentsJson;
            this.context = context;
        }

        @Override
        public String getToolCallId() {
            return toolCallId;
        }

        @Override
        public String getToolName() {
            return toolName;
        }

        @Override
        public String getArgumentsJson() {
            return argumentsJson;
        }

        @Override
        public Object getContext() {
            return context;
        }
    }
}

