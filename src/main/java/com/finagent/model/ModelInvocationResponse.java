package com.finagent.model;

import java.util.Collections;
import java.util.List;

/**
 * Response from a single LLM invocation. Contains assistant message and optional tool calls.
 */
public final class ModelInvocationResponse {

    private final String assistantText;
    private final List<ToolCall> toolCalls;

    public ModelInvocationResponse(String assistantText, List<ToolCall> toolCalls) {
        this.assistantText = assistantText;
        this.toolCalls = toolCalls != null ? List.copyOf(toolCalls) : List.of();
    }

    public String getAssistantText() {
        return assistantText;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /**
     * A single tool/function call from the model.
     */
    public static final class ToolCall {
        private final String id;
        private final String name;
        private final String argumentsJson;

        public ToolCall(String id, String name, String argumentsJson) {
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
}
