/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.api;

import java.util.List;

/**
 * Storage for reflection insights across Reflexion trials.
 * Used to persist "what went wrong" and "what to try differently" between attempts.
 */
public interface ReflexionMemory {

    String getSessionId();

    /**
     * Retrieve reflection texts. When limit is specified, returns the latest N in chronological order.
     *
     * @param limit maximum number of reflections to return; null means all
     * @return list of reflection strings (oldest first within the returned subset)
     */
    List<String> getReflections(Integer limit);

    void addReflection(String reflection);

    void clear();
}
