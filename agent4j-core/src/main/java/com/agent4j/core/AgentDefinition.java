/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.core;

import com.agent4j.api.Agent;
import com.agent4j.api.Handoff;
import com.agent4j.api.InputGuardrail;
import com.agent4j.api.OutputGuardrail;
import com.agent4j.api.Tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable definition used to build an Agent. Implementations of Agent can delegate to this.
 */
public class AgentDefinition {

    private String name;
    private String instructions;
    private final List<Tool> tools = new ArrayList<>();
    private final List<Handoff> handoffs = new ArrayList<>();
    private final List<InputGuardrail> inputGuardrails = new ArrayList<>();
    private final List<OutputGuardrail> outputGuardrails = new ArrayList<>();
    private Class<?> outputType = String.class;

    public String getName() {
        return name;
    }

    public AgentDefinition setName(String name) {
        this.name = name;
        return this;
    }

    public String getInstructions() {
        return instructions;
    }

    public AgentDefinition setInstructions(String instructions) {
        this.instructions = instructions;
        return this;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public AgentDefinition addTool(Tool tool) {
        this.tools.add(tool);
        return this;
    }

    public AgentDefinition setTools(List<Tool> tools) {
        this.tools.clear();
        if (tools != null) {
            this.tools.addAll(tools);
        }
        return this;
    }

    public List<Handoff> getHandoffs() {
        return handoffs;
    }

    public AgentDefinition addHandoff(Handoff handoff) {
        this.handoffs.add(handoff);
        return this;
    }

    public AgentDefinition setHandoffs(List<Handoff> handoffs) {
        this.handoffs.clear();
        if (handoffs != null) {
            this.handoffs.addAll(handoffs);
        }
        return this;
    }

    public List<InputGuardrail> getInputGuardrails() {
        return inputGuardrails;
    }

    public AgentDefinition addInputGuardrail(InputGuardrail g) {
        this.inputGuardrails.add(g);
        return this;
    }

    public List<OutputGuardrail> getOutputGuardrails() {
        return outputGuardrails;
    }

    public AgentDefinition addOutputGuardrail(OutputGuardrail g) {
        this.outputGuardrails.add(g);
        return this;
    }

    public Class<?> getOutputType() {
        return outputType;
    }

    public AgentDefinition setOutputType(Class<?> outputType) {
        this.outputType = outputType != null ? outputType : String.class;
        return this;
    }

    public Agent build() {
        return new DefaultAgent(this);
    }

    private static final class DefaultAgent implements Agent {
        private final String name;
        private final String instructions;
        private final List<Tool> tools;
        private final List<Handoff> handoffs;
        private final List<InputGuardrail> inputGuardrails;
        private final List<OutputGuardrail> outputGuardrails;
        private final Class<?> outputType;

        DefaultAgent(AgentDefinition d) {
            this.name = d.name;
            this.instructions = d.instructions;
            this.tools = List.copyOf(d.tools);
            this.handoffs = List.copyOf(d.handoffs);
            this.inputGuardrails = List.copyOf(d.inputGuardrails);
            this.outputGuardrails = List.copyOf(d.outputGuardrails);
            this.outputType = d.outputType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getInstructions() {
            return instructions;
        }

        @Override
        public List<Tool> getTools() {
            return tools;
        }

        @Override
        public List<Handoff> getHandoffs() {
            return handoffs;
        }

        @Override
        public List<InputGuardrail> getInputGuardrails() {
            return inputGuardrails;
        }

        @Override
        public List<OutputGuardrail> getOutputGuardrails() {
            return outputGuardrails;
        }

        @Override
        public Class<?> getOutputType() {
            return outputType;
        }
    }
}

