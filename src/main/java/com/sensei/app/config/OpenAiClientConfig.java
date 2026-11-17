package com.sensei.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP usado pela integração com a OpenAI.
 * Mesmo comportamento do `new RestTemplate()` usado antes, agora como bean
 * para permitir testes sem chamadas reais à API.
 */
@Configuration
public class OpenAiClientConfig {
    @Bean
    public RestTemplate openAiRestTemplate() {
        return new RestTemplate();
    }
}
