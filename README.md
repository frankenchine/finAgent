# finAgent

Lightweight multi-agent framework for Spring Boot, inspired by [openai-agents-python](https://github.com/openai/openai-agents-python). It provides interface-driven abstractions for agents, tools, handoffs, sessions, and guardrails. LLM calls use a built-in HTTP client for OpenAI-format APIs (e.g. OpenAI, DeepSeek); Spring AI is not required.

## Features

- **Agent**: Stateless configuration (name, instructions, tools, handoffs, guardrails).
- **AgentRunner**: Runs the agent loop until final output or max turns.
- **Session**: Conversation history (e.g. `InMemorySession`); pluggable via `Session` interface.
- **Tools**: `Tool` interface with name, description, parameter schema, and `invoke(ToolContext)`.
- **Handoffs**: Transfer control to another agent; exposed as tools to the LLM.
- **Guardrails**: Input (before LLM) and output (after final response) validation/transformation.
- **ModelInvoker**: LLM abstraction; built-in HTTP-based implementation for OpenAI and DeepSeek, configurable via `application.yml`; you can also provide your own `ModelInvoker` bean.

## Quick Start

1. **Configure the LLM**: Copy `src/main/resources/application.yml.example` to `application.yml` in the same directory, set `finagent.llm.api-key`, and do not commit `application.yml` (it is gitignored).
2. Inject `AgentRunner` and build an agent with `AgentDefinition`:

```java
Agent agent = new AgentDefinition()
    .setName("Assistant")
    .setInstructions("You are a helpful assistant. Reply concisely.")
    .addTool(FunctionToolRegistry.stringArgTool(
            "get_weather",
            "Get the weather for a city",
            "city",
            city -> "The weather in " + city + " is sunny."
    ))
    .build();

RunResult result = runner.run(agent, RunRequest.builder()
    .input("What's the weather in Tokyo?")
    .maxTurns(10)
    .build());

System.out.println("Final output: " + result.getFinalOutput());
```

3. With session (conversation memory):

```java
Session session = new InMemorySession("user_123");
runner.run(agent, RunRequest.builder().input("Hello").session(session).build());
runner.run(agent, RunRequest.builder().input("What did I say?").session(session).build());
```

## LLM and ModelInvoker

This project does not depend on Spring AI. The default LLM integration is a built-in HTTP client that talks to OpenAI-format APIs.

- **Configuration**: Under `finagent.llm` in `application.yml` you set `provider` (e.g. `openai` or `deepseek`), `base-url`, `api-key`, `model`, `temperature`, `max-tokens`, and `timeout-seconds`. When `api-key` is set, Spring Boot auto-configuration registers an `HttpModelInvoker` and the appropriate `LlmApiClient` (OpenAI or DeepSeek).
- **Override**: You can provide your own `ModelInvoker` bean (e.g. a mock or another API) to replace the default.

## Configuration

Copy `src/main/resources/application.yml.example` to `application.yml` in the same directory, set your LLM `api-key`, and run. Do not commit `application.yml` (it is gitignored); use the example file as the template.

Example `finagent.llm` settings (see `application.yml.example` for full content):

```yaml
finagent:
  llm:
    provider: deepseek
    base-url: https://api.deepseek.com/v1
    api-key: your-api-key-here
    model: deepseek-chat
    temperature: 0.7
    max-tokens: 2000
    timeout-seconds: 60
```

## Running the Demo

```bash
mvn spring-boot:run -Dspring-boot.run.mainClass=com.finagent.demo.DemoApplication
```

Ensure `application.yml` contains a valid `finagent.llm.api-key`. The demo runs three examples: an agent with a tool call, a session (multi-turn) example, and a handoff between two agents.

## License

MIT
