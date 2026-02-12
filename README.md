# finAgent

Lightweight multi-agent framework for Spring Boot, inspired by [openai-agents-python](https://github.com/openai/openai-agents-python). It provides interface-driven abstractions for agents, tools, handoffs, sessions, and guardrails.

## Features

- **Agent**: Stateless configuration (name, instructions, tools, handoffs, guardrails).
- **AgentRunner**: Runs the agent loop until final output or max turns; supports sync and async.
- **Session**: Conversation history (e.g. `InMemorySession`); pluggable via `Session` interface.
- **Tools**: `Tool` interface with name, description, parameter schema, and `invoke(ToolContext)`.
- **Handoffs**: Transfer control to another agent; exposed as tools to the LLM.
- **Guardrails**: Input (before LLM) and output (after final response) validation/transformation.
- **ModelInvoker**: LLM abstraction; implement or use Spring AI (see below).

## Quick Start

1. Provide a `ModelInvoker` bean (or use the demo's `MockModelInvoker` when none is present).
2. Build an agent with `AgentDefinition`, then run it:

```java
Agent agent = new AgentDefinition()
    .setName("Assistant")
    .setInstructions("You are a helpful assistant.")
    .addTool(FunctionToolRegistry.stringArgTool("get_weather", "Get weather for a city", "city", city -> "Sunny in " + city))
    .build();

RunResult result = runner.run(agent, RunRequest.builder()
    .input("What's the weather in Tokyo?")
    .maxTurns(10)
    .build());

System.out.println(result.getFinalOutput());
```

3. With session (conversation memory):

```java
Session session = new InMemorySession("user_123");
runner.run(agent, RunRequest.builder().input("Hello").session(session).build());
runner.run(agent, RunRequest.builder().input("What did I say?").session(session).build());
```

## Spring AI Integration

To use Spring AI's `ChatModel` as the LLM backend:

1. Add to your `pom.xml`:
   - `spring-ai-core`
   - `spring-ai-openai-spring-boot-starter` (or another provider)

2. Implement `ModelInvoker` by delegating to `ChatModel.call(Prompt)` and mapping:
   - Your `Message` list → Spring AI `Prompt`
   - Spring AI `ChatResponse` / tool calls → `ModelInvocationResponse`

Alternatively, you can add a `ModelInvoker` bean that wraps your `ChatModel` in the same way as the (optional) `SpringAiChatModelAdapter` pattern described in the docs.

## Configuration

```yaml
finagent:
  max-turns: 20
  session-store: memory
```

## Running the Demo

```bash
mvn spring-boot:run -Dspring-boot.run.mainClass=com.finagent.demo.DemoApplication
```

The demo uses `MockModelInvoker` when no other `ModelInvoker` is configured and runs a simple agent plus a session example.

## License

MIT
