package com.sensei.app.exceptions;

/**
 * Dados de entrada inválidos para a regra de negócio (HTTP 400).
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
