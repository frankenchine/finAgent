/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.agent4j.config.LlmProperties;
import com.agent4j.llm.dto.OpenAiRequest;
import com.agent4j.llm.dto.OpenAiResponse;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        String model = request.getModel();
        openAiRequest.setModel(model != null && !model.isBlank() ? model : properties.getModel());
        openAiRequest.setTemperature(properties.getTemperature());
        openAiRequest.setMaxTokens(properties.getMaxTokens());

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

