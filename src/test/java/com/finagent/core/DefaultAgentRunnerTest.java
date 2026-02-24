package com.finagent.core;

import com.finagent.api.*;
import com.finagent.memory.InMemorySession;
import com.finagent.model.ModelInvocationResponse;
import com.finagent.tools.FunctionToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Multi-scenario unit tests for DefaultAgentRunner using mock ModelInvoker only (no real LLM).
 */
class DefaultAgentRunnerTest {

    // --- 1. Single turn, no tools ---
    @Test
    void singleTurnNoTools() {
        ModelInvocationResponse only = new ModelInvocationResponse("Hello", List.of());
        SequentialMockModelInvoker mock = new SequentialMockModelInvoker(List.of(only));
        AgentRunner runner = new DefaultAgentRunner(mock);

        Agent agent = new AgentDefinition()
                .setName("test")
                .setInstructions("You are helpful.")
                .build();

        RunResult result = runner.run(agent, RunRequest.builder()
                .input("Hi")
                .maxTurns(5)
                .build());

        assertThat(result.getFinalOutput()).isEqualTo("Hello");
        assertThat(result.getCurrentTurn()).isEqualTo(1);
        assertThat(mock.getInvokeCount()).isEqualTo(1);
    }

    // --- 2. Tool call then finish ---
    @Test
    void toolCallThenFinish() {
        ModelInvocationResponse withTool = new ModelInvocationResponse("",
                List.of(new ModelInvocationResponse.ToolCall("tc1", "get_weather", "{\"city\":\"Beijing\"}")));
        ModelInvocationResponse finalText = new ModelInvocationResponse("It is sunny in Beijing.", List.of());
        SequentialMockModelInvoker mock = new SequentialMockModelInvoker(List.of(withTool, finalText));

        Agent agent = new AgentDefinition()
                .setName("test")
                .setInstructions("You are helpful.")
                .addTool(FunctionToolRegistry.stringArgTool("get_weather", "Get weather", "city", city -> "Sunny in " + city))
                .build();

        AgentRunner runner = new DefaultAgentRunner(mock);
        RunResult result = runner.run(agent, RunRequest.builder()
                .input("Weather in Beijing?")
                .maxTurns(5)
                .build());

        assertThat(result.getCurrentTurn()).isEqualTo(2);
        assertThat(result.getFinalOutput()).isEqualTo("It is sunny in Beijing.");
        assertThat(mock.getInvokeCount()).isEqualTo(2);
        assertThat(result.getRawResponses()).hasSize(2);
    }

    // --- 3. Session multi-turn ---
    @Test
    void sessionMultiTurn() {
        // First run: return "First reply". Second run: return "Second reply" (mock sees history via session).
        SequentialMockModelInvoker mock = new SequentialMockModelInvoker(
                List.of(
                        new ModelInvocationResponse("First reply", List.of()),
                        new ModelInvocationResponse("Second reply", List.of())
                ));
        AgentRunner runner = new DefaultAgentRunner(mock);
        Agent agent = new AgentDefinition()
                .setName("test")
                .setInstructions("Echo last user message.")
                .build();

        InMemorySession session = new InMemorySession("s1");
        RunResult r1 = runner.run(agent, RunRequest.builder()
                .input("Hello")
                .session(session)
                .maxTurns(5)
                .build());
        RunResult r2 = runner.run(agent, RunRequest.builder()
                .input("Bye")
                .session(session)
                .maxTurns(5)
                .build());

        assertThat(r1.getFinalOutput()).isEqualTo("First reply");
        assertThat(r2.getFinalOutput()).isEqualTo("Second reply");
        assertThat(session.getItems(null)).isNotEmpty();
    }

    // --- 4. Input guardrail reject ---
    @Test
    void inputGuardrailReject() {
        SequentialMockModelInvoker mock = new SequentialMockModelInvoker(
                List.of(new ModelInvocationResponse("Should not be called", List.of())));
        AgentRunner runner = new DefaultAgentRunner(mock);

        InputGuardrail rejectAll = (messages, context) -> InputGuardrail.InputGuardrailResult.reject("forbidden");
        Agent agent = new AgentDefinition()
                .setName("test")
                .setInstructions("Help.")
                .addInputGuardrail(rejectAll)
                .build();

        RunResult result = runner.run(agent, RunRequest.builder()
                .input("Hi")
                .maxTurns(5)
                .build());

        assertThat(String.valueOf(result.getFinalOutput())).contains("Input rejected");
        assertThat(result.getCurrentTurn()).isEqualTo(0);
        assertThat(result.getInputGuardrailResults()).isNotEmpty();
        assertThat(mock.getInvokeCount()).isEqualTo(0);
    }

