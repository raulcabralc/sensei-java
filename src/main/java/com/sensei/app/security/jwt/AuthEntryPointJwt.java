package com.sensei.app.security.jwt;

import com.sensei.app.security.SecurityErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 401 em JSON para requisições sem token, com token inválido ou expirado.
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityErrorResponses.write(response, HttpStatus.UNAUTHORIZED, SecurityErrorResponses.UNAUTHENTICATED_MESSAGE, request);
    }
}
