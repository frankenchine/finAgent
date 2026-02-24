package com.finagent.api;

import com.finagent.model.Message;

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

