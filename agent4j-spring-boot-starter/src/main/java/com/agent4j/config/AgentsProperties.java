/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the agents framework.
 */
@ConfigurationProperties(prefix = "agent4j")
public class AgentsProperties {

    /**
     * Default maximum turns per run.
     */
    private int maxTurns = 20;

    /**
     * Session store type: memory, jdbc, redis (memory only implemented by default).
     */
    private String sessionStore = "memory";

    public int getMaxTurns() {
        return maxTurns;
    }

    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    public String getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(String sessionStore) {
        this.sessionStore = sessionStore;
    }
}

