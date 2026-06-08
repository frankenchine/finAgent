/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.api;

import java.util.List;

/**
 * An agent is an LLM configured with instructions, tools, handoffs, and guardrails.
 * Stateless configuration only; does not hold Runner or Session.
 * tools handoffs inputGuardrails outputGuardrails all are agent-level configurations, 
 */
public interface Agent {

    String getName();

    /**
     * System prompt / instructions for the agent.
     */
    String getInstructions();

    List<Tool> getTools();

    List<Handoff> getHandoffs();

    List<InputGuardrail> getInputGuardrails();

    List<OutputGuardrail> getOutputGuardrails();

    /**
     * Optional: structured output type (e.g. String class). First version supports String only.
     */
    Class<?> getOutputType();
}

