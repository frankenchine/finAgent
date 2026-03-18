/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A single routing rule that maps (agentName, taskType) to a primary model and optional fallbacks.
 */
public final class LlmRoutingRule {

    private final Pattern agentNamePattern;
    private final String taskType;
    private final ModelIdentifier primaryModel;
    private final List<ModelIdentifier> fallbackModels;

    public LlmRoutingRule(Pattern agentNamePattern,
                          String taskType,
                          ModelIdentifier primaryModel,
                          List<ModelIdentifier> fallbackModels) {
        this.agentNamePattern = agentNamePattern;
        this.taskType = (taskType == null || taskType.isBlank()) ? null : taskType;
        if (primaryModel == null) {
            throw new IllegalArgumentException("primaryModel must not be null");
        }
        this.primaryModel = primaryModel;
        this.fallbackModels = Collections.unmodifiableList(
                fallbackModels != null ? new ArrayList<>(fallbackModels) : new ArrayList<>()
        );
    }

    public Pattern getAgentNamePattern() {
        return agentNamePattern;
    }

    public String getTaskType() {
        return taskType;
    }

    public ModelIdentifier getPrimaryModel() {
        return primaryModel;
    }

    public List<ModelIdentifier> getFallbackModels() {
        return fallbackModels;
    }
}

