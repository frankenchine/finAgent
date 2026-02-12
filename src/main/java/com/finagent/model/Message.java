package com.finagent.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A single message in a conversation. Provider-agnostic representation.
 * 核心字段：1、消息类型；2、消息内容；3、工具调用ID；4、工具调用列表（仅ASSISTANT消息）
 * 1、消息类型：SYSTEM、USER、ASSISTANT、TOOL
 * 2、消息内容：消息的文本内容
 * 3、工具调用ID：如果消息是工具调用，则包含工具调用ID
 * 4、工具调用列表：如果ASSISTANT消息包含工具调用，则包含完整的工具调用信息
 * 
 */
public interface Message {

    MessageType getMessageType();

    String getText();

    default Map<String, Object> getMetadata() {
        return Collections.emptyMap();
    }

    /**
     * Get tool calls associated with this message. Only applicable for ASSISTANT messages.
     * Returns null if the message has no tool calls.
     *
     * @return list of tool call information, or null if no tool calls
     */
    default List<ToolCallInfo> getToolCalls() {
        return null;
    }

    enum MessageType {
        SYSTEM,
        USER,
        ASSISTANT,
        TOOL
    }

    static Message system(String text) {
        return new SimpleMessage(MessageType.SYSTEM, text, null, null);
    }

    static Message user(String text) {
        return new SimpleMessage(MessageType.USER, text, null, null);
    }

    static Message assistant(String text) {
        return new SimpleMessage(MessageType.ASSISTANT, text, null, null);
    }

    /**
     * Create an assistant message with tool calls.
     *
     * @param text the assistant message text (may be null or empty if tool_calls are present)
     * @param toolCalls list of tool call information, or null if no tool calls
     * @return a Message instance
     */
    static Message assistant(String text, List<ToolCallInfo> toolCalls) {
        return new SimpleMessage(MessageType.ASSISTANT, text, null, toolCalls);
    }

    static Message tool(String text, String toolCallId) {
        return new SimpleMessage(MessageType.TOOL, text, toolCallId, null);
    }

    /**
     * Information about a tool call made by an assistant message.
     */
    final class ToolCallInfo {
        private final String id;
        private final String name;
        private final String argumentsJson;

        public ToolCallInfo(String id, String name, String argumentsJson) {
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

    final class SimpleMessage implements Message {
        private final MessageType messageType;
        private final String text;
        private final String toolCallId;
        private final List<ToolCallInfo> toolCalls;

        public SimpleMessage(MessageType messageType, String text, String toolCallId, List<ToolCallInfo> toolCalls) {
            this.messageType = messageType;
            this.text = text != null ? text : "";
            this.toolCallId = toolCallId;
            this.toolCalls = toolCalls != null && !toolCalls.isEmpty() ? List.copyOf(toolCalls) : null;
        }

        @Override
        public MessageType getMessageType() {
            return messageType;
        }

        @Override
        public String getText() {
            return text;
        }

        public String getToolCallId() {
            return toolCallId;
        }

        @Override
        public List<ToolCallInfo> getToolCalls() {
            return toolCalls;
        }
    }
}
