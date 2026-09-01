package com.example.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.SignupRequest;
import com.example.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    private MockMvcTester mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mvc = create(mockMvc);
    }

    @Test
    void 회원가입_성공_시_사용자가_생성되고_JWT를_반환한다() {
        SignupRequest request = new SignupRequest("signup@example.com", "password123", "테스터");

        mvc.post()
                .uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.accessToken")
                .isNotNull();

        assertThat(userRepository.findByEmailAndDeletedAtIsNull("signup@example.com")).isPresent();
    }

    @Test
    void 로그인_성공_시_유효한_JWT를_반환한다() {
        SignupRequest signupRequest = new SignupRequest("login@example.com", "password123", "테스터");
        mvc.post()
                .uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest))
                .exchange();

        LoginRequest loginRequest = new LoginRequest("login@example.com", "password123");

        mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.accessToken")
                .isNotNull();
    }

    @Test
    void 토큰_없이_보호된_엔드포인트_접근시_401을_반환한다() {
        mvc.get()
                .uri("/api/todos")
                .exchange()
                .assertThat()
                .hasStatus(401)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("UNAUTHORIZED");
    }
}
