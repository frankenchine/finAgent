/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

import com.agent4j.model.Message;
import java.util.Collections;
import java.util.List;

/**
 * Request for a single agent run. Contains input, optional session, max turns, and context.
 */
public final class RunRequest {

    private final Object input;
    private final Session session;
    private final int maxTurns;
    private final Object context;

    private RunRequest(Object input, Session session, int maxTurns, Object context) {
        this.input = input;
        this.session = session;
        this.maxTurns = maxTurns;
        this.context = context;
    }

    public Object getInput() {
        return input;
    }

    public Session getSession() {
        return session;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public Object getContext() {
        return context;
    }

    /**
     * Resolve input to a list of messages for the runner. If input is String, wrap as single user message.
     */
    @SuppressWarnings("unchecked")
    public List<Message> getInputAsMessages() {
        if (input instanceof String) {
            return List.of(Message.user((String) input));
        }
        if (input instanceof List) {
            return (List<Message>) input;
        }
        return Collections.emptyList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Object input;
        private Session session;
        private int maxTurns = 20;
        private Object context;

        public Builder input(String userMessage) {
            this.input = userMessage;
            return this;
        }

        public Builder input(List<Message> messages) {
            this.input = messages;
            return this;
        }

        public Builder session(Session session) {
            this.session = session;
            return this;
        }

        public Builder maxTurns(int maxTurns) {
            this.maxTurns = maxTurns;
            return this;
        }

        public Builder context(Object context) {
            this.context = context;
            return this;
        }

        public RunRequest build() {
            if (input == null) {
                throw new IllegalStateException("input is required");
            }
            return new RunRequest(input, session, maxTurns, context);
        }
    }
}

