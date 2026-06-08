/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.memory;

import com.agent4j.api.Session;
import com.agent4j.model.Message;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of Session. Items are stored in a list (oldest first).
 */
public class InMemorySession implements Session {

    private final String sessionId;
    private final List<Message> items = new CopyOnWriteArrayList<>();

    public InMemorySession(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public List<Message> getItems(Integer limit) {
        if (limit == null || limit <= 0) {
            return List.copyOf(items);
        }
        int size = items.size();
        if (size <= limit) {
            return List.copyOf(items);
        }
        return List.copyOf(items.subList(size - limit, size));
    }

    @Override
    public void addItems(List<Message> newItems) {
        if (newItems != null && !newItems.isEmpty()) {
            items.addAll(newItems);
        }
    }

    @Override
    public Message popItem() {
        return items.isEmpty() ? null : items.remove(items.size() - 1);
    }

    @Override
    public void clear() {
        items.clear();
    }
}

