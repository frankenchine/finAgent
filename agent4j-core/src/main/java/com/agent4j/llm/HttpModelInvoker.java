/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import com.agent4j.core.ModelInvoker;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;

/**
 * HTTP-based ModelInvoker implementation that delegates to LLM API clients.
 */
public class HttpModelInvoker implements ModelInvoker {

    private final LlmApiClient apiClient;

    public HttpModelInvoker(LlmApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public ModelInvocationResponse invoke(ModelInvocationRequest request) {
        return apiClient.invoke(request);
    }
}

