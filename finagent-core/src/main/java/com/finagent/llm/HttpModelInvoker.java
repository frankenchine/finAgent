package com.finagent.llm;

import com.finagent.core.ModelInvoker;
import com.finagent.model.ModelInvocationRequest;
import com.finagent.model.ModelInvocationResponse;

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

