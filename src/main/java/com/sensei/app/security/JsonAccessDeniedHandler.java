package com.sensei.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 403 em JSON quando um usuário autenticado tenta uma operação proibida
 * (regras de URL, @PreAuthorize ou AccessDeniedException lançada pelos services).
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        SecurityErrorResponses.write(response, HttpStatus.FORBIDDEN, SecurityErrorResponses.ACCESS_DENIED_MESSAGE, request);
    }
}
