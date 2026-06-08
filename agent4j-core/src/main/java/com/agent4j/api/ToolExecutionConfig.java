/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.api;

import java.util.function.BiFunction;

/**
 * Per-run tool execution behavior.
 */
public final class ToolExecutionConfig {

    private final long timeoutMillis;
    private final BiFunction<ToolInvocationError, Tool.ToolContext, Object> errorFormatter;

    private ToolExecutionConfig(Builder b) {
        this.timeoutMillis = b.timeoutMillis;
        this.errorFormatter = b.errorFormatter != null ? b.errorFormatter : ToolExecutionConfig::defaultError;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public Object formatError(ToolInvocationError error, Tool.ToolContext context) {
        return errorFormatter.apply(error, context);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ToolExecutionConfig defaults() {
        return builder().build();
    }

    private static Object defaultError(ToolInvocationError error, Tool.ToolContext context) {
        return "Error: " + error.getMessage();
    }

    public static final class Builder {
        private long timeoutMillis;
        private BiFunction<ToolInvocationError, Tool.ToolContext, Object> errorFormatter;

        public Builder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = Math.max(0, timeoutMillis);
            return this;
        }

        public Builder errorFormatter(BiFunction<ToolInvocationError, Tool.ToolContext, Object> errorFormatter) {
            this.errorFormatter = errorFormatter;
            return this;
        }

        public ToolExecutionConfig build() {
            return new ToolExecutionConfig(this);
        }
    }
}
