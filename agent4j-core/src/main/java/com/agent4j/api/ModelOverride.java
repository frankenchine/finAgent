/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

/**
 * Optional per-agent override for LLM model parameters.
 * When set on an Agent, overrides the default LlmProperties for invocations by that agent.
 *
 * @param model     model name (e.g. gpt-4, deepseek-coder)
 * @param temperature temperature (0.0-2.0), null to use default
 * @param maxTokens max tokens in response, null to use default
 */
public record ModelOverride(String model, Double temperature, Integer maxTokens) {

    public static ModelOverride of(String model) {
        return new ModelOverride(model, null, null);
    }

    public static ModelOverride of(String model, Double temperature, Integer maxTokens) {
        return new ModelOverride(model, temperature, maxTokens);
    }
}
