# LLM 动态路由（LlmRouter）

本页说明如何在下游 Spring Boot 项目中配置与使用 agent4j 的 LLM 动态路由能力，以及可扩展点（规则来源、策略、自定义 invoker、多模型编排的预留接口）。

---

## 目标与能力范围

- **按上下文选模型**：根据 `agentName` / `taskType`（以及未来可扩展的 userId、标签等）选择 `primary-model`
- **失败自动回退**：primary 调用失败后，按 `fallback-models` 顺序重试
- **Invoker 注册缓存**：`ModelInvokerRegistry` 内存持有 `Map<ModelIdentifier, ModelInvoker>`（单例 Bean）
- **兼容路径**：不使用路由时仍可直接走 `AgentRunner -> ModelInvoker` 或手动调用 `LlmApiClient`

---

## 配置

在 `application.yml` 中使用 `agent4j.llm`：

```yaml
agent4j:
  llm:
    provider: openai
    api-key: your-api-key-here
    model: gpt-4o-mini

    # 当没有任何 routing-rules 命中时使用（可选）
    default-model: openai:gpt-4o-mini

    # 路由规则（可选，按顺序匹配，先命中先使用）
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

- `primary-model` / `fallback-models` / `default-model` 的格式为 `{provider}:{modelName}`，例如 `openai:gpt-4o-mini`
- 默认实现会把当前单一 `ModelInvoker` 注册到 `ModelInvokerRegistry` 中（key 为 `provider:model` 以及 `default-model`）。如果你需要同时支持多 provider / 多 endpoint，可以自行注册更多 `ModelInvoker`

---

## 使用方式

### 方式 A：保持 AgentRunner 兼容路径（不显式路由）

只使用 `AgentRunner` 的场景无需改动，仍会走默认 `ModelInvoker`：

```java
RunResult result = agentRunner.run(agent, RunRequest.builder().input(q).maxTurns(10).build());
```

### 方式 B：显式使用 LlmRouter（推荐用于“手动调用一次 LLM”或多模型编排）

```java
import com.agent4j.llm.LlmRouter;
import com.agent4j.llm.RoutingContext;
import com.agent4j.model.Message;
import com.agent4j.model.ModelInvocationRequest;

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

### 方式 C：router 优先、client 兜底（兼容历史写法）

当你的示例 Agent/Service 过去直接依赖 `LlmApiClient` 时，可以逐步迁移为 router 优先：

```java
ModelInvocationResponse resp = (llmRouter != null)
        ? llmRouter.routeAndInvoke(ctx, req)
        : llmApiClient.invoke(req);
```

---

## 扩展点

### 1) 自定义规则来源（DB / 配置中心）

实现并注册 `LlmRoutingRuleRepository` Bean 即可替换默认的基于配置的仓库：

- **默认实现**：`InMemoryLlmRoutingRuleRepository`（由 `RoutingRuleRepositories.fromProperties(...)` 构建）
- **可替换实现**：从数据库、Nacos、Apollo 等加载后转换为 `LlmRoutingRule` 列表

### 2) 自定义路由策略（更复杂的选模逻辑）

实现并注册 `LlmRoutingStrategy` Bean：

- **默认实现**：`SimpleLlmRoutingStrategy`（按 `agent` 正则 + 可选 `taskType` 匹配，未命中使用 `default-model`）
- **可扩展**：可根据 userId、成本预算、标签、灰度策略等做决策

### 3) 注册更多 ModelInvoker（支持多 provider / 多 endpoint）

拿到 `ModelInvokerRegistry` 后注册更多 `ModelIdentifier -> ModelInvoker`：

- 适用于同一应用内同时调用多个 provider、或同 provider 不同 baseUrl/不同鉴权的场景

### 4) 多模型链式调用（预留）

当前默认只实现 **单模型 + fallback 重试**。后续可以扩展 `LlmRoutingDecision` 支持 pre/post processors：

- **preProcessors**：先用便宜模型做摘要/意图识别/检索 query 重写
- **primaryModel**：用强模型完成主任务
- **postProcessors**：用审校模型做校对/安全检查/格式化重写

