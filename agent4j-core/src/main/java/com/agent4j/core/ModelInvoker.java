/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.core;

import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;
import com.agent4j.model.ModelStreamEvent;

import java.util.function.Consumer;

/**
 * Abstraction for a single LLM call. Implementations can delegate to Spring AI ChatModel or custom HTTP client.
 */
public interface ModelInvoker {

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

