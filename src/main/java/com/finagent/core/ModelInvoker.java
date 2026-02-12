package com.finagent.core;

import com.finagent.model.ModelInvocationRequest;
import com.finagent.model.ModelInvocationResponse;

/**
 * Abstraction for a single LLM call. Implementations can delegate to Spring AI ChatModel or custom HTTP client.
 */
public interface ModelInvoker {

    ModelInvocationResponse invoke(ModelInvocationRequest request);
}
