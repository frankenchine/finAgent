package com.finagent.demo;

import com.finagent.core.ModelInvoker;
import com.finagent.model.ModelInvocationRequest;
import com.finagent.model.ModelInvocationResponse;

import java.util.List;

/**
 * Simple ModelInvoker for demos and tests when no real LLM is configured.
 * Returns a fixed response or echoes the last user message.
 */
public class MockModelInvoker implements ModelInvoker {

    private final boolean echoUserMessage;

    public MockModelInvoker() {
        this(true);
    }

    public MockModelInvoker(boolean echoUserMessage) {
        this.echoUserMessage = echoUserMessage;
    }

    @Override
    public ModelInvocationResponse invoke(ModelInvocationRequest request) {
        if (echoUserMessage && !request.getMessages().isEmpty()) {
            String lastUser = request.getMessages().stream()
                    .filter(m -> m.getMessageType() == com.finagent.model.Message.MessageType.USER)
                    .reduce((a, b) -> b)
                    .map(com.finagent.model.Message::getText)
                    .orElse("");
            if (!lastUser.isEmpty()) {
                return new ModelInvocationResponse("Echo: " + lastUser, List.of());
            }
        }
        return new ModelInvocationResponse("Hello from mock agent.", List.of());
    }
}
