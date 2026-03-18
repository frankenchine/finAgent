/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import java.util.List;

/**
 * Abstraction for routing rule storage.
 * Default implementation is in-memory, but users can back this by DB or remote config.
 */
public interface LlmRoutingRuleRepository {

    /**
     * Return all routing rules in priority order (first match wins).
     */
    List<LlmRoutingRule> listRules();
}

