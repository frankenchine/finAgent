package com.finagent.tools;

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

