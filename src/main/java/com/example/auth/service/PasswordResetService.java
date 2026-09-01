package com.example.auth.service;

import com.example.auth.entity.PasswordResetToken;
import com.example.auth.repository.PasswordResetTokenRepository;
import com.example.common.exception.InvalidResetTokenException;
import com.example.common.mail.MailSender;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long TOKEN_TTL_MINUTES = 30;
    private static final long RATE_LIMIT_MINUTES = 1;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;

    @Value("${app.password-reset-url}")
    private String passwordResetUrl;

    @Transactional
    public void forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmailAndDeletedAtIsNull(email);
        if (userOpt.isEmpty() || userOpt.get().getPassword() == null) {
            return; // FR-R03: 계정 존재 여부를 노출하지 않기 위해 조용히 반환
        }

        User user = userOpt.get();
        if (tokenRepository.existsByUserIdAndCreatedAtAfter(
                user.getId(), LocalDateTime.now().minusMinutes(RATE_LIMIT_MINUTES))) {
            return; // FR-R07: 1분 내 재요청도 동일한 성공 응답을 유지하기 위해 조용히 반환
        }

        String rawToken = generateRawToken();
        PasswordResetToken token =
                PasswordResetToken.create(user.getId(), hash(rawToken), TOKEN_TTL_MINUTES);
        tokenRepository.save(token);

        String link = passwordResetUrl + "?token=" + rawToken;
        mailSender.send(user.getEmail(), "비밀번호 재설정 안내", link);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token =
                tokenRepository
                        .findByTokenAndUsedAtIsNull(hash(rawToken))
                        .orElseThrow(InvalidResetTokenException::new);

        if (token.isExpired()) {
            throw new InvalidResetTokenException();
        }

        User user =
                userRepository
                        .findByIdAndDeletedAtIsNull(token.getUserId())
                        .orElseThrow(InvalidResetTokenException::new);

        user.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed();
        tokenRepository.findAllByUserIdAndUsedAtIsNull(user.getId()).forEach(PasswordResetToken::markUsed); // FR-R05
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
