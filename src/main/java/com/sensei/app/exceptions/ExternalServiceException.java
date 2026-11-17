package com.sensei.app.exceptions;

/**
 * Falha ao comunicar com um serviço externo (ex.: OpenAI). A mensagem é genérica;
 * a causa fica disponível apenas para logs do servidor.
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
