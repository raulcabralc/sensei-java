package com.sensei.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Clientes HTTP da integração com a OpenAI, com timeouts de conexão e de leitura.
 * Usa a mesma implementação HTTP de antes (SimpleClientHttpRequestFactory, padrão do
 * `new RestTemplate()`), mudando apenas os limites de tempo. Sem retry automático.
 */
@Configuration
@EnableConfigurationProperties(OpenAiTimeoutProperties.class)
public class OpenAiClientConfig {
    @Bean
    public RestTemplate openAiChatRestTemplate(OpenAiTimeoutProperties timeouts) {
        return restTemplate(timeouts.connect(), timeouts.chatRead());
    }

    @Bean
    public RestTemplate openAiTranscriptionRestTemplate(OpenAiTimeoutProperties timeouts) {
        return restTemplate(timeouts.connect(), timeouts.transcriptionRead());
    }

    static RestTemplate restTemplate(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return new RestTemplate(requestFactory);
    }
}
