/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a routing strategy decision.
 * Currently focuses on a primary model with optional fallbacks, but is
 * designed to be extended with pre/post processor chains in the future.
 */
public final class LlmRoutingDecision {

    private final ModelIdentifier primaryModel;
    private final List<ModelIdentifier> fallbackModels;

    private LlmRoutingDecision(Builder builder) {
        this.primaryModel = builder.primaryModel;
        this.fallbackModels = Collections.unmodifiableList(new ArrayList<>(builder.fallbackModels));
    }

    public ModelIdentifier getPrimaryModel() {
        return primaryModel;
    }

    public List<ModelIdentifier> getFallbackModels() {
        return fallbackModels;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ModelIdentifier primaryModel;
        private final List<ModelIdentifier> fallbackModels = new ArrayList<>();

        public Builder primaryModel(ModelIdentifier primaryModel) {
            this.primaryModel = primaryModel;
            return this;
        }

        public Builder addFallback(ModelIdentifier fallback) {
            if (fallback != null) {
                this.fallbackModels.add(fallback);
            }
            return this;
        }

        public Builder fallbackModels(List<ModelIdentifier> fallbacks) {
            if (fallbacks != null) {
                this.fallbackModels.addAll(fallbacks);
            }
            return this;
        }

        public LlmRoutingDecision build() {
            if (primaryModel == null) {
                throw new IllegalStateException("primaryModel is required");
            }
            return new LlmRoutingDecision(this);
        }
    }
}

