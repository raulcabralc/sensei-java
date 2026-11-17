package com.sensei.app.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final int jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwtSecret}") String jwtSecret,
            @Value("${app.jwtExpirationMs}") int jwtExpirationMs
    ) {
        this.signingKey = buildKey(jwtSecret);

        if (jwtExpirationMs <= 0) {
            throw new IllegalStateException("Invalid JWT configuration: app.jwtExpirationMs (JWT_EXPIRATION_MS) must be greater than zero.");
        }

        this.jwtExpirationMs = jwtExpirationMs;
    }

    /**
     * Valida a chave na inicialização. O algoritmo HMAC-SHA continua sendo inferido
     * pelo tamanho da chave, como antes. As mensagens nunca incluem o segredo.
     */
    private static SecretKey buildKey(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Invalid JWT configuration: app.jwtSecret (JWT_SECRET) is not set.");
        }

        try {
            return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        } catch (WeakKeyException e) {
            throw new IllegalStateException("Invalid JWT configuration: app.jwtSecret (JWT_SECRET) must be at least 32 bytes (256 bits) for HMAC-SHA signing.");
        }
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Date issuedAt = new Date();
        Date expirationDate = new Date(issuedAt.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiration(expirationDate)
                .signWith(signingKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Rejected JWT: token expired.");
        } catch (JwtException | IllegalArgumentException e) {
            // Assinatura inválida, token malformado, não assinado ou vazio. O token nunca é logado.
            log.debug("Rejected JWT: {}", e.getClass().getSimpleName());
        }

        return false;
    }
}
