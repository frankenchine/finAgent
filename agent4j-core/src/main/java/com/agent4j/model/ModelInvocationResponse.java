/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.model;

import java.util.List;

/**
 * Response from a single LLM invocation. Contains assistant message and optional tool calls.
 * 核心字段：1、大模型回复文本，后续同一成为助手文本；2、工具调用列表
 * 1、助手文本：大模型回复文本，后续同一成为助手文本
 * 2、工具调用列表：大模型返回的工具调用列表，如果模型返回了工具调用，则需要调用工具执行器执行工具
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