    // --- 5. Output guardrail transform ---
    @Test
    void outputGuardrailTransform() {
        ModelInvocationResponse response = new ModelInvocationResponse("hello world", List.of());
        SequentialMockModelInvoker mock = new SequentialMockModelInvoker(List.of(response));
        AgentRunner runner = new DefaultAgentRunner(mock);

        OutputGuardrail toUpper = (output, context) ->
                OutputGuardrail.OutputGuardrailResult.pass(output != null ? output.toString().toUpperCase() : "");
        Agent agent = new AgentDefinition()
                .setName("test")
                .setInstructions("Help.")
                .addOutputGuardrail(toUpper)
                .build();

        RunResult result = runner.run(agent, RunRequest.builder()
                .input("Say hello")
                .maxTurns(5)
                .build());

        assertThat(result.getFinalOutput()).isEqualTo("HELLO WORLD");
    }

    // --- 6. Max turns reached ---
    @Test
    void maxTurnsReached() {
        ModelInvocationResponse toolOnly = new ModelInvocationResponse("",
                List.of(new ModelInvocationResponse.ToolCall("tc1", "get_weather", "{\"city\":\"X\"}")));
        SequentialMockModelInvoker mock = new SequentialMockModelInvoker(
                List.of(toolOnly, toolOnly, toolOnly),
                toolOnly);
        Agent agent = new AgentDefinition()
                .setName("test")
                .setInstructions("Help.")
                .addTool(FunctionToolRegistry.stringArgTool("get_weather", "Get weather", "city", city -> "Sunny"))
                .build();
        AgentRunner runner = new DefaultAgentRunner(mock);
        int maxTurns = 3;

        RunResult result = runner.run(agent, RunRequest.builder()
                .input("Weather?")
                .maxTurns(maxTurns)
                .build());

        assertThat(result.getCurrentTurn()).isEqualTo(maxTurns);
        assertThat(result.getFinalOutput()).isEqualTo("");
    }

    // --- 7. Handoff ---
    @Test
    void handoff() {
        Agent agentB = new AgentDefinition()
                .setName("agentB")
                .setInstructions("I am B.")
                .build();
        String handoffToolName = "transfer_to_b";
        Handoff handoffToB = new Handoff() {
            @Override
            public String getToolName() {
                return handoffToolName;
            }

            @Override
            public String getToolDescription() {
                return "Transfer to B";
            }

            @Override
            public Agent getTargetAgent() {
                return agentB;
            }
        };

        ModelInvocationResponse handoffCall = new ModelInvocationResponse("",
                List.of(new ModelInvocationResponse.ToolCall("h1", handoffToolName, "{}")));
        ModelInvocationResponse finalFromB = new ModelInvocationResponse("Done by B", List.of());
        SequentialMockModelInvoker mock = new SequentialMockModelInvoker(List.of(handoffCall, finalFromB));

        Agent agentA = new AgentDefinition()
                .setName("agentA")
                .setInstructions("You may hand off to B.")
                .addHandoff(handoffToB)
                .build();
        AgentRunner runner = new DefaultAgentRunner(mock);

        RunResult result = runner.run(agentA, RunRequest.builder()
                .input("Hand off to B")
                .maxTurns(5)
                .build());

        assertThat(result.getLastAgent()).isSameAs(agentB);
        assertThat(result.getFinalOutput()).isEqualTo("Done by B");
        assertThat(result.getCurrentTurn()).isEqualTo(2);
    }

    // --- 8. Unknown tool ---
    @Test
    void unknownTool() {
        ModelInvocationResponse unknownToolCall = new ModelInvocationResponse("",
                List.of(new ModelInvocationResponse.ToolCall("tc1", "unknown_tool", "{}")));
        ModelInvocationResponse finalText = new ModelInvocationResponse("I could not use that tool.", List.of());
        SequentialMockModelInvoker mock = new SequentialMockModelInvoker(List.of(unknownToolCall, finalText));

        Agent agent = new AgentDefinition()
                .setName("test")
                .setInstructions("Help.")
                .addTool(FunctionToolRegistry.stringArgTool("get_weather", "Get weather", "city", city -> "Sunny"))
                .build();
        AgentRunner runner = new DefaultAgentRunner(mock);

        RunResult result = runner.run(agent, RunRequest.builder()
                .input("Use unknown_tool")
                .maxTurns(5)
                .build());

        assertThat(result.getCurrentTurn()).isEqualTo(2);
        assertThat(result.getFinalOutput()).isEqualTo("I could not use that tool.");
        assertThat(result.getRawResponses()).hasSize(2);
    }
}
