package com.finagent.llm;

import com.finagent.model.ModelInvocationRequest;
import com.finagent.model.ModelInvocationResponse;

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

