/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import com.agent4j.config.LlmProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helpers to build repositories from configuration properties.
 */
public final class RoutingRuleRepositories {

    private RoutingRuleRepositories() {
    }

    public static LlmRoutingRuleRepository fromProperties(LlmProperties properties) {
        if (properties == null || properties.getRoutingRules() == null || properties.getRoutingRules().isEmpty()) {
            return new InMemoryLlmRoutingRuleRepository(List.of());
        }

        List<LlmRoutingRule> rules = new ArrayList<>();
        for (LlmProperties.RoutingRuleProperties rp : properties.getRoutingRules()) {
            if (rp == null) {
                continue;
            }
            if (rp.getPrimaryModel() == null || rp.getPrimaryModel().isBlank()) {
                continue;
            }

            Pattern agentPattern = null;
            if (rp.getAgent() != null && !rp.getAgent().isBlank()) {
                agentPattern = Pattern.compile(rp.getAgent());
            }

            ModelIdentifier primary = ModelIdentifier.fromString(rp.getPrimaryModel());
            List<ModelIdentifier> fallbacks = new ArrayList<>();
            if (rp.getFallbackModels() != null) {
                for (String fb : rp.getFallbackModels()) {
                    if (fb != null && !fb.isBlank()) {
                        fallbacks.add(ModelIdentifier.fromString(fb));
                    }
                }
            }

            rules.add(new LlmRoutingRule(agentPattern, rp.getTaskType(), primary, fallbacks));
        }

        return new InMemoryLlmRoutingRuleRepository(rules);
    }
}

