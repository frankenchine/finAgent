/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.llm;

import com.agent4j.config.LlmProperties;
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
    public com.agent4j.model.ModelInvocationResponse invoke(com.agent4j.model.ModelInvocationRequest request) {
        // DeepSeek uses the same API format as OpenAI, so we can reuse the parent implementation
        return super.invoke(request);
    }
}

