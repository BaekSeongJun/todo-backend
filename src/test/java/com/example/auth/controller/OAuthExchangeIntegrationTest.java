package com.example.auth.controller;

import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.example.auth.dto.OAuthExchangeRequest;
import com.example.auth.jwt.JwtTokenProvider;
import com.example.auth.oauth.OneTimeCodeStore;
import com.example.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class OAuthExchangeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OneTimeCodeStore oneTimeCodeStore;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private MockMvcTester mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mvc = create(mockMvc);
    }

    private String issueOneTimeCode() {
        String jwt = jwtTokenProvider.generateToken(999L, UserRole.USER);
        return oneTimeCodeStore.issue(jwt);
    }

    @Test
    void 유효한_일회용_코드는_JWT로_정상_교환된다() {
        String code = issueOneTimeCode();
        OAuthExchangeRequest request = new OAuthExchangeRequest(code);

        mvc.post()
                .uri("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .exchange()
                .assertThat()
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.accessToken")
                .isNotNull();
    }

    @Test
    void 이미_사용된_일회용_코드는_재사용시_실패한다() {
        String code = issueOneTimeCode();
        OAuthExchangeRequest request = new OAuthExchangeRequest(code);
        String body = objectMapper.writeValueAsString(request);

        mvc.post()
                .uri("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange()
                .assertThat()
                .hasStatusOk();

        mvc.post()
                .uri("/api/auth/oauth/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange()
                .assertThat()
                .hasStatus(401)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("INVALID_OAUTH_CODE");
    }
}
