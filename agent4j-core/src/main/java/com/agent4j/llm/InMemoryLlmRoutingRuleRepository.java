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
 * Simple in-memory routing rule repository.
 */
public class InMemoryLlmRoutingRuleRepository implements LlmRoutingRuleRepository {

    private final List<LlmRoutingRule> rules;

    public InMemoryLlmRoutingRuleRepository(List<LlmRoutingRule> rules) {
        this.rules = Collections.unmodifiableList(rules != null ? new ArrayList<>(rules) : new ArrayList<>());
    }

    @Override
    public List<LlmRoutingRule> listRules() {
        return rules;
    }
}

