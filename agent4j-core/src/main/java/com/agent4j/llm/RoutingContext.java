/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Context for a single routed LLM invocation.
 * Carries agent and task level metadata as well as arbitrary attributes.
 */
public final class RoutingContext {

    private final String agentName;
    private final String taskType;
    private final String userId;
    private final Map<String, Object> attributes;

    private RoutingContext(Builder builder) {
        this.agentName = builder.agentName;
        this.taskType = builder.taskType;
        this.userId = builder.userId;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
    }

    public String getAgentName() {
        return agentName;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getUserId() {
        return userId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String agentName;
        private String taskType;
        private String userId;
        private final Map<String, Object> attributes = new HashMap<>();

        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        public Builder taskType(String taskType) {
            this.taskType = taskType;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key != null && value != null) {
                this.attributes.put(key, value);
            }
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
            return this;
        }

        public RoutingContext build() {
            return new RoutingContext(this);
        }
    }
}

