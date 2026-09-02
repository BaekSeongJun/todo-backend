package com.example.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.SignupRequest;
import com.example.auth.jwt.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private MockMvcTester mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mvc = create(mockMvc);
    }

    private String bodyOf(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(String json) {
        Map<String, Object> root = objectMapper.readValue(json, Map.class);
        return (Map<String, Object>) root.get("data");
    }

    private String signupAndGetToken(String email) {
        SignupRequest request = new SignupRequest(email, "password123", "테스터");
        MvcTestResult result =
                mvc.post()
                        .uri("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .exchange();
        result.assertThat().hasStatusOk();
        return (String) extractData(bodyOf(result)).get("accessToken");
    }

    private Long extractUserIdFromToken(String token) {
        return jwtTokenProvider.getUserId(token);
    }

    /** 회원가입 경로로는 ADMIN이 될 수 없으므로(FR-M03), DB에서 직접 role을 승격한 뒤 재로그인해 새 토큰을 받는다. */
    private String promoteToAdminAndGetToken(String email) {
        entityManager
                .createNativeQuery("UPDATE users SET role = 'ADMIN' WHERE email = :email")
                .setParameter("email", email)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        LoginRequest loginRequest = new LoginRequest(email, "password123");
        MvcTestResult result =
                mvc.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .exchange();
        result.assertThat().hasStatusOk();
        return (String) extractData(bodyOf(result)).get("accessToken");
    }

    @Test
    void USER_토큰으로_관리자_API_접근시_403을_반환한다() {
        String userToken = signupAndGetToken("admin-403@example.com");

        mvc.get()
                .uri("/api/admin/users")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .assertThat()
                .hasStatus(403)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void 토큰_없이_관리자_API_접근시_401을_반환한다() {
        mvc.get()
                .uri("/api/admin/users")
                .exchange()
                .assertThat()
                .hasStatus(401)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("UNAUTHORIZED");
    }

    @Test
    void 관리자_토큰으로_이메일_검색_페이지네이션이_정상_동작한다() {
        String adminToken = promoteToAdminAndGetToken(signupEmail("admin-search@example.com"));
        signupAndGetToken("search-target@example.com");
        signupAndGetToken("unrelated@example.com");

        MvcTestResult result =
                mvc.get()
                        .uri("/api/admin/users?email=search-target")
                        .header("Authorization", "Bearer " + adminToken)
                        .exchange();
        result.assertThat().hasStatusOk();

        Map<String, Object> page = extractData(bodyOf(result));
        List<?> content = (List<?>) page.get("content");
        assertThat(content).hasSize(1);
        assertThat(page).containsKeys("page", "size", "totalElements", "totalPages", "first", "last");
    }

    @Test
    void 관리자_자신의_id로_상태변경_시도시_400을_반환한다() {
        String email = "admin-self@example.com";
        String userToken = signupAndGetToken(email);
        Long adminId = extractUserIdFromToken(userToken);
        String adminToken = promoteToAdminAndGetToken(email);

        mvc.patch()
                .uri("/api/admin/users/{id}/status", adminId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}")
                .exchange()
                .assertThat()
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SELF_STATUS_CHANGE_NOT_ALLOWED");
    }

    @Test
    void 비활성화된_계정은_재로그인과_기존_토큰_모두_차단된다() {
        String adminToken = promoteToAdminAndGetToken(signupEmail("admin-deactivate@example.com"));
        String targetEmail = "target-deactivate@example.com";
        String targetToken = signupAndGetToken(targetEmail);
        Long targetId = extractUserIdFromToken(targetToken);

        mvc.patch()
                .uri("/api/admin/users/{id}/status", targetId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}")
                .exchange()
                .assertThat()
                .hasStatusOk();

        LoginRequest loginRequest = new LoginRequest(targetEmail, "password123");
        mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
                .exchange()
                .assertThat()
                .hasStatus(401);

        mvc.get()
                .uri("/api/todos")
                .header("Authorization", "Bearer " + targetToken)
                .exchange()
                .assertThat()
                .hasStatus(401);
    }

    @Test
    void 전체_할일_조회에_작성자_정보가_포함된다() {
        String adminToken = promoteToAdminAndGetToken(signupEmail("admin-todos@example.com"));
        String authorToken = signupAndGetToken("todo-author@example.com");

        mvc.post()
                .uri("/api/todos")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"관리자 조회 대상\",\"content\":\"내용\",\"priority\":\"MEDIUM\"}")
                .exchange()
                .assertThat()
                .hasStatusOk();

        MvcTestResult result =
                mvc.get()
                        .uri("/api/admin/todos")
                        .header("Authorization", "Bearer " + adminToken)
                        .exchange();
        result.assertThat().hasStatusOk();

        Map<String, Object> page = extractData(bodyOf(result));
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).isNotEmpty();
        Map<String, Object> first = content.get(0);
        assertThat(first.get("authorEmail")).isNotNull();
        assertThat(first.get("authorName")).isNotNull();
    }

    @Test
    void 전체_할일_조회에_제목_작성자_완료여부_우선순위_필터가_모두_적용된다() {
        String adminToken = promoteToAdminAndGetToken(signupEmail("admin-todo-filter@example.com"));
        String authorToken = signupAndGetToken("filter-author@example.com");

        mvc.post()
                .uri("/api/todos")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"장보기 목록\",\"content\":\"내용\",\"priority\":\"HIGH\"}")
                .exchange()
                .assertThat()
                .hasStatusOk();
        mvc.post()
                .uri("/api/todos")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"보고서 제출\",\"content\":\"내용\",\"priority\":\"LOW\"}")
                .exchange()
                .assertThat()
                .hasStatusOk();

        // 제목 검색
        Map<String, Object> byTitle =
                extractData(
                        bodyOf(
                                mvc.get()
                                        .uri("/api/admin/todos?title=장보기")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .exchange()));
        assertThat((List<?>) byTitle.get("content")).hasSize(1);

        // 작성자 검색
        Map<String, Object> byAuthor =
                extractData(
                        bodyOf(
                                mvc.get()
                                        .uri("/api/admin/todos?author=filter-author")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .exchange()));
        assertThat((List<?>) byAuthor.get("content")).hasSize(2);

        // 완료 여부 필터 (전부 미완료 상태이므로 completed=false로 둘 다 조회됨)
        Map<String, Object> byCompleted =
                extractData(
                        bodyOf(
                                mvc.get()
                                        .uri("/api/admin/todos?author=filter-author&completed=false")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .exchange()));
        assertThat((List<?>) byCompleted.get("content")).hasSize(2);

        // 우선순위 필터
        Map<String, Object> byPriority =
                extractData(
                        bodyOf(
                                mvc.get()
                                        .uri("/api/admin/todos?author=filter-author&priority=HIGH")
                                        .header("Authorization", "Bearer " + adminToken)
                                        .exchange()));
        assertThat((List<?>) byPriority.get("content")).hasSize(1);
    }

    @Test
    void 통계_API가_전체_사용자_활성_사용자_전체_할일_완료_할일_수를_반환한다() {
        String adminToken = promoteToAdminAndGetToken(signupEmail("admin-stats@example.com"));

        MvcTestResult result =
                mvc.get()
                        .uri("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken)
                        .exchange();
        result.assertThat().hasStatusOk();

        Map<String, Object> data = extractData(bodyOf(result));
        assertThat(data)
                .containsKeys("totalUsers", "activeUsers", "totalTodos", "completedTodos");
    }

    private String signupEmail(String email) {
        signupAndGetToken(email);
        return email;
    }
}
