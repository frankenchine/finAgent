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

