/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

import com.agent4j.model.Message;

import java.util.List;

/**
 * Session protocol for conversation history management across agent runs.
 * Aligns with Python SDK: get_items, add_items, pop_item, clear_session.
 */
public interface Session {

    String getSessionId();

    /**
     * Retrieve conversation history, most recent first when limit is applied.
     */
    List<Message> getItems(Integer limit);

    void addItems(List<Message> items);

    Message popItem();

    void clear();
}

