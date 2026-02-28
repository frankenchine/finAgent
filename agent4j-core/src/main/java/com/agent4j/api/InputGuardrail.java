package com.agent4j.api;

import com.agent4j.model.Message;

import java.util.List;

/**
 * Guardrail that runs before the LLM is invoked (only for the first agent in the chain).
 * Can validate or transform input; can reject with a reason.
 */
public interface InputGuardrail {

    /**
     * Process input before it is sent to the LLM.
     *
     * @param input current input messages
     * @param context optional run context
     * @return result with either passed/transformed messages or rejection
     */
    InputGuardrailResult process(List<Message> input, Object context);

    final class InputGuardrailResult {
        private final boolean passed;
        private final List<Message> messages;
        private final String rejectReason;

        public InputGuardrailResult(boolean passed, List<Message> messages, String rejectReason) {
            this.passed = passed;
            this.messages = messages != null ? List.copyOf(messages) : List.of();
            this.rejectReason = rejectReason;
        }

        public static InputGuardrailResult pass(List<Message> messages) {
            return new InputGuardrailResult(true, messages, null);
        }

        public static InputGuardrailResult reject(String reason) {
            return new InputGuardrailResult(false, null, reason);
        }

        public boolean isPassed() {
            return passed;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public String getRejectReason() {
            return rejectReason;
        }
    }
}

