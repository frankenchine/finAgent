package com.agent4j.llm;

import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;

/**
 * Interface for LLM API clients.
 */
public interface LlmApiClient {

    /**
     * Invoke the LLM API with the given request.
     *
     * @param request the model invocation request
     * @return the model invocation response
     */
    ModelInvocationResponse invoke(ModelInvocationRequest request);
}

