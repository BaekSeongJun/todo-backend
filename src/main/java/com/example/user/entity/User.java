package com.example.user.entity;

import com.example.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * users에는 @SQLRestriction을 걸지 않는다.
 * Todo.user 같은 ToOne 연관의 대상이 Soft Delete되면 EntityNotFoundException이
 * 발생하기 때문이며, 대신 UserRepository 메서드에 deleted_at IS NULL 조건을 명시한다.
 */
@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled;

    @Builder
    private User(
            String email,
            String password,
            String name,
            AuthProvider provider,
            String providerId,
            UserRole role,
            boolean enabled) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role != null ? role : UserRole.USER;
        this.enabled = enabled;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
