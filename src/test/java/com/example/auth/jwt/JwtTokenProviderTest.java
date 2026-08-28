package com.example.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.user.entity.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET =
            "test-secret-key-for-jwt-token-provider-unit-test-must-be-long-enough";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET);

    @Test
    void 토큰을_발급하면_검증에_성공한다() {
        String token = provider.generateToken(1L, UserRole.USER);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    void 위조된_토큰은_검증에_실패한다() {
        String tampered = provider.generateToken(1L, UserRole.USER) + "tampered";

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void 만료된_토큰은_검증에_실패한다() throws InterruptedException {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Date past = new Date(System.currentTimeMillis() - 10000);
        String expired =
                Jwts.builder()
                        .subject("1")
                        .issuedAt(new Date(past.getTime() - 1000))
                        .expiration(past)
                        .signWith(key, Jwts.SIG.HS256)
                        .compact();

        assertThat(provider.validateToken(expired)).isFalse();
    }
}
