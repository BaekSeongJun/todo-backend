package com.example.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.auth.dto.TokenResponse;
import com.example.auth.jwt.JwtTokenProvider;
import com.example.auth.oauth.OneTimeCodeStore;
import com.example.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
    private final OneTimeCodeStore oneTimeCodeStore = Mockito.mock(OneTimeCodeStore.class);
    private final AuthService authService =
            new AuthService(userRepository, passwordEncoder, jwtTokenProvider, oneTimeCodeStore);

    @Test
    void exchangeCode_호출_시_consume_결과를_TokenResponse로_감싸_반환한다() {
        Mockito.when(oneTimeCodeStore.consume("code-1")).thenReturn("jwt-value");

        TokenResponse result = authService.exchangeCode("code-1");

        assertThat(result.accessToken()).isEqualTo("jwt-value");
    }
}
