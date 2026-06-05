/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

/**
 * Per-run model settings. Null fields keep provider defaults.
 */
public final class ModelSettings {

    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private final String toolChoice;

    private ModelSettings(Builder b) {
        this.model = b.model;
        this.temperature = b.temperature;
        this.maxTokens = b.maxTokens;
        this.toolChoice = b.toolChoice;
    }

    public String getModel() {
        return model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public String getToolChoice() {
        return toolChoice;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private String toolChoice;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder toolChoice(String toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public ModelSettings build() {
            return new ModelSettings(this);
        }
    }
}
