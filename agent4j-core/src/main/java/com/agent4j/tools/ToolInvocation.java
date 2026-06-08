/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.tools;

/**
 * Single tool invocation from the runner, based on a model's tool call.
 */
public class ToolInvocation {

    private final String id;
    private final String name;
    private final String argumentsJson;

    public ToolInvocation(String id, String name, String argumentsJson) {
        this.id = id;
        this.name = name;
        this.argumentsJson = argumentsJson != null ? argumentsJson : "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }
}

