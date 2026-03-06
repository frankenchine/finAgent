/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.core;

import com.agent4j.api.RunResult;
import com.agent4j.api.TrialEvaluator;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * LLM-based trial evaluator. Calls the model to judge success/failure and produce feedback.
 */
public class LlmTrialEvaluator implements TrialEvaluator {

    private static final Pattern SUCCESS_PATTERN = Pattern.compile("\\b(success|yes|true|pass|correct|succeeded)\\b", Pattern.CASE_INSENSITIVE);

    private final ModelInvoker modelInvoker;
    private final String evaluationInstructions;

    public LlmTrialEvaluator(ModelInvoker modelInvoker) {
        this(modelInvoker, "Evaluate whether the agent's response successfully completes the task. " +
                "Reply with SUCCESS or FAILURE on the first line, then a brief feedback explaining why.");
    }

    public LlmTrialEvaluator(ModelInvoker modelInvoker, String evaluationInstructions) {
        this.modelInvoker = modelInvoker;
        this.evaluationInstructions = evaluationInstructions != null ? evaluationInstructions : "";
    }

    @Override
    public TrialEvaluation evaluate(RunResult result, Object context) {
        String task = context != null ? context.toString() : "";
        String outcome = result.getFinalOutput() != null ? result.getFinalOutput().toString() : "";

        String systemPrompt = evaluationInstructions;
        String userContent = "Task: " + task + "\n\nAgent's response: " + outcome;

        ModelInvocationRequest request = new ModelInvocationRequest(
                systemPrompt,
                List.of(Message.user(userContent)),
                List.of()
        );
        ModelInvocationResponse response = modelInvoker.invoke(request);
        String text = response.getAssistantText();
        if (text == null || text.isBlank()) {
            return TrialEvaluation.failure("No evaluation returned.");
        }

        String firstLine = text.lines().findFirst().orElse("").trim();
        String rest = text.contains("\n") ? text.substring(text.indexOf('\n') + 1).trim() : "";
        String feedback = rest.isBlank() ? text.trim() : rest;

        boolean success = firstLine.toUpperCase(Locale.ROOT).contains("SUCCESS") || SUCCESS_PATTERN.matcher(firstLine).find();
        return new TrialEvaluation(success, feedback);
    }
}
