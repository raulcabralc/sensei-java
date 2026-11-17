package com.sensei.app.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Clientes HTTP da OpenAI para testes, interceptados por MockRestServiceServer.
 *
 * Classe de nível superior de propósito: declarada como classe estática aninhada da classe
 * de teste, ela é detectada pelo Spring Boot apenas para a classe externa, e as classes @Nested
 * passam a usar outro ApplicationContext. Os mocks injetados (contexto da classe externa)
 * deixavam de ser resetados após cada teste, pois o reset do @MockitoBean atua no contexto
 * da classe @Nested em execução.
 */
@TestConfiguration
public class OpenAiTestClients {
    @Bean
    RestTemplate openAiChatRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    RestTemplate openAiTranscriptionRestTemplate() {
        return new RestTemplate();
    }
}
