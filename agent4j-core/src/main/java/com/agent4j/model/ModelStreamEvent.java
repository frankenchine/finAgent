/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.model;

/**
 * Streaming event from a model invocation.
 */
public final class ModelStreamEvent {

    public enum Type {
        DELTA,
        COMPLETED
    }

    private final Type type;
    private final String delta;
    private final ModelInvocationResponse response;

    private ModelStreamEvent(Type type, String delta, ModelInvocationResponse response) {
        this.type = type;
        this.delta = delta;
        this.response = response;
    }

    public static ModelStreamEvent delta(String delta) {
        return new ModelStreamEvent(Type.DELTA, delta, null);
    }

    public static ModelStreamEvent completed(ModelInvocationResponse response) {
        return new ModelStreamEvent(Type.COMPLETED, null, response);
    }

    public Type getType() {
        return type;
    }

    public String getDelta() {
        return delta;
    }

    public ModelInvocationResponse getResponse() {
        return response;
    }
}
