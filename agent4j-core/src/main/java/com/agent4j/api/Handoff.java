package com.agent4j.api;

/**
 * A handoff represents transfer of control to another agent.
 * The runner treats handoffs as special tools: when the LLM "calls" a handoff,
 * the current agent is switched to the handoff's target agent.
 */
public interface Handoff {

    /**
     * Tool name exposed to the LLM (e.g. "transfer_to_spanish_agent").
     */
    String getToolName();

    /**
     * Description for the LLM to decide when to hand off.
     */
    String getToolDescription();

    /**
     * The agent to switch to when this handoff is invoked.
     */
    Agent getTargetAgent();
}

