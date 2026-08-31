package com.example.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.example.auth.jwt.JwtTokenProvider;
import com.example.common.exception.UnauthorizedException;
import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import com.example.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2LoginSuccessHandlerTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final JwtTokenProvider jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
    private final OneTimeCodeStore oneTimeCodeStore = Mockito.mock(OneTimeCodeStore.class);
    private final OAuth2LoginSuccessHandler handler =
            new OAuth2LoginSuccessHandler(userRepository, jwtTokenProvider, oneTimeCodeStore);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "oauthRedirectUrl", "http://localhost:3000/oauth/callback");
    }

    private OAuth2User oAuth2UserOf(String email) {
        return new DefaultOAuth2User(
                List.of(), Map.of("email", email, "sub", "google-sub"), "email");
    }

    @Test
    void 인증_성공_시_code_파라미터로_리다이렉트하고_JWT는_노출하지_않는다() throws IOException {
        User user = User.builder().email("hong@example.com").name("홍길동").role(UserRole.USER).build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(oAuth2UserOf("hong@example.com"));

        Mockito.when(userRepository.findByEmailAndDeletedAtIsNull("hong@example.com"))
                .thenReturn(Optional.of(user));
        Mockito.when(jwtTokenProvider.generateToken(1L, UserRole.USER)).thenReturn("jwt-secret-value");
        Mockito.when(oneTimeCodeStore.issue("jwt-secret-value")).thenReturn("code-uuid");

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.encodeRedirectURL(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(response).sendRedirect(urlCaptor.capture());
        String redirectedUrl = urlCaptor.getValue();

        assertThat(redirectedUrl).isEqualTo("http://localhost:3000/oauth/callback?code=code-uuid");
        assertThat(redirectedUrl).doesNotContain("jwt-secret-value");
    }

    @Test
    void 존재하지_않는_이메일이면_UnauthorizedException이_발생한다() {
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(oAuth2UserOf("unknown@example.com"));
        Mockito.when(userRepository.findByEmailAndDeletedAtIsNull("unknown@example.com"))
                .thenReturn(Optional.empty());

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(request, response, authentication))
                .isInstanceOf(UnauthorizedException.class);

        Mockito.verify(jwtTokenProvider, Mockito.never()).generateToken(any(), any());
    }
}
