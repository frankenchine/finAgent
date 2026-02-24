package com.finagent.llm;

import com.finagent.config.LlmProperties;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * DeepSeek API client implementation.
 * DeepSeek API is compatible with OpenAI API format, so we can reuse OpenAiApiClient logic.
 */
public class DeepSeekApiClient extends OpenAiApiClient {

    public DeepSeekApiClient(LlmProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(properties, restTemplate, objectMapper);
    }

    @Override
    public com.finagent.model.ModelInvocationResponse invoke(com.finagent.model.ModelInvocationRequest request) {
        // DeepSeek uses the same API format as OpenAI, so we can reuse the parent implementation
        return super.invoke(request);
    }
}

