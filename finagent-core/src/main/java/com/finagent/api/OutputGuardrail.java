package com.finagent.api;

/**
 * Guardrail that runs on the final output before returning to the user.
 * Can validate or transform the output.
 */
public interface OutputGuardrail {

    /**
     * Process the final output before returning.
     *
     * @param output the agent's final output (e.g. String)
     * @param context optional run context
     * @return result with either passed/transformed output or rejection
     */
    OutputGuardrailResult process(Object output, Object context);

    final class OutputGuardrailResult {
        private final boolean passed;
        private final Object output;
        private final String rejectReason;

        public OutputGuardrailResult(boolean passed, Object output, String rejectReason) {
            this.passed = passed;
            this.output = output;
            this.rejectReason = rejectReason;
        }

        public static OutputGuardrailResult pass(Object output) {
            return new OutputGuardrailResult(true, output, null);
        }

        public static OutputGuardrailResult reject(String reason) {
            return new OutputGuardrailResult(false, null, reason);
        }

        public boolean isPassed() {
            return passed;
        }

        public Object getOutput() {
            return output;
        }

        public String getRejectReason() {
            return rejectReason;
        }
    }
}

