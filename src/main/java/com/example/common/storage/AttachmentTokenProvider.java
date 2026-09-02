package com.example.common.storage;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 로컬 다운로드 URL의 서명 토큰을 발급·검증한다(FR-F05). JwtTokenProvider와 동일한
 * JWT_SECRET으로 서명하되(PRD 9.1에 별도 다운로드 서명키가 없음), 토큰 자체가
 * 첨부 ID·만료 시각을 담아 DB 조회 없이 검증 가능하다는 점에서 로그인 토큰과는
 * 별도 클래스로 분리한다.
 */
@Component
public class AttachmentTokenProvider {

    private static final String ATTACHMENT_ID_CLAIM = "attachmentId";

    private final SecretKey key;

    public AttachmentTokenProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(Long attachmentId, long ttlSeconds) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlSeconds * 1000);
        return Jwts.builder()
                .claim(ATTACHMENT_ID_CLAIM, attachmentId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** 파싱·검증에 실패하면(위조·만료 포함) {@link JwtException}을 던진다. */
    public Long parseAttachmentId(String token) {
        Object claim =
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .get(ATTACHMENT_ID_CLAIM);
        return ((Number) claim).longValue();
    }
}
