/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

/**
 * Optional lifecycle hooks for a run. Implement only the callbacks you need.
 */
public interface RunHooks {

    default void onEvent(RunEvent event) {
    }

    default void onRunStart(RunEvent event) {
        onEvent(event);
    }

    default void onRunComplete(RunEvent event) {
        onEvent(event);
    }

    default void onModelStart(RunEvent event) {
        onEvent(event);
    }

    default void onModelComplete(RunEvent event) {
        onEvent(event);
    }

    default void onModelDelta(RunEvent event) {
        onEvent(event);
    }

    default void onToolStart(RunEvent event) {
        onEvent(event);
    }

    default void onToolComplete(RunEvent event) {
        onEvent(event);
    }

    default void onHandoff(RunEvent event) {
        onEvent(event);
    }

    default void onGuardrail(RunEvent event) {
        onEvent(event);
    }
}
