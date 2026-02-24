# finAgent

finAgent 是一个面向 Spring Boot 的**轻量级多智能体（multi‑agent）框架**，设计灵感来自 [openai-agents-python](https://github.com/openai/openai-agents-python)。  
它通过一组接口抽象，把「智能体、工具调用、Agent 之间的 handoff、会话记忆、Guardrail」等概念封装成可插拔组件，并内置了对 **OpenAI 格式 HTTP 接口（OpenAI、DeepSeek 等）** 的访问实现，无需依赖 Spring AI。

当前仓库已经收敛为两个对外可复用的模块：

- `finagent-core`：核心能力与接口定义
- `finagent-spring-boot-starter`：Spring Boot 自动装配与配置封装（Starter）

---

## 模块结构

### 1. finagent-core

核心模块，包含所有与运行时逻辑相关的代码：

- `com.finagent.api`
  - `Agent`：无状态 Agent 配置（名称、说明、Tools、Handoffs、Guardrails、输出类型）。
  - `AgentRunner`：执行 Agent 的主入口，负责循环 LLM 调用直到终止。
  - `RunRequest` / `RunResult`：一次 Run 的请求和结果封装。
  - `Tool`：工具接口（名称、描述、参数 Schema、`invoke(ToolContext)`）。
  - `Handoff`：Agent 间的交接（由 LLM 通过“工具调用”的形式触发）。
  - `Session`：会话记忆接口（如 `InMemorySession`）。
  - `InputGuardrail` / `OutputGuardrail`：输入/输出 Guardrail。
- `com.finagent.core`
  - `DefaultAgentRunner`：默认的 AgentRunner 实现，负责：
    - 调用 `ModelInvoker`（LLM）
    - 判断是最终输出、工具调用还是 handoff
    - 处理 Guardrail 和 Session 记忆
  - `AgentDefinition`：用于构建 `Agent` 的可变配置类。
  - `ModelInvoker`：一次 LLM 调用的抽象接口。
- `com.finagent.model`
  - `Message`：统一的对话消息表示（SYSTEM/USER/ASSISTANT/TOOL）。
  - `ModelInvocationRequest` / `ModelInvocationResponse`：LLM 调用请求与返回结构（含 tool_calls）。
- `com.finagent.tools`
  - `FunctionToolRegistry`：快速把 Java 函数包装成 `Tool`。
  - `ToolExecutor`、`ToolInvocation`、`ToolSchema`：工具执行与 Schema 适配。
- `com.finagent.handoffs`
  - `HandoffResolver`、`HandoffToolAdapter`：把 Handoff 暴露给 LLM 并在收到调用时完成 Agent 切换。
- `com.finagent.memory`
  - `InMemorySession`：基于内存的 Session 实现。
- `com.finagent.llm`
  - `LlmApiClient`：LLM HTTP 客户端接口。
  - `HttpModelInvoker`：基于 `LlmApiClient` 的 `ModelInvoker` 实现。
  - `LlmProvider`：支持的 Provider 枚举（`OPENAI`、`DEEPSEEK`）。
  - `OpenAiApiClient` / `DeepSeekApiClient`：面向 OpenAI 格式聊天接口的 HTTP 客户端。
  - `dto.OpenAiRequest` / `dto.OpenAiResponse`：与 OpenAI Chat Completions 协议兼容的 DTO。
- `com.finagent.config`
  - `LlmProperties`：`finagent.llm.*` 配置属性（Provider、Base URL、API Key、模型、超时、温度等）。

### 2. finagent-spring-boot-starter

Starter 模块，负责 Spring Boot 自动装配与配置绑定：

- `com.finagent.config.AgentsAutoConfiguration`
  - 自动注入 `AgentRunner`：
    - 若容器中存在 `ModelInvoker` Bean 且没有用户自定义 `AgentRunner`，则创建 `DefaultAgentRunner`。
- `com.finagent.config.LlmAutoConfiguration`
  - 自动配置 LLM 相关 Bean：
    - `RestTemplate`（超时等）
    - `LlmApiClient`（根据 `finagent.llm.provider` 选择 OpenAI/DeepSeek）
    - `HttpModelInvoker`（默认 `ModelInvoker` 实现）
- `com.finagent.config.AgentsProperties`
  - `finagent.*` 级别的通用配置（如默认最大轮数、Session 存储方式等）。
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  - 注册上述自动配置类，使其在引入 starter 后自动生效。

---

## 在下游 Spring Boot 项目中使用

### 1. 添加依赖

在你的 Spring Boot 项目的 `pom.xml` 中加入：

xml
<dependency>
<groupId>com.finagent</groupId>
<artifactId>finagent-spring-boot-starter</artifactId>
<version>0.1.0-SNAPSHOT</version>
</dependency>


（假设你已经在本机或私服中安装/发布了该版本）

### 2. 配置 LLM

在下游项目的 `application.yml` 中配置 `finagent.llm` 属性，例如：

yaml
finagent:
llm:
provider: deepseek # openai 或 deepseek
base-url: https://api.deepseek.com/v1
api-key: your-api-key-here
model: deepseek-chat
temperature: 0.7
max-tokens: 2000
timeout-seconds: 60


当 `api-key` 配置完成后，Starter 会自动：

- 创建具备超时配置的 `RestTemplate`
- 按 `provider` 选择 `OpenAiApiClient` 或 `DeepSeekApiClient`
- 注册 `HttpModelInvoker` 作为默认 `ModelInvoker` Bean
- 再由 `AgentsAutoConfiguration` 创建一个默认的 `AgentRunner` Bean

### 3. 注入 AgentRunner 并构建 Agent

示例 Controller：
java
import com.finagent.api.AgentRunner;
import com.finagent.api.RunRequest;
import com.finagent.api.RunResult;
import com.finagent.api.Agent;
import com.finagent.core.AgentDefinition;
import com.finagent.tools.FunctionToolRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class ChatController {
private final AgentRunner agentRunner;
public ChatController(AgentRunner agentRunner) {
this.agentRunner = agentRunner;
}
@GetMapping("/chat")
public String chat(@RequestParam String q) {
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
RunResult result = agentRunner.run(
agent,
RunRequest.builder()
.input(q)
.maxTurns(10)
.build()
);
return String.valueOf(result.getFinalOutput());
}
}


---

## 会话记忆（Session）示例

使用 `InMemorySession` 在多次调用间共享上下文：

java
import com.finagent.api.Session;
import com.finagent.memory.InMemorySession;
// 创建会话
Session session = new InMemorySession("user_123");
// 第一次对话
agentRunner.run(agent, RunRequest.builder()
.input("Hello")
.session(session)
.build());
// 第二次对话，带上历史
agentRunner.run(agent, RunRequest.builder()
.input("What did I say?")
.session(session)
.build());


你也可以自定义 `Session` 实现（如 Redis、数据库），并在 `RunRequest.builder().session(...)` 中传入。

---

## 工具（Tool）与 Handoff

### Tool

通过 `FunctionToolRegistry` 快速把 Java 函数暴露给 LLM：

java
Agent agent = new AgentDefinition()
.setName("Assistant")
.setInstructions("You are a helpful assistant.")
.addTool(FunctionToolRegistry.stringArgTool(
"get_weather",
"Get the weather for a city",
"city",
city -> "The weather in " + city + " is sunny."
))
.build();


LLM 在回复中返回 `tool_calls` 时，`DefaultAgentRunner` 会自动：

1. 解析 `tool_calls` 列表
2. 构造 `ToolInvocation`
3. 通过 `ToolExecutor` 调用具体的 `Tool`
4. 把 Tool 执行结果作为 `TOOL` 消息追加到对话，再进行下一轮 LLM 调用

### Handoff

Handoff 通过「特殊 Tool」的方式让 LLM 选择切换到另一个 Agent：

java
Handoff toSpanish = new Handoff() {
@Override
public String getToolName() {
return "transfer_to_spanish_agent";
}
@Override
public String getToolDescription() {
return "Hand off to the Spanish-speaking agent.";
}
@Override
public Agent getTargetAgent() {
return spanishAgent;
}
};
Agent triageAgent = new AgentDefinition()
.setName("Triage agent")
.setInstructions("Route the request to the right language agent.")
.addHandoff(toSpanish)
.build();


当 LLM 调用 `transfer_to_spanish_agent` 时，`DefaultAgentRunner` 会使用 `HandoffResolver` 切换当前 Agent，并继续对话。

---

## Guardrail：输入与输出的约束

你可以为 Agent 配置：

- `InputGuardrail`：在第一次 LLM 调用前检查/修改输入，或直接拒绝。
- `OutputGuardrail`：在最终输出返回给用户前做校验/转换。

示例（输出转大写）：

java
OutputGuardrail toUpper = (output, context) ->
OutputGuardrail.OutputGuardrailResult.pass(
output != null ? output.toString().toUpperCase() : ""
);
Agent agent = new AgentDefinition()
.setName("test")
.setInstructions("Help.")
.addOutputGuardrail(toUpper)
.build();


---

## 自定义 ModelInvoker

如果你不想使用内置的 HTTP 客户端，可以自己实现 `ModelInvoker` 并注册为 Spring Bean：

java
import com.finagent.core.ModelInvoker;
import com.finagent.model.ModelInvocationRequest;
import com.finagent.model.ModelInvocationResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class CustomModelInvokerConfig {
@Bean
public ModelInvoker myModelInvoker() {
return new ModelInvoker() {
@Override
public ModelInvocationResponse invoke(ModelInvocationRequest request) {
// 在这里调用你自己的 LLM 服务，并构造 ModelInvocationResponse
return new ModelInvocationResponse("Hello from custom model", java.util.List.of());
}
};
}
}


当容器中存在你自定义的 `ModelInvoker` Bean 时，`LlmAutoConfiguration` 中默认的 HTTP 实现会被跳过，`AgentsAutoConfiguration` 会使用你的 Bean 创建 `DefaultAgentRunner`。

---

## 构建与安装

在本工程根目录执行：

bash
mvn -q -DskipTests clean install


会在本地 Maven 仓库生成：

- `com.finagent:finagent-core`
- `com.finagent:finagent-spring-boot-starter`

下游项目只需要依赖 `finagent-spring-boot-starter` 即可。

---

## 许可证

本项目使用 **MIT License**。你可以自由地在商业和非商业项目中使用、修改和分发（需保留许可证声明）。  
详细条款见仓库中的 `LICENSE` 文件（如有）。

