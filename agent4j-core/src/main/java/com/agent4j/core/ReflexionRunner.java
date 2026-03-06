/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.core;

import com.agent4j.api.*;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs an agent with Reflexion multi-trial loop: execute, evaluate, reflect, retry.
 */
public class ReflexionRunner {

    private static final String REFLECTION_PREFIX = "Reflections from previous attempts:\n\n";

    private final AgentRunner agentRunner;
    private final ModelInvoker modelInvoker;

    public ReflexionRunner(AgentRunner agentRunner, ModelInvoker modelInvoker) {
        this.agentRunner = agentRunner;
        this.modelInvoker = modelInvoker;
    }

    /**
     * Run the agent with Reflexion: multiple trials, reflection memory, and evaluation.
     */
    public RunResult run(Agent agent, ReflexionRunRequest reflexionRequest) {
        ReflexionMemory memory = reflexionRequest.getReflexionMemory();
        TrialEvaluator evaluator = reflexionRequest.getTrialEvaluator();
        ReflexionRunConfig config = reflexionRequest.getConfig();
        RunRequest baseRequest = reflexionRequest.getBaseRequest();
        Object taskContext = baseRequest.getInput() != null ? baseRequest.getInput().toString() : "";
        Object runContext = baseRequest.getContext();

        // 保证每次开始新的一轮时，清空之前的反思记忆
        memory.clear();

        RunResult lastResult = null;
        for (int trial = 1; trial <= config.getMaxTrials(); trial++) {
            List<Message> inputMessages = buildInputWithReflections(
                    reflexionRequest.getInputAsMessages(),
                    memory.getReflections(config.getReflectionLimit())
            );

            RunRequest trialRequest = RunRequest.builder()
                    .input(inputMessages)
                    .maxTurns(baseRequest.getMaxTurns())
                    .context(runContext)
                    .build();

            lastResult = agentRunner.run(agent, trialRequest);

            TrialEvaluator.TrialEvaluation evaluation = evaluator.evaluate(lastResult, taskContext);

            // 如果评估通过，则返回结果
            if (evaluation.isSuccess()) {
                return lastResult;
            }
            // 如果评估不通过，则进行反思，并存入反思记忆
            if (trial < config.getMaxTrials()) {
                String reflection = generateReflection(
                        modelInvoker,
                        config,
                        taskContext.toString(),
                        lastResult.getFinalOutput() != null ? lastResult.getFinalOutput().toString() : "",
                        evaluation.getFeedback()
                );
                if (reflection != null && !reflection.isBlank()) {
                    memory.addReflection(reflection);
                }
            }
        }

        return lastResult != null ? lastResult : RunResult.builder()
                .input(baseRequest.getInput())
                .finalOutput("No result after " + config.getMaxTrials() + " trials.")
                .maxTurns(baseRequest.getMaxTurns())
                .currentTurn(0)
                .build();
    }

    private List<Message> buildInputWithReflections(List<Message> originalInput, List<String> reflections) {
        if (reflections == null || reflections.isEmpty()) {
            return new ArrayList<>(originalInput);
        }
        String reflectionBlock = REFLECTION_PREFIX + String.join("\n\n", reflections);
        List<Message> result = new ArrayList<>();
        result.add(Message.user(reflectionBlock));
        result.addAll(originalInput);
        return result;
    }

    private String generateReflection(ModelInvoker invoker, ReflexionRunConfig config,
                                     String task, String outcome, String feedback) {
        String prompt = config.buildReflectPrompt(task, outcome, feedback);
        ModelInvocationRequest request = new ModelInvocationRequest(
                "You are a reflective assistant. Write a brief, actionable reflection.",
                List.of(Message.user(prompt)),
                List.of()
        );
        ModelInvocationResponse response = invoker.invoke(request);
        return response != null ? response.getAssistantText() : null;
    }
}
