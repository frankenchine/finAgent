package com.finagent.config;

import com.finagent.api.AgentRunner;
import com.finagent.core.DefaultAgentRunner;
import com.finagent.core.ModelInvoker;
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
}
