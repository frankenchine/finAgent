package com.finagent.model;

import java.util.List;
import java.util.Map;

/**
 * Request for a single LLM invocation. Contains messages and tool/handoff definitions for the model.
 * 核心字段：1、系统提示；2、消息列表；3、工具/手柄定义列表
 * 1、系统提示：系统提示的文本内容
 * 2、消息列表：消息列表, 要传给LLM的历史消息列表
 * 3、tool/handoff定义列表：tool/handoff定义列表，告诉LLM有哪些工具/handoff可以调用
 * 
 */
public final class ModelInvocationRequest {

    private final String systemPrompt;
    private final List<Message> messages;
    private final List<ToolSpec> toolSpecs;

    public ModelInvocationRequest(String systemPrompt, List<Message> messages, List<ToolSpec> toolSpecs) {
        this.systemPrompt = systemPrompt;
        this.messages = messages != null ? List.copyOf(messages) : List.of();
        this.toolSpecs = toolSpecs != null ? List.copyOf(toolSpecs) : List.of();
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<ToolSpec> getToolSpecs() {
        return toolSpecs;
    }

    /**
     * Tool/function spec for the LLM (name, description, parameters schema).
     */
    public static final class ToolSpec {
        private final String name;
        private final String description;
        private final Map<String, Object> parameters;

        public ToolSpec(String name, String description, Map<String, Object> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }
    }
}
