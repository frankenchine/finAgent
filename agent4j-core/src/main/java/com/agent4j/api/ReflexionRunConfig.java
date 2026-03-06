/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

/**
 * Configuration for Reflexion multi-trial runs.
 */
public final class ReflexionRunConfig {

    private static final String DEFAULT_REFLECT_PROMPT = "Task: {task}\n"
            + "Your previous attempt: {outcome}\n"
            + "Evaluation: {feedback}\n\n"
            + "Based on the above, write a brief reflection on what went wrong and what to try differently next time. Be specific and actionable.";

    private final int maxTrials;
    private final int reflectionLimit;
    private final String reflectPromptTemplate;

    private ReflexionRunConfig(Builder b) {
        this.maxTrials = b.maxTrials > 0 ? b.maxTrials : 3;
        this.reflectionLimit = b.reflectionLimit > 0 ? b.reflectionLimit : 10;
        this.reflectPromptTemplate = b.reflectPromptTemplate != null ? b.reflectPromptTemplate : DEFAULT_REFLECT_PROMPT;
    }

    public int getMaxTrials() {
        return maxTrials;
    }

    public int getReflectionLimit() {
        return reflectionLimit;
    }

    public String getReflectPromptTemplate() {
        return reflectPromptTemplate;
    }

    /**
     * Build the reflect prompt by replacing {task}, {outcome}, {feedback} placeholders.
     */
    public String buildReflectPrompt(String task, String outcome, String feedback) {
        return reflectPromptTemplate
                .replace("{task}", task != null ? task : "")
                .replace("{outcome}", outcome != null ? outcome : "")
                .replace("{feedback}", feedback != null ? feedback : "");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxTrials = 3;
        private int reflectionLimit = 10;
        private String reflectPromptTemplate;

        public Builder maxTrials(int maxTrials) {
            this.maxTrials = maxTrials;
            return this;
        }

        public Builder reflectionLimit(int reflectionLimit) {
            this.reflectionLimit = reflectionLimit;
            return this;
        }

        public Builder reflectPromptTemplate(String template) {
            this.reflectPromptTemplate = template;
            return this;
        }

        public ReflexionRunConfig build() {
            return new ReflexionRunConfig(this);
        }
    }
}
