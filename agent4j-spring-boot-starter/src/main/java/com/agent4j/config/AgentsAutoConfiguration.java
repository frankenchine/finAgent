/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.config;

import com.agent4j.api.AgentRunner;
import com.agent4j.core.DefaultAgentRunner;
import com.agent4j.core.ModelInvoker;
import com.agent4j.core.ReflexionRunner;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(LlmAutoConfiguration.class)
@EnableConfigurationProperties(AgentsProperties.class)
public class AgentsAutoConfiguration {

    /**
     * 如果容器中存在 ModelInvoker Bean，则创建 DefaultAgentRunner Bean
     * 默认的普通AgentRunner实现，用于支持普通Agent执行
     */
    @Bean
    @ConditionalOnBean(ModelInvoker.class)
    @ConditionalOnMissingBean(AgentRunner.class)
    public AgentRunner agentRunner(ModelInvoker modelInvoker) {
        return new DefaultAgentRunner(modelInvoker);
    }

    /**
     * 如果容器中存在 AgentRunner 和 ModelInvoker Bean，则创建 ReflexionRunner Bean
     * 用于支持Reflexion多轮试错+反思（执行 → 评估 → 反思 → 重试）
     */
    @Bean
    @ConditionalOnBean({AgentRunner.class, ModelInvoker.class})
    @ConditionalOnMissingBean(ReflexionRunner.class)
    public ReflexionRunner reflexionRunner(AgentRunner agentRunner, ModelInvoker modelInvoker) {
        return new ReflexionRunner(agentRunner, modelInvoker);
    }
}

