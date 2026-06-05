/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

/**
 * Lightweight event emitted by the runner for tracing, hooks, and streaming.
 */
public final class RunEvent {

    public enum Type {
        RUN_STARTED,
        AGENT_STARTED,
        MODEL_STARTED,
        MODEL_DELTA,
        MODEL_COMPLETED,
        TOOL_STARTED,
        TOOL_COMPLETED,
        HANDOFF,
        GUARDRAIL,
        RUN_COMPLETED
    }

    private final Type type;
    private final Agent agent;
    private final String name;
    private final Object payload;
    private final int turn;

    private RunEvent(Type type, Agent agent, String name, Object payload, int turn) {
        this.type = type;
        this.agent = agent;
        this.name = name;
        this.payload = payload;
        this.turn = turn;
    }

    public static RunEvent of(Type type, Agent agent, String name, Object payload, int turn) {
        return new RunEvent(type, agent, name, payload, turn);
    }

    public Type getType() {
        return type;
    }

    public Agent getAgent() {
        return agent;
    }

    public String getName() {
        return name;
    }

    public Object getPayload() {
        return payload;
    }

    public int getTurn() {
        return turn;
    }
}
