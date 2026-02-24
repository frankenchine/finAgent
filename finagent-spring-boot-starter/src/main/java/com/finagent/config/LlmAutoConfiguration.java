package com.finagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finagent.core.ModelInvoker;
import com.finagent.llm.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for LLM API integration.
 * 1、restTemplate 全局配置
 * 2、llmApiClient 根据provider不同，创建不同的apiClient实现类
 * 3、modelInvoker 将apiClient封装至modelInvoker，方便后续调用
 */
@AutoConfiguration
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "llmRestTemplate")
    public RestTemplate llmRestTemplate(LlmProperties properties) {
        RestTemplate restTemplate = new RestTemplate();

        // Configure timeout
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = (properties.getTimeoutSeconds() != null ? properties.getTimeoutSeconds() : 60) * 1000;
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);

        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(LlmApiClient.class)
    public LlmApiClient llmApiClient(LlmProperties properties,
                                     @Qualifier("llmRestTemplate") RestTemplate restTemplate,
                                     ObjectMapper objectMapper) {
        LlmProvider provider = LlmProvider.fromString(properties.getProvider());

        switch (provider) {
            case OPENAI:
                return new OpenAiApiClient(properties, restTemplate, objectMapper);
            case DEEPSEEK:
                return new DeepSeekApiClient(properties, restTemplate, objectMapper);
            default:
                throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        }
    }

    @Bean
    @ConditionalOnMissingBean(ModelInvoker.class)
    public ModelInvoker httpModelInvoker(LlmApiClient llmApiClient) {
        return new HttpModelInvoker(llmApiClient);
    }
}

