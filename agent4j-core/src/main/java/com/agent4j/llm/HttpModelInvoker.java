/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.llm;

import com.agent4j.core.ModelInvoker;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;
import com.agent4j.model.ModelStreamEvent;

import java.util.function.Consumer;

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

    @Override
    public ModelInvocationResponse invokeStream(ModelInvocationRequest request, Consumer<ModelStreamEvent> consumer) {
        return apiClient.invokeStream(request, consumer);
    }
}

