package com.finagent.core;

import com.finagent.model.ModelInvocationRequest;
import com.finagent.model.ModelInvocationResponse;

import java.util.List;

/**
 * Test-only ModelInvoker that returns a sequence of preset responses per invoke() call.
 * Used to simulate multi-turn flows (e.g. first call tool_calls, second call final text).
 */
public class SequentialMockModelInvoker implements ModelInvoker {

    private final List<ModelInvocationResponse> responses;
    private final ModelInvocationResponse defaultResponse;
    private int index;

    /**
     * @param responses list of responses to return in order; once exhausted, defaultResponse is used
     * @param defaultResponse response when index >= responses.size()
     */
    public SequentialMockModelInvoker(List<ModelInvocationResponse> responses,
                                      ModelInvocationResponse defaultResponse) {
        this.responses = responses != null ? List.copyOf(responses) : List.of();
        this.defaultResponse = defaultResponse != null
                ? defaultResponse
                : new ModelInvocationResponse("", List.of());
    }

    /**
     * Uses empty string response with no tool_calls as default when sequence is exhausted.
     */
    public SequentialMockModelInvoker(List<ModelInvocationResponse> responses) {
        this(responses, new ModelInvocationResponse("", List.of()));
    }

    @Override
    public ModelInvocationResponse invoke(ModelInvocationRequest request) {
        if (index < responses.size()) {
            return responses.get(index++);
        }
        return defaultResponse;
    }

    public int getInvokeCount() {
        return index;
    }
}
