# agent4j

agent4j 是一个基于 Spring Boot 的**轻量级多智能体（multi‑agent）框架**，设计灵感来自 [openai-agents-python](https://github.com/openai/openai-agents-python)。  
它通过一组接口抽象，把「智能体、工具调用、Agent 之间的 handoff、会话记忆、Guardrail」等概念封装成可插拔组件，并内置了对 **OpenAI 格式 HTTP 接口（OpenAI、DeepSeek 等）** 的访问实现，无需依赖 Spring AI。

当前仓库已经收敛为两个对外可复用的模块：

- `agent4j-core`：核心能力与接口定义
- `agent4j-spring-boot-starter`：Spring Boot 自动装配与配置封装（Starter）

---

## 模块结构

### 1. agent4j-core

核心模块，包含所有与运行时逻辑相关的代码：

- `com.agent4j.api`
  - `Agent`：无状态 Agent 配置（名称、说明、Tools、Handoffs、Guardrails、输出类型）。
  - `AgentRunner`：执行 Agent 的主入口，负责循环 LLM 调用直到终止。
  - `RunRequest` / `RunResult`：一次 Run 的请求和结果封装。
  - `Tool`：工具接口（名称、描述、参数 Schema、`invoke(ToolContext)`）。
  - `Handoff`：Agent 间的交接（由 LLM 通过“工具调用”的形式触发）。
  - `Session`：会话记忆接口（如 `InMemorySession`）。
  - `ReflexionMemory`：反思记忆接口，用于 Reflexion 多轮试错。
  - `TrialEvaluator`：单次 Run 的成功/失败评估器。
  - `ReflexionRunConfig` / `ReflexionRunRequest`：Reflexion 运行配置与请求。
  - `InputGuardrail` / `OutputGuardrail`：输入/输出 Guardrail。
- `com.agent4j.core`
  - `ReflexionRunner`：Reflexion 多轮试错执行器（执行 → 评估 → 反思 → 重试）。
  - `LlmTrialEvaluator`：基于 LLM 的 TrialEvaluator 实现。
  - `DefaultAgentRunner`：默认的 AgentRunner 实现，负责：
    - 调用 `ModelInvoker`（LLM）
    - 判断是最终输出、工具调用还是 handoff
    - 处理 Guardrail 和 Session 记忆
  - `AgentDefinition`：用于构建 `Agent` 的可变配置类。
  - `ModelInvoker`：一次 LLM 调用的抽象接口。
- `com.agent4j.model`
  - `Message`：统一的对话消息表示（SYSTEM/USER/ASSISTANT/TOOL）。
  - `ModelInvocationRequest` / `ModelInvocationResponse`：LLM 调用请求与返回结构（含 tool_calls）。
- `com.agent4j.tools`
  - `FunctionToolRegistry`：快速把 Java 函数包装成 `Tool`。
  - `ToolExecutor`、`ToolInvocation`、`ToolSchema`：工具执行与 Schema 适配。
- `com.agent4j.handoffs`
  - `HandoffResolver`、`HandoffToolAdapter`：把 Handoff 暴露给 LLM 并在收到调用时完成 Agent 切换。
- `com.agent4j.memory`
  - `InMemorySession`：基于内存的 Session 实现。
- `com.agent4j.llm`
  - `LlmApiClient`：LLM HTTP 客户端接口。
  - `HttpModelInvoker`：基于 `LlmApiClient` 的 `ModelInvoker` 实现。
  - `LlmProvider`：支持的 Provider 枚举（`OPENAI`、`DEEPSEEK`）。
  - `OpenAiApiClient` / `DeepSeekApiClient`：面向 OpenAI 格式聊天接口的 HTTP 客户端。
  - `dto.OpenAiRequest` / `dto.OpenAiResponse`：与 OpenAI Chat Completions 协议兼容的 DTO。
- `com.agent4j.config`
  - `LlmProperties`：`agent4j.llm.*` 配置属性（Provider、Base URL、API Key、模型、超时、温度等）。

### 2. agent4j-spring-boot-starter

Starter 模块，负责 Spring Boot 自动装配与配置绑定：

- `com.agent4j.config.AgentsAutoConfiguration`
  - 自动注入 `AgentRunner`：
    - 若容器中存在 `ModelInvoker` Bean 且没有用户自定义 `AgentRunner`，则创建 `DefaultAgentRunner`。
- `com.agent4j.config.LlmAutoConfiguration`
  - 自动配置 LLM 相关 Bean：
    - `RestTemplate`（超时等）
    - `LlmApiClient`（根据 `agent4j.llm.provider` 选择 OpenAI/DeepSeek）
    - `HttpModelInvoker`（默认 `ModelInvoker` 实现）
- `com.agent4j.config.AgentsProperties`
  - `agent4j.*` 级别的通用配置（如默认最大轮数、Session 存储方式等）。
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  - 注册上述自动配置类，使其在引入 starter 后自动生效。

---

## 在下游 Spring Boot 项目中使用

### 1. 添加依赖

在你的 Spring Boot 项目的 `pom.xml` 中加入：

```xml
<dependency>
  <groupId>com.agent4j</groupId>
  <artifactId>agent4j-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

（假设你已经在本机或私服中安装/发布了该版本）

### 2. 配置 LLM

在下游项目的 `application.yml` 中配置 `agent4j.llm` 属性，例如：

```yaml
agent4j:
  llm:
    provider: deepseek               # openai 或 deepseek
    base-url: https://api.deepseek.com/v1
    api-key: your-api-key-here
    model: deepseek-chat
    temperature: 0.7
    max-tokens: 2000
    timeout-seconds: 60
```

当 `api-key` 配置完成后，Starter 会自动：

- 创建具备超时配置的 `RestTemplate`
- 按 `provider` 选择 `OpenAiApiClient` 或 `DeepSeekApiClient`
- 注册 `HttpModelInvoker` 作为默认 `ModelInvoker` Bean
- 再由 `AgentsAutoConfiguration` 创建一个默认的 `AgentRunner` Bean

---

## LLM 动态路由（LlmRouter）

在某些场景下，你可能希望**同一套 Agent 逻辑**能根据 `agentName` / `taskType` 等上下文选择不同模型（例如：短回答走便宜模型，代码生成走更强模型），并在失败时按 fallback 顺序重试。

Starter 已默认装配以下 Bean（均可被用户自定义覆盖）：

- `LlmRoutingRuleRepository`：路由规则仓库（默认从配置读取，内存保存）
- `SimpleLlmRoutingStrategy`：按规则匹配（agent regex + 可选 taskType），否则走 default
- `ModelInvokerRegistry`：`Map<ModelIdentifier, ModelInvoker>` 的内存注册表
- `LlmRouter`：门面，负责 route + invoke + fallback

更完整说明见 `docs/llm-routing.md`。

### 1) 配置示例

```yaml
agent4j:
  llm:
    provider: openai
    api-key: your-api-key-here
    model: gpt-4o-mini

    # 当没有任何 routing-rules 命中时使用（可选）
    default-model: openai:gpt-4o-mini

    # 路由规则（可选）
    routing-rules:
      - agent: "supportAgent"          # Java 正则；为空表示匹配任意 agent
        task-type: "short_answer"      # 可选：精确匹配（忽略大小写）
        primary-model: "openai:gpt-4o-mini"
        fallback-models:
          - "openai:gpt-4o"
      - agent: "codeAgent"
        task-type: "code_generation"
        primary-model: "openai:gpt-4o"
```

说明：

- `primary-model` / `fallback-models` 的格式为 `{provider}:{modelName}`，例如 `openai:gpt-4o-mini`
- 默认实现会把当前单一 `ModelInvoker` 注册到 `ModelInvokerRegistry` 中（key 为 `provider:model` 以及 `default-model`）。如果你需要同时支持多 provider / 多 baseUrl，可以自行注册更多 `ModelInvoker`。

### 2) 使用示例（保留兼容路径）

如果你只使用 `AgentRunner`（兼容路径），无需改动，仍会走默认的 `ModelInvoker`：

```java
// 兼容路径：不使用路由
RunResult result = agentRunner.run(agent, RunRequest.builder().input(q).maxTurns(10).build());
```

如果你希望显式路由（例如在你的业务代码中“手动调用一次 LLM”，或做多模型编排），可以直接注入 `LlmRouter`：

```java
import com.agent4j.llm.LlmRouter;
import com.agent4j.llm.RoutingContext;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;

// ...
RoutingContext ctx = RoutingContext.builder()
        .agentName("supportAgent")
        .taskType("short_answer")
        .build();

ModelInvocationRequest req = new ModelInvocationRequest(
        "You are a helpful assistant. Reply concisely.",
        java.util.List.of(Message.user("Explain what agent4j is.")),
        java.util.List.of()
);

String text = llmRouter.routeAndInvoke(ctx, req).getAssistantText();
```

如果你的“示例 Agent / Service”历史上直接依赖 `LlmApiClient`，推荐改成 **router 优先、client 兜底** 的兼容写法：

```java
import com.agent4j.llm.LlmApiClient;
import com.agent4j.llm.LlmRouter;
import com.agent4j.llm.RoutingContext;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;

public class SupportAgentService {
    private final LlmRouter llmRouter;       // 可选（有动态路由能力时注入）
    private final LlmApiClient llmApiClient; // 兼容路径（没有路由时仍可用）

    public SupportAgentService(LlmRouter llmRouter, LlmApiClient llmApiClient) {
        this.llmRouter = llmRouter;
        this.llmApiClient = llmApiClient;
    }

    public String answer(String question) {
        RoutingContext ctx = RoutingContext.builder()
                .agentName("supportAgent")
                .taskType("short_answer")
                .build();

        ModelInvocationRequest req = new ModelInvocationRequest(
                "You are a helpful assistant. Reply concisely.",
                java.util.List.of(Message.user(question)),
                java.util.List.of()
        );

        ModelInvocationResponse resp = (llmRouter != null)
                ? llmRouter.routeAndInvoke(ctx, req)
                : llmApiClient.invoke(req);

        return resp.getAssistantText();
    }
}
```

### 3) 扩展点

- **自定义规则来源**：实现 `LlmRoutingRuleRepository`（例如从 DB / 配置中心拉取规则）并注册为 Bean。
- **自定义路由策略**：实现 `LlmRoutingStrategy`（例如按 userId/成本预算/标签等）并注册为 Bean。
- **自定义 invoker 注册**：拿到 `ModelInvokerRegistry` 后注册更多 `ModelIdentifier -> ModelInvoker`，以支持多 provider/多 endpoint 的组合。

当前默认路由策略是 `SimpleLlmRoutingStrategy`（按 `agent` 正则 + 可选 `taskType` 匹配，未命中则使用 `default-model`）。

未来扩展点（接口已预留但默认实现暂不启用）：

- **多模型链式调用**：可在后续迭代中扩展 `LlmRoutingDecision` 支持 pre/post processors（先便宜模型预处理，再贵模型完成任务）。

### 3. 注入 AgentRunner 并构建 Agent

示例 Controller：

```java
import com.agent4j.api.AgentRunner;
import com.agent4j.api.RunRequest;
import com.agent4j.api.RunResult;
import com.agent4j.api.Agent;
import com.agent4j.core.AgentDefinition;
import com.agent4j.tools.FunctionToolRegistry;
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
```

---

## 会话记忆（Session）示例

使用 `InMemorySession` 在多次调用间共享上下文：

```java
import com.agent4j.api.Session;
import com.agent4j.memory.InMemorySession;

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
```

你也可以自定义 `Session` 实现（如 Redis、数据库），并在 `RunRequest.builder().session(...)` 中传入。

---

## Reflexion：多轮试错与反思记忆

Reflexion 模式支持多轮试错：每轮失败后由 LLM 生成反思，存入 memory，下一轮将反思注入 context 以改进。适用于需要从失败中学习的任务（如解题、优化）。

```java
import com.agent4j.api.*;
import com.agent4j.core.ReflexionRunner;
import com.agent4j.core.LlmTrialEvaluator;
import com.agent4j.memory.InMemoryReflexionMemory;

// 创建 Reflexion 组件
ReflexionMemory memory = new InMemoryReflexionMemory("task_001");
TrialEvaluator evaluator = new LlmTrialEvaluator(modelInvoker,
    "Evaluate whether the agent's solution is correct. Reply with SUCCESS or FAILURE, then brief feedback.");

ReflexionRunConfig config = ReflexionRunConfig.builder()
    .maxTrials(3)
    .reflectionLimit(5)
    .build();

ReflexionRunner reflexionRunner = new ReflexionRunner(agentRunner, modelInvoker);

// 执行 Reflexion 多轮试错
RunResult result = reflexionRunner.run(agent,
    ReflexionRunRequest.builder()
        .input("Solve: ...")
        .reflexionMemory(memory)
        .trialEvaluator(evaluator)
        .config(config)
        .build());
```

- `ReflexionMemory`：存储反思文本，可自定义实现（如 Redis、SQLite）。
- `TrialEvaluator`：评估单次 Run 的成功/失败；`LlmTrialEvaluator` 使用 LLM 判断，也可实现自定义逻辑。
- `ReflexionRunConfig`：可配置 `maxTrials`、`reflectionLimit`、`reflectPromptTemplate`。

---

## 工具（Tool）与 Handoff

### Tool

通过 `FunctionToolRegistry` 快速把 Java 函数暴露给 LLM：
```java
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
```

LLM 在回复中返回 `tool_calls` 时，`DefaultAgentRunner` 会自动：

1. 解析 `tool_calls` 列表
2. 构造 `ToolInvocation`
3. 通过 `ToolExecutor` 调用具体的 `Tool`
4. 把 Tool 执行结果作为 `TOOL` 消息追加到对话，再进行下一轮 LLM 调用

### Handoff

Handoff 通过「特殊 Tool」的方式让 LLM 选择切换到另一个 Agent：

```java
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
```

当 LLM 调用 `transfer_to_spanish_agent` 时，`DefaultAgentRunner` 会使用 `HandoffResolver` 切换当前 Agent，并继续对话。

---

## Guardrail：输入与输出的约束

你可以为 Agent 配置：

- `InputGuardrail`：在第一次 LLM 调用前检查/修改输入，或直接拒绝。
- `OutputGuardrail`：在最终输出返回给用户前做校验/转换。

示例（输出转大写）：

```java
OutputGuardrail toUpper = (output, context) ->
        OutputGuardrail.OutputGuardrailResult.pass(
                output != null ? output.toString().toUpperCase() : ""
        );

Agent agent = new AgentDefinition()
        .setName("test")
        .setInstructions("Help.")
        .addOutputGuardrail(toUpper)
        .build();
```

---

## 自定义 ModelInvoker

如果你不想使用内置的 HTTP 客户端，可以自己实现 `ModelInvoker` 并注册为 Spring Bean：

```java
import com.agent4j.core.ModelInvoker;
import com.agent4j.model.ModelInvocationRequest;
import com.agent4j.model.ModelInvocationResponse;
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
```

当容器中存在你自定义的 `ModelInvoker` Bean 时，`LlmAutoConfiguration` 中默认的 HTTP 实现会被跳过，`AgentsAutoConfiguration` 会使用你的 Bean 创建 `DefaultAgentRunner`。

---

## 构建与安装

在本工程根目录执行：

```bash
mvn -q -DskipTests clean install
```

会在本地 Maven 仓库生成：

- `com.agent4j:agent4j-core`
- `com.agent4j:agent4j-spring-boot-starter`

下游项目只需要依赖 `agent4j-spring-boot-starter` 即可。

---

## 许可证

本项目采用 **PolyForm Noncommercial License 1.0.0**。

- **允许**：个人学习、研究、业余项目；教育机构、慈善机构、公共研究机构使用
- **禁止**：未经授权的商业用途（公司产品、SaaS、内部营利性工具等）

商业使用请联系 [agent4j@sina.com] 获取商业许可。详细条款见仓库中的 `LICENSE` 文件。

