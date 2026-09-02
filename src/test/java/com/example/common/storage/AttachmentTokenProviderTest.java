package com.example.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class AttachmentTokenProviderTest {

    private static final String SECRET =
            "test-secret-key-for-attachment-token-provider-unit-test-must-be-long-enough";

    private final AttachmentTokenProvider provider = new AttachmentTokenProvider(SECRET);

    @Test
    void 토큰을_발급하면_첨부ID를_정상적으로_파싱한다() {
        String token = provider.generateToken(42L, 300);

        assertThat(provider.parseAttachmentId(token)).isEqualTo(42L);
    }

    @Test
    void 위조된_토큰은_파싱에_실패한다() {
        String tampered = provider.generateToken(42L, 300) + "tampered";

        assertThatThrownBy(() -> provider.parseAttachmentId(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void 만료된_토큰은_파싱에_실패한다() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Date past = new Date(System.currentTimeMillis() - 10000);
        String expired =
                Jwts.builder()
                        .claim("attachmentId", 42L)
                        .issuedAt(new Date(past.getTime() - 1000))
                        .expiration(past)
                        .signWith(key, Jwts.SIG.HS256)
                        .compact();

        assertThatThrownBy(() -> provider.parseAttachmentId(expired))
                .isInstanceOf(JwtException.class);
    }
}
