/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Default routing strategy:
 * - first-match wins based on (agentName regex, optional taskType)
 * - otherwise falls back to the configured default model
 */
public class SimpleLlmRoutingStrategy implements LlmRoutingStrategy {

    private final LlmRoutingRuleRepository repository;
    private final ModelIdentifier defaultModel;

    public SimpleLlmRoutingStrategy(LlmRoutingRuleRepository repository, ModelIdentifier defaultModel) {
        this.repository = repository;
        if (defaultModel == null) {
            throw new IllegalArgumentException("defaultModel must not be null");
        }
        this.defaultModel = defaultModel;
    }

    @Override
    public LlmRoutingDecision decide(RoutingContext context) {
        String agentName = context != null ? context.getAgentName() : null;
        String taskType = context != null ? context.getTaskType() : null;

        List<LlmRoutingRule> rules = repository != null ? repository.listRules() : List.of();
        for (LlmRoutingRule rule : rules) {
            if (rule == null) {
                continue;
            }
            if (!matchesAgent(rule.getAgentNamePattern(), agentName)) {
                continue;
            }
            if (!matchesTaskType(rule.getTaskType(), taskType)) {
                continue;
            }
            return LlmRoutingDecision.builder()
                    .primaryModel(rule.getPrimaryModel())
                    .fallbackModels(rule.getFallbackModels())
                    .build();
        }

        return LlmRoutingDecision.builder()
                .primaryModel(defaultModel)
                .build();
    }

    private boolean matchesAgent(Pattern agentNamePattern, String agentName) {
        if (agentNamePattern == null) {
            return true;
        }
        if (agentName == null) {
            return false;
        }
        return agentNamePattern.matcher(agentName).matches();
    }

    private boolean matchesTaskType(String ruleTaskType, String ctxTaskType) {
        if (ruleTaskType == null || ruleTaskType.isBlank()) {
            return true;
        }
        if (ctxTaskType == null) {
            return false;
        }
        return ruleTaskType.toLowerCase(Locale.ROOT).equals(ctxTaskType.toLowerCase(Locale.ROOT));
    }
}

