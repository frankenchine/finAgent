/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

import com.agent4j.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of an agent run. Contains final output, last agent, raw responses, and guardrail results.
 */
public final class RunResult {

    private final Object finalOutput;
    private final Agent lastAgent;
    private final List<Message> rawResponses;
    private final Object input;
    private final List<Object> inputGuardrailResults;
    private final List<Object> outputGuardrailResults;
    private final int maxTurns;
    private final int currentTurn;

    private RunResult(Builder b) {
        this.finalOutput = b.finalOutput;
        this.lastAgent = b.lastAgent;
        this.rawResponses = b.rawResponses != null
                ? Collections.unmodifiableList(new ArrayList<>(b.rawResponses))
                : Collections.<Message>emptyList();
        this.input = b.input;
        this.inputGuardrailResults = b.inputGuardrailResults != null
                ? Collections.unmodifiableList(new ArrayList<>(b.inputGuardrailResults))
                : Collections.<Object>emptyList();
        this.outputGuardrailResults = b.outputGuardrailResults != null
                ? Collections.unmodifiableList(new ArrayList<>(b.outputGuardrailResults))
                : Collections.<Object>emptyList();
        this.maxTurns = b.maxTurns;
        this.currentTurn = b.currentTurn;
    }

    public Object getFinalOutput() {
        return finalOutput;
    }

    public Agent getLastAgent() {
        return lastAgent;
    }

    public List<Message> getRawResponses() {
        return rawResponses;
    }

    public Object getInput() {
        return input;
    }

    public List<Object> getInputGuardrailResults() {
        return inputGuardrailResults;
    }

    public List<Object> getOutputGuardrailResults() {
        return outputGuardrailResults;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Object finalOutput;
        private Agent lastAgent;
        private List<Message> rawResponses;
        private Object input;
        private List<Object> inputGuardrailResults;
        private List<Object> outputGuardrailResults;
        private int maxTurns;
        private int currentTurn;

        public Builder finalOutput(Object finalOutput) {
            this.finalOutput = finalOutput;
            return this;
        }

        public Builder lastAgent(Agent lastAgent) {
            this.lastAgent = lastAgent;
            return this;
        }

        public Builder rawResponses(List<Message> rawResponses) {
            this.rawResponses = rawResponses;
            return this;
        }

        public Builder input(Object input) {
            this.input = input;
            return this;
        }

        public Builder inputGuardrailResults(List<Object> inputGuardrailResults) {
            this.inputGuardrailResults = inputGuardrailResults;
            return this;
        }

        public Builder outputGuardrailResults(List<Object> outputGuardrailResults) {
            this.outputGuardrailResults = outputGuardrailResults;
            return this;
        }

        public Builder maxTurns(int maxTurns) {
            this.maxTurns = maxTurns;
            return this;
        }

        public Builder currentTurn(int currentTurn) {
            this.currentTurn = currentTurn;
            return this;
        }

        public RunResult build() {
            return new RunResult(this);
        }
    }
}

