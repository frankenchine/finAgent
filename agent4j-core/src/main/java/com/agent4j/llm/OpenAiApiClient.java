/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.agent4j.api.ModelSettings;
import com.agent4j.config.LlmProperties;
import com.agent4j.llm.dto.OpenAiRequest;
import com.agent4j.llm.dto.OpenAiResponse;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;
import com.agent4j.model.ModelStreamEvent;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * OpenAI API client implementation.
 */
public class OpenAiApiClient implements LlmApiClient {

    private final LlmProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiApiClient(LlmProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ModelInvocationResponse invoke(ModelInvocationRequest request) {
        try {
            OpenAiRequest openAiRequest = convertToOpenAiRequest(request);
            String url = buildApiUrl();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());

            HttpEntity<OpenAiRequest> entity = new HttpEntity<>(openAiRequest, headers);

            ResponseEntity<OpenAiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    OpenAiResponse.class
            );

            return convertFromOpenAiResponse(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke OpenAI API: " + e.getMessage(), e);
        }
    }

    @Override
    public ModelInvocationResponse invokeStream(ModelInvocationRequest request, Consumer<ModelStreamEvent> consumer) {
        try {
            OpenAiRequest openAiRequest = convertToOpenAiRequest(request);
            openAiRequest.setStream(true);
            String url = buildApiUrl();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM));
            headers.setBearerAuth(properties.getApiKey());

