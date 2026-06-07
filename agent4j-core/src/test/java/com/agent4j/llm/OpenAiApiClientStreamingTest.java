/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import com.agent4j.config.LlmProperties;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;
import com.agent4j.model.ModelStreamEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiApiClientStreamingTest {

    @Test
    void streamsOpenAiChatCompletionDeltas() {
        LlmProperties properties = new LlmProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.example.test/v1");
        properties.setModel("test-model");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        OpenAiApiClient client = new OpenAiApiClient(properties, restTemplate, new ObjectMapper());
        String body = ""
                + "data: {\"choices\":[{\"delta\":{\"content\":\"he\"}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"llo\"}}]}\n\n"
                + "data: [DONE]\n\n";

        server.expect(requestTo("https://api.example.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"model\":\"test-model\",\"stream\":true}"))
                .andRespond(withSuccess(body, MediaType.TEXT_EVENT_STREAM));

        List<String> deltas = new ArrayList<>();
        ModelInvocationResponse response = client.invokeStream(
                new ModelInvocationRequest("", List.of(Message.user("hello")), List.of()),
                event -> {
                    if (event.getType() == ModelStreamEvent.Type.DELTA) {
                        deltas.add(event.getDelta());
                    }
                });

        assertThat(deltas).containsExactly("he", "llo");
        assertThat(response.getAssistantText()).isEqualTo("hello");
        server.verify();
    }

    @Test
    void streamsOpenAiToolCallDeltas() {
        LlmProperties properties = new LlmProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.example.test/v1");
        properties.setModel("test-model");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        OpenAiApiClient client = new OpenAiApiClient(properties, restTemplate, new ObjectMapper());
        String body = ""
                + "\n"
                + "event: ignored\n"
                + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call-1\",\"function\":{\"name\":\"lookup\",\"arguments\":\"{\\\"q\\\":\"}}]}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"\\\"agent4j\\\"}\"}}]}}]}\n\n"
                + "data: [DONE]\n\n";

        server.expect(requestTo("https://api.example.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"model\":\"test-model\",\"stream\":true}"))
                .andRespond(withSuccess(body, MediaType.TEXT_EVENT_STREAM));

        ModelInvocationResponse response = client.invokeStream(
                new ModelInvocationRequest("", List.of(Message.user("hello")), List.of()),
                event -> {
                });

        assertThat(response.getAssistantText()).isEmpty();
        assertThat(response.getToolCalls()).hasSize(1);
        assertThat(response.getToolCalls().get(0).getId()).isEqualTo("call-1");
        assertThat(response.getToolCalls().get(0).getName()).isEqualTo("lookup");
        assertThat(response.getToolCalls().get(0).getArgumentsJson()).isEqualTo("{\"q\":\"agent4j\"}");
        server.verify();
    }
}
