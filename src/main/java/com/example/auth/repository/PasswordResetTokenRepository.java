package com.example.auth.repository;

import com.example.auth.entity.PasswordResetToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndUsedAtIsNull(String token);

    List<PasswordResetToken> findAllByUserIdAndUsedAtIsNull(Long userId);

    boolean existsByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
}