            StringBuilder assistantText = new StringBuilder();
            Map<Integer, StreamingToolCallBuilder> toolCallBuilders = new LinkedHashMap<>();
            restTemplate.execute(
                    url,
                    HttpMethod.POST,
                    clientHttpRequest -> {
                        headers.forEach((name, values) -> values.forEach(value -> clientHttpRequest.getHeaders().add(name, value)));
                        objectMapper.writeValue(clientHttpRequest.getBody(), openAiRequest);
                    },
                    clientHttpResponse -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                                clientHttpResponse.getBody(),
                                StandardCharsets.UTF_8
                        ))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String data = line.substring("data:".length()).trim();
                                if ("[DONE]".equals(data)) {
                                    break;
                                }
                                String delta;
                                try {
                                    delta = applyStreamingDelta(data, toolCallBuilders);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to parse OpenAI stream chunk", e);
                                }
                                if (delta != null && !delta.isEmpty()) {
                                    assistantText.append(delta);
                                    if (consumer != null) {
                                        consumer.accept(ModelStreamEvent.delta(delta));
                                    }
                                }
                            }
                        }
                        return null;
                    }
            );

            List<ModelInvocationResponse.ToolCall> toolCalls = toolCallBuilders.values().stream()
                    .map(StreamingToolCallBuilder::build)
                    .collect(Collectors.toList());
            ModelInvocationResponse response = new ModelInvocationResponse(assistantText.toString(), toolCalls);
            if (consumer != null) {
                consumer.accept(ModelStreamEvent.completed(response));
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to stream OpenAI API: " + e.getMessage(), e);
        }
    }

    private String buildApiUrl() {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://api.openai.com/v1";
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/chat/completions";
    }

    private OpenAiRequest convertToOpenAiRequest(ModelInvocationRequest request) {
        OpenAiRequest openAiRequest = new OpenAiRequest();
        ModelSettings settings = request.getModelSettings();
        openAiRequest.setModel(settings != null && settings.getModel() != null ? settings.getModel() : properties.getModel());
        openAiRequest.setTemperature(settings != null && settings.getTemperature() != null
                ? settings.getTemperature()
                : properties.getTemperature());
        openAiRequest.setMaxTokens(settings != null && settings.getMaxTokens() != null
                ? settings.getMaxTokens()
                : properties.getMaxTokens());
        if (settings != null && settings.getToolChoice() != null) {
            openAiRequest.setToolChoice(settings.getToolChoice());
        }

        // Convert messages
        List<OpenAiRequest.Message> messages = new ArrayList<>();

        // Add system prompt if present
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            OpenAiRequest.Message systemMessage = new OpenAiRequest.Message();
            systemMessage.setRole("system");
            systemMessage.setContent(request.getSystemPrompt());
            messages.add(systemMessage);
        }

        // Convert conversation messages
        for (Message msg : request.getMessages()) {
            OpenAiRequest.Message openAiMsg = new OpenAiRequest.Message();

            switch (msg.getMessageType()) {
                case SYSTEM:
                    openAiMsg.setRole("system");
                    openAiMsg.setContent(msg.getText());
                    break;
                case USER:
                    openAiMsg.setRole("user");
                    openAiMsg.setContent(msg.getText());
                    break;
                case ASSISTANT:
                    openAiMsg.setRole("assistant");
                    // Check if this assistant message has tool_calls
                    List<Message.ToolCallInfo> toolCalls = msg.getToolCalls();
                    if (toolCalls != null && !toolCalls.isEmpty()) {
                        // Convert tool calls to OpenAI format
                        List<OpenAiRequest.ToolCall> openAiToolCalls = new ArrayList<>();
                        for (Message.ToolCallInfo toolCallInfo : toolCalls) {
                            OpenAiRequest.ToolCall toolCall = new OpenAiRequest.ToolCall();
                            toolCall.setId(toolCallInfo.getId());
                            toolCall.setType("function");

                            OpenAiRequest.FunctionCall functionCall = new OpenAiRequest.FunctionCall();
                            functionCall.setName(toolCallInfo.getName());
                            functionCall.setArguments(toolCallInfo.getArgumentsJson());
                            toolCall.setFunction(functionCall);

                            openAiToolCalls.add(toolCall);
                        }
                        openAiMsg.setToolCalls(openAiToolCalls);
                        // If assistant has tool_calls, content should be null
                        openAiMsg.setContent(null);
                    } else {
                        // No tool calls, set content normally
                        openAiMsg.setContent(msg.getText());
                    }
                    break;
                case TOOL:
                    openAiMsg.setRole("tool");
                    openAiMsg.setContent(msg.getText());
                    if (msg instanceof Message.SimpleMessage) {
                        openAiMsg.setToolCallId(((Message.SimpleMessage) msg).getToolCallId());
                    }
                    break;
            }

            messages.add(openAiMsg);
        }

        openAiRequest.setMessages(messages);

        // Convert tools if present
        if (request.getToolSpecs() != null && !request.getToolSpecs().isEmpty()) {
            List<OpenAiRequest.Tool> tools = request.getToolSpecs().stream()
                    .map(this::convertToolSpec)
                    .collect(Collectors.toList());
            openAiRequest.setTools(tools);
        }

        return openAiRequest;
    }

    private String applyStreamingDelta(String json, Map<Integer, StreamingToolCallBuilder> toolCallBuilders) throws Exception {
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
        com.fasterxml.jackson.databind.JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        com.fasterxml.jackson.databind.JsonNode delta = choices.get(0).path("delta");
        com.fasterxml.jackson.databind.JsonNode toolCalls = delta.path("tool_calls");
        if (toolCalls.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode toolCall : toolCalls) {
                int index = toolCall.path("index").asInt(toolCallBuilders.size());
                StreamingToolCallBuilder builder = toolCallBuilders.computeIfAbsent(index, ignored -> new StreamingToolCallBuilder());
                if (toolCall.hasNonNull("id")) {
                    builder.id = toolCall.path("id").asText();
                }
                com.fasterxml.jackson.databind.JsonNode function = toolCall.path("function");
                if (function.hasNonNull("name")) {
                    builder.name = function.path("name").asText();
                }
                if (function.hasNonNull("arguments")) {
                    builder.arguments.append(function.path("arguments").asText());
                }
            }
        }
        com.fasterxml.jackson.databind.JsonNode content = delta.path("content");
        return content.isTextual() ? content.asText() : null;
    }

    private static final class StreamingToolCallBuilder {
        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();

        private ModelInvocationResponse.ToolCall build() {
            return new ModelInvocationResponse.ToolCall(id, name, arguments.toString());
        }
    }

    private OpenAiRequest.Tool convertToolSpec(ModelInvocationRequest.ToolSpec toolSpec) {
        OpenAiRequest.Tool tool = new OpenAiRequest.Tool();
        tool.setType("function");

        OpenAiRequest.Function function = new OpenAiRequest.Function();
        function.setName(toolSpec.getName());
        function.setDescription(toolSpec.getDescription());

        // Convert parameters to OpenAI format (JSON Schema)
        Map<String, Object> parameters;
        Map<String, Object> toolParams = toolSpec.getParameters();

        if (toolParams == null || toolParams.isEmpty()) {
            // Empty parameters
            parameters = new HashMap<>();
            parameters.put("type", "object");
            parameters.put("properties", new HashMap<>());
            parameters.put("required", new ArrayList<>());
        } else if (toolParams.containsKey("type") && toolParams.containsKey("properties")) {
            // Already in JSON Schema format, use as-is
            parameters = new HashMap<>(toolParams);
            // Ensure required field exists
            if (!parameters.containsKey("required")) {
                parameters.put("required", new ArrayList<>());
            }
        } else {
            // Convert simple map to JSON Schema format
            parameters = new HashMap<>();
            parameters.put("type", "object");
            parameters.put("properties", toolParams);
            parameters.put("required", new ArrayList<>());
        }

        function.setParameters(parameters);
        tool.setFunction(function);

        return tool;
    }

    private ModelInvocationResponse convertFromOpenAiResponse(OpenAiResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return new ModelInvocationResponse("", List.of());
        }

        OpenAiResponse.Choice choice = response.getChoices().get(0);
        OpenAiResponse.Message message = choice.getMessage();

        String assistantText = message.getContent() != null ? message.getContent() : "";

        // Convert tool calls
        List<ModelInvocationResponse.ToolCall> toolCalls = new ArrayList<>();
        if (message.getToolCalls() != null) {
            for (OpenAiResponse.ToolCall toolCall : message.getToolCalls()) {
                String id = toolCall.getId();
                String name = toolCall.getFunction().getName();
                String arguments = toolCall.getFunction().getArguments();

                toolCalls.add(new ModelInvocationResponse.ToolCall(id, name, arguments));
            }
        }

        return new ModelInvocationResponse(assistantText, toolCalls);
    }
}

