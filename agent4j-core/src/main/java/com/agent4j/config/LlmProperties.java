/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for LLM API integration.
 */
@ConfigurationProperties(prefix = "agent4j.llm")
public class LlmProperties {

    /**
     * LLM provider: openai, deepseek
     */
    private String provider = "openai";

    /**
     * Base URL for the LLM API
     */
    private String baseUrl;

    /**
     * API key for authentication
     */
    private String apiKey;

    /**
     * Model name (e.g., gpt-3.5-turbo, gpt-4, deepseek-chat)
     */
    private String model = "gpt-3.5-turbo";

    /**
     * Default model identifier used by routing when no rule matches.
     * Format: "{provider}:{modelName}" (e.g. "openai:gpt-4o-mini").
     * If not set, falls back to "{provider}:{model}".
     */
    private String defaultModel;

    /**
     * Optional routing rules loaded from configuration.
     */
    private List<RoutingRuleProperties> routingRules = new ArrayList<>();

    /**
     * Temperature parameter (0.0 to 2.0)
     */
    private Double temperature = 0.7;

    /**
     * Maximum tokens in the response
     */
    private Integer maxTokens = 2000;

    /**
     * Request timeout in seconds
     */
    private Integer timeoutSeconds = 60;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public List<RoutingRuleProperties> getRoutingRules() {
        return routingRules;
    }

    public void setRoutingRules(List<RoutingRuleProperties> routingRules) {
        this.routingRules = routingRules != null ? routingRules : new ArrayList<>();
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public static class RoutingRuleProperties {
        /**
         * Agent name pattern (Java regex). If empty, matches any agent.
         */
        private String agent;

        /**
         * Optional task type to match (exact match, case-insensitive).
         */
        private String taskType;

        /**
         * Primary model identifier: "{provider}:{modelName}".
         */
        private String primaryModel;

        /**
         * Fallback model identifiers: list of "{provider}:{modelName}".
         */
        private List<String> fallbackModels = new ArrayList<>();

        public String getAgent() {
            return agent;
        }

        public void setAgent(String agent) {
            this.agent = agent;
        }

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public String getPrimaryModel() {
            return primaryModel;
        }

        public void setPrimaryModel(String primaryModel) {
            this.primaryModel = primaryModel;
        }

        public List<String> getFallbackModels() {
            return fallbackModels;
        }

        public void setFallbackModels(List<String> fallbackModels) {
            this.fallbackModels = fallbackModels != null ? fallbackModels : new ArrayList<>();
        }
    }
}

