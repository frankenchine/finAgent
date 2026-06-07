/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

/**
 * Serializable event DTO for exposing runner events over SSE or other transports.
 */
public final class AgentStreamEvent {

    private final String type;
    private final String name;
    private final int turn;
    private final String delta;
    private final Object data;
    private final String error;

    private AgentStreamEvent(String type, String name, int turn, String delta, Object data, String error) {
        this.type = type;
        this.name = name;
        this.turn = turn;
        this.delta = delta;
        this.data = data;
        this.error = error;
    }

    public static AgentStreamEvent fromRunEvent(RunEvent event) {
        Object payload = event.getPayload();
        String delta = event.getType() == RunEvent.Type.MODEL_DELTA && payload != null ? payload.toString() : null;
        String error = payload instanceof Throwable ? ((Throwable) payload).getMessage() : null;
        Object data = toData(event, delta, error);
        return new AgentStreamEvent(toEventName(event.getType()), event.getName(), event.getTurn(), delta, data, error);
    }

    public static AgentStreamEvent error(String type, String message) {
        return new AgentStreamEvent(type, null, 0, null, null, message);
    }

    public static String toEventName(RunEvent.Type type) {
        return type.name().toLowerCase();
    }

    private static Object toData(RunEvent event, String delta, String error) {
        if (delta != null || error != null) {
            return null;
        }
        if (event.getType() == RunEvent.Type.RUN_COMPLETED && event.getPayload() instanceof RunResult) {
            RunResult result = (RunResult) event.getPayload();
            String lastAgentName = result.getLastAgent() != null ? result.getLastAgent().getName() : null;
            return new RunCompletedData(result.getFinalOutput(), lastAgentName, result.getCurrentTurn(), result.getMaxTurns());
        }
        if (event.getType() == RunEvent.Type.AGENT_STARTED && event.getAgent() != null) {
            return new NamedData(event.getAgent().getName());
        }
        if (event.getType() == RunEvent.Type.HANDOFF && event.getPayload() instanceof Agent) {
            return new NamedData(((Agent) event.getPayload()).getName());
        }
        return event.getName() != null ? new NamedData(event.getName()) : null;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getTurn() {
        return turn;
    }

    public String getDelta() {
        return delta;
    }

    public Object getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public static final class NamedData {
        private final String name;

        private NamedData(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static final class RunCompletedData {
        private final Object finalOutput;
        private final String lastAgentName;
        private final int currentTurn;
        private final int maxTurns;

        private RunCompletedData(Object finalOutput, String lastAgentName, int currentTurn, int maxTurns) {
            this.finalOutput = finalOutput;
            this.lastAgentName = lastAgentName;
            this.currentTurn = currentTurn;
            this.maxTurns = maxTurns;
        }

        public Object getFinalOutput() {
            return finalOutput;
        }

        public String getLastAgentName() {
            return lastAgentName;
        }

        public int getCurrentTurn() {
            return currentTurn;
        }

        public int getMaxTurns() {
            return maxTurns;
        }
    }
}
