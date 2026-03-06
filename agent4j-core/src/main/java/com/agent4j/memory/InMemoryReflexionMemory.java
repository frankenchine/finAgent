/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.memory;

import com.agent4j.api.ReflexionMemory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of ReflexionMemory. Reflections are stored in chronological order.
 */
public class InMemoryReflexionMemory implements ReflexionMemory {

    private final String sessionId;
    private final List<String> reflections = new CopyOnWriteArrayList<>();

    public InMemoryReflexionMemory(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public List<String> getReflections(Integer limit) {
        if (limit == null || limit <= 0) {
            return List.copyOf(reflections);
        }
        int size = reflections.size();
        if (size <= limit) {
            return List.copyOf(reflections);
        }
        return List.copyOf(reflections.subList(size - limit, size));
    }

    @Override
    public void addReflection(String reflection) {
        if (reflection != null && !reflection.isBlank()) {
            reflections.add(reflection.trim());
        }
    }

    @Override
    public void clear() {
        reflections.clear();
    }
}
