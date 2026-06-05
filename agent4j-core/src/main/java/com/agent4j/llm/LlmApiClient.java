/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;
import com.agent4j.model.ModelStreamEvent;

import java.util.function.Consumer;

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

    default ModelInvocationResponse invokeStream(ModelInvocationRequest request, Consumer<ModelStreamEvent> consumer) {
        ModelInvocationResponse response = invoke(request);
        if (consumer != null) {
            consumer.accept(ModelStreamEvent.delta(response.getAssistantText()));
            consumer.accept(ModelStreamEvent.completed(response));
        }
        return response;
    }
}

