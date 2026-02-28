/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.core;

import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;

/**
 * Abstraction for a single LLM call. Implementations can delegate to Spring AI ChatModel or custom HTTP client.
 */
public interface ModelInvoker {

    ModelInvocationResponse invoke(ModelInvocationRequest request);
}

