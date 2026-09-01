package com.example.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.example.common.exception.InvalidResetTokenException;
import com.example.common.mail.MailSender;
import com.example.user.entity.AuthProvider;
import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import com.example.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordResetServiceTest {

    @Autowired private PasswordResetService passwordResetService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private MailSender mailSender;

    private User createLocalUser(String email) {
        User user =
                User.builder()
                        .email(email)
                        .password(passwordEncoder.encode("oldpass1"))
                        .name("테스터")
                        .provider(AuthProvider.LOCAL)
                        .role(UserRole.USER)
                        .enabled(true)
                        .build();
        return userRepository.save(user);
    }

    private String requestAndCaptureToken(String email) {
        passwordResetService.forgotPassword(email);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(eq(email), anyString(), bodyCaptor.capture());

        String link = bodyCaptor.getValue();
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    @Test
    void 재설정_성공_후_같은_토큰을_재사용하면_실패한다() {
        User user = createLocalUser("reuse-test@example.com");
        String rawToken = requestAndCaptureToken(user.getEmail());

        passwordResetService.resetPassword(rawToken, "newpass1");

        assertThatThrownBy(() -> passwordResetService.resetPassword(rawToken, "anotherpass1"))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void 미가입_이메일도_예외없이_동일하게_처리된다() {
        assertThatCode(() -> passwordResetService.forgotPassword("no-such-user@example.com"))
                .doesNotThrowAnyException();

        verify(mailSender, org.mockito.Mockito.never()).send(anyString(), anyString(), any());
    }

    @Test
    void 소셜_전용_계정도_예외없이_동일하게_처리된다() {
        User socialUser =
                userRepository.save(
                        User.builder()
                                .email("social-only@example.com")
                                .password(null)
                                .name("소셜테스터")
                                .provider(AuthProvider.GOOGLE)
                                .providerId("google-1")
                                .role(UserRole.USER)
                                .enabled(true)
                                .build());

        assertThatCode(() -> passwordResetService.forgotPassword(socialUser.getEmail()))
                .doesNotThrowAnyException();

        verify(mailSender, org.mockito.Mockito.never()).send(anyString(), anyString(), any());
    }

    @Test
    void 재설정_성공_시_비밀번호가_실제로_변경된다() {
        User user = createLocalUser("changed-test@example.com");
        String rawToken = requestAndCaptureToken(user.getEmail());

        passwordResetService.resetPassword(rawToken, "brandnew1");

        User reloaded = userRepository.findByEmailAndDeletedAtIsNull(user.getEmail()).orElseThrow();
        assertThat(passwordEncoder.matches("brandnew1", reloaded.getPassword())).isTrue();
    }
}
