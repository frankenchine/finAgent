/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.api;

/**
 * Tool execution failure passed to the configured error formatter.
 */
public final class ToolInvocationError {

    private final String toolName;
    private final String message;
    private final Throwable cause;

    public ToolInvocationError(String toolName, String message, Throwable cause) {
        this.toolName = toolName;
        this.message = message;
        this.cause = cause;
    }

    public String getToolName() {
        return toolName;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }
}
