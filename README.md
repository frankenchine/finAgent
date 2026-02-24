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
