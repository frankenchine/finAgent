package com.agent4j.core;

import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;

/**
 * Abstraction for a single LLM call. Implementations can delegate to Spring AI ChatModel or custom HTTP client.
 */
public interface ModelInvoker {

    ModelInvocationResponse invoke(ModelInvocationRequest request);
}

