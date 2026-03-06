/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

import com.agent4j.model.Message;

import java.util.List;

/**
 * Request for a Reflexion multi-trial run. Wraps RunRequest with reflexion-specific fields.
 */
public final class ReflexionRunRequest {

    private final RunRequest baseRequest;
    private final ReflexionMemory reflexionMemory;
    private final TrialEvaluator trialEvaluator;
    private final ReflexionRunConfig config;

    private ReflexionRunRequest(Builder b) {
        this.baseRequest = b.baseRequest;
        this.reflexionMemory = b.reflexionMemory;
        this.trialEvaluator = b.trialEvaluator;
        this.config = b.config != null ? b.config : ReflexionRunConfig.builder().build();
    }

    public RunRequest getBaseRequest() {
        return baseRequest;
    }

    public ReflexionMemory getReflexionMemory() {
        return reflexionMemory;
    }

    public TrialEvaluator getTrialEvaluator() {
        return trialEvaluator;
    }

    public ReflexionRunConfig getConfig() {
        return config;
    }

    public Object getInput() {
        return baseRequest.getInput();
    }

    public List<Message> getInputAsMessages() {
        return baseRequest.getInputAsMessages();
    }

    public Object getContext() {
        return baseRequest.getContext();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private RunRequest baseRequest;
        private ReflexionMemory reflexionMemory;
        private TrialEvaluator trialEvaluator;
        private ReflexionRunConfig config;

        public Builder baseRequest(RunRequest request) {
            this.baseRequest = request;
            return this;
        }

        public Builder input(String input) {
            this.baseRequest = RunRequest.builder().input(input).build();
            return this;
        }

        public Builder input(String input, Session session) {
            this.baseRequest = RunRequest.builder().input(input).session(session).build();
            return this;
        }

        public Builder input(String input, Session session, int maxTurns, Object context) {
            this.baseRequest = RunRequest.builder()
                    .input(input)
                    .session(session)
                    .maxTurns(maxTurns)
                    .context(context)
                    .build();
            return this;
        }

        public Builder reflexionMemory(ReflexionMemory memory) {
            this.reflexionMemory = memory;
            return this;
        }

        public Builder trialEvaluator(TrialEvaluator evaluator) {
            this.trialEvaluator = evaluator;
            return this;
        }

        public Builder config(ReflexionRunConfig config) {
            this.config = config;
            return this;
        }

        public ReflexionRunRequest build() {
            if (baseRequest == null) {
                throw new IllegalStateException("baseRequest or input is required");
            }
            if (reflexionMemory == null) {
                throw new IllegalStateException("reflexionMemory is required");
            }
            if (trialEvaluator == null) {
                throw new IllegalStateException("trialEvaluator is required");
            }
            return new ReflexionRunRequest(this);
        }
    }
}
