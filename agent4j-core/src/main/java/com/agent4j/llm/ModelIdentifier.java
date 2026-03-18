/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import java.util.Objects;

/**
 * Value object that uniquely identifies a concrete model instance.
 * This avoids scattering raw model name strings across the codebase.
 */
public final class ModelIdentifier {

    private final String provider;
    private final String model;

    public ModelIdentifier(String provider, String model) {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("provider must not be null or empty");
        }
        if (model == null || model.isEmpty()) {
            throw new IllegalArgumentException("model must not be null or empty");
        }
        this.provider = provider;
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    /**
     * Parse identifier from a compact string form like "openai:gpt-4.1-mini".
     */
    public static ModelIdentifier fromString(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("value must not be null or empty");
        }
        String[] parts = value.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid model identifier: " + value + ". Expected format 'provider:model'.");
        }
        return new ModelIdentifier(parts[0], parts[1]);
    }

    /**
     * Serialize this identifier to a compact string form "provider:model".
     */
    @Override
    public String toString() {
        return provider + ":" + model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModelIdentifier that = (ModelIdentifier) o;
        return Objects.equals(provider, that.provider) && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, model);
    }
}

