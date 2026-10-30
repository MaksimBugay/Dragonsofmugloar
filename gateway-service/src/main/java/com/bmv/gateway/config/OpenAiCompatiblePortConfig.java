package com.bmv.gateway.config;

import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.gpt_5_4.AiAgentToolAzureGpt54OutboundPort;
import com.bmv.flow.foundry.infrastructure.adapter.out.ai.azure.gpt_5_4.conf.AzureAiGpt54Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiCompatiblePortConfig {

    @Value("${bmv.ai.port.open-ai.url:}")
    private String openAiCompatiblePortUrl;

    @Value("${bmv.ai.port.open-ai.access-key:}")
    private String openAiCompatiblePortAccessKey;

    @Bean
    public AiAgentToolAzureGpt54OutboundPort aiAgentToolAzureGpt54OutboundPort() {
        return new AiAgentToolAzureGpt54OutboundPort(
                new AzureAiGpt54Config() {
                    @Override
                    public String accessToken() {
                        return openAiCompatiblePortAccessKey;
                    }

                    @Override
                    public String apiUrl() {
                        return openAiCompatiblePortUrl;
                    }
                }
        );
    }
}
