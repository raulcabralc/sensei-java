package com.sensei.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensei.app.dtos.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Mensagens e escrita das respostas de erro de segurança (401 e 403) geradas fora dos
 * controllers (filtros). Usa o mesmo contrato {@link ApiErrorResponse} do GlobalExceptionHandler.
 */
public final class SecurityErrorResponses {
    public static final String UNAUTHENTICATED_MESSAGE = "Full authentication is required to access this resource.";
    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";
    public static final String ACCESS_DENIED_MESSAGE = "You do not have permission to perform this operation.";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SecurityErrorResponses() {
    }

    public static void write(HttpServletResponse response,
                             HttpStatus status,
                             String message,
                             HttpServletRequest request) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(response.getWriter(), ApiErrorResponse.of(status, message, request.getRequestURI()));
    }
}
