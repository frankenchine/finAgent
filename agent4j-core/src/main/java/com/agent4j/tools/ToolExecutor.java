/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.tools;

import com.agent4j.api.Tool;
import com.agent4j.api.ToolExecutionConfig;
import com.agent4j.api.ToolInvocationError;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes tool invocations by dispatching to the matching Tool from the agent's tool list.
 */
public class ToolExecutor {

    private final Map<String, Tool> toolsByName;
    private final ToolExecutionConfig config;

    public ToolExecutor(List<Tool> tools) {
        this(tools, ToolExecutionConfig.defaults());
    }

    public ToolExecutor(List<Tool> tools, ToolExecutionConfig config) {
        this.toolsByName = tools.stream()
                .collect(java.util.stream.Collectors.toMap(Tool::getName, t -> t, (a, b) -> a));
        this.config = config != null ? config : ToolExecutionConfig.defaults();
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
            return invokeTool(tool, ctx);
        } catch (Exception e) {
            return config.formatError(new ToolInvocationError(invocation.getName(), e.getMessage(), e), ctx);
        }
    }

    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    private Object invokeTool(Tool tool, Tool.ToolContext ctx) throws Exception {
        if (config.getTimeoutMillis() <= 0) {
            return tool.invoke(ctx);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Object> future = executor.submit(() -> tool.invoke(ctx));
        try {
            return future.get(config.getTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("tool timed out after " + config.getTimeoutMillis() + " ms", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        } finally {
            executor.shutdownNow();
        }
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

