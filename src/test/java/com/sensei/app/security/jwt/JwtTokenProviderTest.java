package com.sensei.app.security.jwt;

import com.sensei.app.enums.UserRole;
import com.sensei.app.security.AuthenticatedUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {
    // Valor exclusivo de teste (>= 32 bytes). Não é usado em nenhum ambiente.
    private static final String TEST_SECRET = "test-only-jwt-secret-for-automated-tests-0123456789";

    private final JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, 3_600_000);

    private String tokenFor(String email) {
        AuthenticatedUser principal = new AuthenticatedUser("id-1", email, null, UserRole.STUDENT);
        return provider.generateToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    void generatedTokenIsValidAndKeepsEmailAsSubject() {
        String token = tokenFor("aluno@test.com");

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsernameFromToken(token)).isEqualTo("aluno@test.com");
    }

    @Test
    void tokenFormatIsUnchanged_subjectIssuedAtAndExpirationOnly() {
        String token = tokenFor("aluno@test.com");

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "iat", "exp");
    }

    @Test
    void expiredTokenIsRejected() {
        Date past = new Date(System.currentTimeMillis() - 60_000);
        String expired = Jwts.builder()
                .subject("aluno@test.com")
                .issuedAt(new Date(past.getTime() - 60_000))
                .expiration(past)
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(provider.validateToken(expired)).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = tokenFor("aluno@test.com");
        String[] parts = token.split("\\.");
        // Altera o payload (subject) mantendo a assinatura original.
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"professor@test.com\"}".getBytes(StandardCharsets.UTF_8));

        assertThat(provider.validateToken(parts[0] + "." + forgedPayload + "." + parts[2])).isFalse();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtTokenProvider other = new JwtTokenProvider("another-test-only-secret-with-enough-bytes-987654321", 3_600_000);
        AuthenticatedUser principal = new AuthenticatedUser("id-1", "aluno@test.com", null, UserRole.STUDENT);
        String foreign = other.generateToken(new UsernamePasswordAuthenticationToken(principal, null));

        assertThat(provider.validateToken(foreign)).isFalse();
    }

    @Test
    void unsignedMalformedAndEmptyTokensAreRejected() {
        String unsigned = Jwts.builder().subject("aluno@test.com").compact();

        assertThat(provider.validateToken(unsigned)).isFalse();
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    void shortSecretFailsAtStartupWithoutRevealingIt() {
        String weakSecret = "short-secret-123";

        assertThatThrownBy(() -> new JwtTokenProvider(weakSecret, 3_600_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes")
                .hasMessageNotContaining(weakSecret)
                .hasNoCause();
    }

    @Test
    void blankSecretFailsAtStartup() {
        assertThatThrownBy(() -> new JwtTokenProvider("   ", 3_600_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void nonPositiveExpirationFailsAtStartup() {
        assertThatThrownBy(() -> new JwtTokenProvider(TEST_SECRET, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_EXPIRATION_MS");
    }
}
