package com.example.todo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.example.auth.dto.SignupRequest;
import com.example.todo.dto.TodoCreateRequest;
import com.example.todo.dto.TodoUpdateRequest;
import com.example.todo.entity.Priority;
import jakarta.persistence.EntityManager;
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
class TodoControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;

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

    private Long createTodo(String token, String title, String content) {
        TodoCreateRequest request = new TodoCreateRequest(title, content, null, Priority.MEDIUM);
        MvcTestResult result =
                mvc.post()
                        .uri("/api/todos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .exchange();
        result.assertThat().hasStatusOk();
        return ((Number) extractData(bodyOf(result)).get("id")).longValue();
    }

    @Test
    void CRUD_및_페이지네이션이_정상_동작한다() {
        String token = signupAndGetToken("todo-crud@example.com");

        createTodo(token, "할 일 1", "내용1");
        createTodo(token, "할 일 2", "내용2");
        Long thirdId = createTodo(token, "할 일 3", "내용3");

        MvcTestResult listResult =
                mvc.get()
                        .uri("/api/todos?page=0&size=2")
                        .header("Authorization", "Bearer " + token)
                        .exchange();
        listResult.assertThat().hasStatusOk();
        Map<String, Object> page = extractData(bodyOf(listResult));
        assertThat((java.util.List<?>) page.get("content")).hasSize(2);
        assertThat(((Number) page.get("totalElements")).longValue()).isEqualTo(3);
        assertThat((Boolean) page.get("first")).isTrue();

        TodoUpdateRequest updateRequest =
                new TodoUpdateRequest("수정된 제목", "수정된 내용", null, Priority.HIGH);
        MvcTestResult updateResult =
                mvc.put()
                        .uri("/api/todos/{id}", thirdId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .exchange();
        updateResult.assertThat().hasStatusOk();
        assertThat(extractData(bodyOf(updateResult)).get("title")).isEqualTo("수정된 제목");

        MvcTestResult toggleResult =
                mvc.patch()
                        .uri("/api/todos/{id}/toggle", thirdId)
                        .header("Authorization", "Bearer " + token)
                        .exchange();
        toggleResult.assertThat().hasStatusOk();
        assertThat(extractData(bodyOf(toggleResult)).get("completed")).isEqualTo(true);
    }

    @Test
    void 삭제된_할일은_deleted_at이_기록되고_목록에서_제외된다() {
        String token = signupAndGetToken("todo-delete@example.com");
        Long id = createTodo(token, "삭제될 할 일", "내용");

        mvc.delete()
                .uri("/api/todos/{id}", id)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .assertThat()
                .hasStatusOk();

        Object deletedAt =
                entityManager
                        .createNativeQuery("select deleted_at from todos where id = :id")
                        .setParameter("id", id)
                        .getSingleResult();
        assertThat(deletedAt).isNotNull();

        MvcTestResult listResult =
                mvc.get().uri("/api/todos").header("Authorization", "Bearer " + token).exchange();
        listResult.assertThat().hasStatusOk();
        Map<String, Object> page = extractData(bodyOf(listResult));
        assertThat((java.util.List<?>) page.get("content")).isEmpty();
    }

    @Test
    void 타인의_할일_조회_수정_삭제는_404를_반환한다() {
        String ownerToken = signupAndGetToken("todo-owner@example.com");
        String otherToken = signupAndGetToken("todo-other@example.com");
        Long id = createTodo(ownerToken, "소유자의 할 일", "내용");

        mvc.get()
                .uri("/api/todos/{id}", id)
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .assertThat()
                .hasStatus(404)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("TODO_NOT_FOUND");

        TodoUpdateRequest updateRequest = new TodoUpdateRequest("탈취 시도", null, null, Priority.LOW);
        mvc.put()
                .uri("/api/todos/{id}", id)
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .exchange()
                .assertThat()
                .hasStatus(404)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("TODO_NOT_FOUND");

        mvc.delete()
                .uri("/api/todos/{id}", id)
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .assertThat()
                .hasStatus(404)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("TODO_NOT_FOUND");
    }

    @Test
    void 본문에_script_태그를_포함해도_정제되어_저장된다() {
        String token = signupAndGetToken("todo-sanitize@example.com");
        String maliciousContent = "<script>alert(1)</script>안전한 내용";

        TodoCreateRequest request =
                new TodoCreateRequest("정제 테스트", maliciousContent, null, Priority.MEDIUM);

        MvcTestResult result =
                mvc.post()
                        .uri("/api/todos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .exchange();
        result.assertThat().hasStatusOk();

        String savedContent = (String) extractData(bodyOf(result)).get("content");
        assertThat(savedContent).doesNotContain("<script>").doesNotContain("alert(1)");
        assertThat(savedContent).contains("안전한 내용");
    }
}
