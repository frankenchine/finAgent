/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import com.agent4j.core.ModelInvoker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of model identifiers to invokers.
 * Intended to be a singleton Spring bean backed by a concurrent map.
 */
public class ModelInvokerRegistry {

    private final Map<ModelIdentifier, ModelInvoker> invokers = new ConcurrentHashMap<>();

    public ModelInvokerRegistry() {
    }

    public void register(ModelIdentifier identifier, ModelInvoker invoker) {
        if (identifier == null) {
            throw new IllegalArgumentException("identifier must not be null");
        }
        if (invoker == null) {
            throw new IllegalArgumentException("invoker must not be null");
        }
        invokers.put(identifier, invoker);
    }

    public ModelInvoker get(ModelIdentifier identifier) {
        if (identifier == null) {
            return null;
        }
        return invokers.get(identifier);
    }
}

