package com.sensei.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Timeouts das chamadas HTTP à OpenAI (openai.timeout.*).
 * A transcrição de áudio (até 25 MB) pode levar minutos; o resumo de texto é mais rápido.
 * Não há retry automático: as operações são cobradas e não devem ser repetidas sem controle.
 */
@ConfigurationProperties(prefix = "openai.timeout")
public record OpenAiTimeoutProperties(
        @DefaultValue("10s") Duration connect,
        @DefaultValue("90s") Duration chatRead,
        @DefaultValue("300s") Duration transcriptionRead
) {
    public OpenAiTimeoutProperties {
        requirePositive("openai.timeout.connect", connect);
        requirePositive("openai.timeout.chat-read", chatRead);
        requirePositive("openai.timeout.transcription-read", transcriptionRead);
    }

    private static void requirePositive(String property, Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("Invalid OpenAI configuration: " + property + " must be greater than zero.");
        }
    }
}
