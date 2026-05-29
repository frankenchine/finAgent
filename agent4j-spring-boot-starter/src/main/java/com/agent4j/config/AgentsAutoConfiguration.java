/*
 * Copyright (c) 2025 agent4j
 * Licensed under the PolyForm Noncommercial License 1.0.0
 * https://polyformproject.org/licenses/noncommercial/1.0.0/
 */
package com.agent4j.config;

import com.agent4j.api.AgentRunner;
import com.agent4j.core.DefaultAgentRunner;
import com.agent4j.core.ModelInvoker;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(LlmAutoConfiguration.class)
@EnableConfigurationProperties(AgentsProperties.class)
public class AgentsAutoConfiguration {

    @Bean
    @ConditionalOnBean(ModelInvoker.class)
    @ConditionalOnMissingBean(AgentRunner.class)
    public AgentRunner agentRunner(ModelInvoker modelInvoker) {
        return new DefaultAgentRunner(modelInvoker);
    }
}

