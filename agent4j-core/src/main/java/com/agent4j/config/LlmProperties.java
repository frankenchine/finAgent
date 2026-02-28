package com.agent4j.config;

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
}

