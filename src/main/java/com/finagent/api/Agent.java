package com.finagent.api;

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
