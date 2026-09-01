package com.example.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * password_reset_tokens는 Soft Delete 대상이 아니므로(PRD 7장 예외)
 * BaseTimeEntity/BaseEntity를 상속하지 않고 created_at만 직접 선언한다.
 */
@Getter
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = @Index(name = "idx_password_reset_tokens_token", columnList = "token", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PasswordResetToken(Long userId, String token, LocalDateTime expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public static PasswordResetToken create(Long userId, String hashedToken, long ttlMinutes) {
        return new PasswordResetToken(userId, hashedToken, LocalDateTime.now().plusMinutes(ttlMinutes));
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }
}
