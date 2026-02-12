package com.finagent.llm;

/**
 * Supported LLM providers.
 */
public enum LlmProvider {
    OPENAI("openai"),
    DEEPSEEK("deepseek");

    private final String value;

    LlmProvider(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LlmProvider fromString(String value) {
        if (value == null) {
            return OPENAI; // default
        }
        for (LlmProvider provider : LlmProvider.values()) {
            if (provider.value.equalsIgnoreCase(value)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown LLM provider: " + value);
    }
}
