package com.sensei.app.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Formato único das respostas de erro da API (segurança e negócio):
 * {"status": 404, "error": "Not Found", "message": "...", "path": "/..."}
 * Erros de validação incluem também "fieldErrors": {"campo": "mensagem"}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ApiErrorResponse withFieldErrors(HttpStatus status, String message, String path,
                                                   Map<String, String> fieldErrors) {
        return new ApiErrorResponse(status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
