/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

/**
 * Evaluates whether a single agent run (trial) succeeded or failed.
 * Used by ReflexionRunner to decide whether to retry with reflection.
 */
public interface TrialEvaluator {

    /**
     * Evaluate the outcome of a trial.
     *
     * @param result the RunResult from the trial
     * @param context optional context (e.g. original task, RunRequest context)
     * @return evaluation with success flag and optional feedback for reflection prompt
     */
    TrialEvaluation evaluate(RunResult result, Object context);

    /**
     * Result of evaluating a trial.
     */
    final class TrialEvaluation {
        private final boolean success;
        private final String feedback;

        public TrialEvaluation(boolean success, String feedback) {
            this.success = success;
            this.feedback = feedback != null ? feedback : "";
        }

        public static TrialEvaluation success() {
            return new TrialEvaluation(true, "");
        }

        public static TrialEvaluation success(String feedback) {
            return new TrialEvaluation(true, feedback);
        }

        public static TrialEvaluation failure(String feedback) {
            return new TrialEvaluation(false, feedback);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getFeedback() {
            return feedback;
        }
    }
}
