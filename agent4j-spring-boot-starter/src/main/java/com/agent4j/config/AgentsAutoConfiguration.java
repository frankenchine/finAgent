/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.config;

import com.agent4j.api.AgentRunner;
import com.agent4j.core.DefaultAgentRunner;
import com.agent4j.core.ModelInvoker;
import com.agent4j.sse.AgentSseService;
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

    @Bean
    @ConditionalOnBean(ModelInvoker.class)
    @ConditionalOnMissingBean(AgentRunner.class)
    public AgentRunner agentRunner(ModelInvoker modelInvoker) {
        return new DefaultAgentRunner(modelInvoker);
    }

    @Bean
    @ConditionalOnBean(AgentRunner.class)
    @ConditionalOnMissingBean(AgentSseService.class)
    public AgentSseService agentSseService(AgentRunner agentRunner) {
        return new AgentSseService(agentRunner);
    }
}

