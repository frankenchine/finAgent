/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.llm;

import com.agent4j.core.ModelInvoker;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade that agents use to invoke models via routing logic.
 * It delegates model selection to {@link LlmRoutingStrategy} and then
 * dispatches the call to the appropriate {@link ModelInvoker} from the registry.
 */
public class LlmRouter {

    private final ModelInvokerRegistry registry;
    private final LlmRoutingStrategy routingStrategy;

    public LlmRouter(ModelInvokerRegistry registry, LlmRoutingStrategy routingStrategy) {
        this.registry = registry;
        this.routingStrategy = routingStrategy;
    }

    public ModelInvocationResponse routeAndInvoke(RoutingContext context, ModelInvocationRequest request) {
        LlmRoutingDecision decision = routingStrategy.decide(context);

        List<ModelIdentifier> candidates = new ArrayList<>();
        candidates.add(decision.getPrimaryModel());
        candidates.addAll(decision.getFallbackModels());

        RuntimeException lastError = null;

        for (ModelIdentifier candidate : candidates) {
            ModelInvoker invoker = registry.get(candidate);
            if (invoker == null) {
                continue;
            }
            try {
                return invoker.invoke(request);
            } catch (RuntimeException ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new IllegalStateException("No available ModelInvoker for routing decision " + decision.getPrimaryModel());
    }
}

