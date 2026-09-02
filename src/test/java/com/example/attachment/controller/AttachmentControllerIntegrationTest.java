package com.example.attachment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.assertj.MockMvcTester.create;

import com.example.auth.dto.SignupRequest;
import com.example.common.storage.AttachmentTokenProvider;
import com.example.todo.dto.TodoCreateRequest;
import com.example.todo.entity.Priority;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * PRD 12.2 항목 16(용량·형식·개수 제한)·17(타인 할 일 404)에 대응한다. Phase 8에서 확립된
 * @DataJpaTest 패키지 이동·Replace.NONE 필수 사실은 여기서는 @SpringBootTest 전체 컨텍스트를
 * 쓰므로 해당 없고, application-test.yml의 app.upload-dir(./target/test-uploads)로
 * 실제 개발 업로드 디렉터리를 오염시키지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttachmentControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private AttachmentTokenProvider attachmentTokenProvider;

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

    private Long createTodo(String token, String title) {
        TodoCreateRequest request = new TodoCreateRequest(title, "내용", null, Priority.MEDIUM);
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

    private MockMultipartFile pngFile(String name, byte[] content) {
        return new MockMultipartFile("file", name, "image/png", content);
    }

    private MvcTestResult upload(String token, Long todoId, MockMultipartFile file) {
        return mvc.post()
                .uri("/api/todos/{todoId}/attachments", todoId)
                .header("Authorization", "Bearer " + token)
                .multipart()
                .file(file)
                .exchange();
    }

    @Test
    void 십메가바이트_초과_파일_업로드는_400과_ATTACHMENT_TOO_LARGE를_반환한다() {
        String token = signupAndGetToken("attach-toolarge@example.com");
        Long todoId = createTodo(token, "용량 초과 테스트");
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];

        upload(token, todoId, pngFile("big.png", oversized))
                .assertThat()
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("ATTACHMENT_TOO_LARGE");
    }

    @Test
    void 허용되지_않는_확장자_업로드는_400과_ATTACHMENT_TYPE_NOT_ALLOWED를_반환한다() {
        String token = signupAndGetToken("attach-badtype@example.com");
        Long todoId = createTodo(token, "확장자 제한 테스트");
        MockMultipartFile exeFile =
                new MockMultipartFile(
                        "file", "malware.exe", "application/octet-stream", "content".getBytes());

        upload(token, todoId, exeFile)
                .assertThat()
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("ATTACHMENT_TYPE_NOT_ALLOWED");
    }

    @Test
    void 여섯번째_파일_업로드는_400과_ATTACHMENT_LIMIT_EXCEEDED를_반환한다() {
        String token = signupAndGetToken("attach-limit@example.com");
        Long todoId = createTodo(token, "개수 제한 테스트");

        for (int i = 0; i < 5; i++) {
            upload(token, todoId, pngFile("file" + i + ".png", "data".getBytes()))
                    .assertThat()
                    .hasStatusOk();
        }

        upload(token, todoId, pngFile("file5.png", "data".getBytes()))
                .assertThat()
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("ATTACHMENT_LIMIT_EXCEEDED");
    }

    @Test
    void 타인의_할일에_업로드_시도하면_404와_TODO_NOT_FOUND를_반환한다() {
        String ownerToken = signupAndGetToken("attach-owner1@example.com");
        String otherToken = signupAndGetToken("attach-other1@example.com");
        Long todoId = createTodo(ownerToken, "소유자의 할 일");

        upload(otherToken, todoId, pngFile("intruder.png", "data".getBytes()))
                .assertThat()
                .hasStatus(404)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("TODO_NOT_FOUND");
    }

    @Test
    void 타인의_첨부에_다운로드URL_발급과_삭제를_시도하면_404와_ATTACHMENT_NOT_FOUND를_반환한다() {
        String ownerToken = signupAndGetToken("attach-owner2@example.com");
        String otherToken = signupAndGetToken("attach-other2@example.com");
        Long todoId = createTodo(ownerToken, "소유자의 할 일 2");

        MvcTestResult uploadResult = upload(ownerToken, todoId, pngFile("secret.png", "data".getBytes()));
        uploadResult.assertThat().hasStatusOk();
        Long attachmentId = ((Number) extractData(bodyOf(uploadResult)).get("id")).longValue();

        mvc.get()
                .uri("/api/attachments/{id}/download-url", attachmentId)
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .assertThat()
                .hasStatus(404)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("ATTACHMENT_NOT_FOUND");

        mvc.delete()
                .uri("/api/attachments/{id}", attachmentId)
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .assertThat()
                .hasStatus(404)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("ATTACHMENT_NOT_FOUND");
    }

    @Test
    void 정상_업로드_후_목록조회_다운로드URL발급_다운로드까지_전체_흐름이_동작한다() {
        String token = signupAndGetToken("attach-happy@example.com");
        Long todoId = createTodo(token, "정상 흐름 테스트");
        byte[] content = "png-binary-content".getBytes();

        MvcTestResult uploadResult = upload(token, todoId, pngFile("photo.png", content));
        uploadResult.assertThat().hasStatusOk();
        Long attachmentId = ((Number) extractData(bodyOf(uploadResult)).get("id")).longValue();

        MvcTestResult listResult =
                mvc.get()
                        .uri("/api/todos/{todoId}/attachments", todoId)
                        .header("Authorization", "Bearer " + token)
                        .exchange();
        listResult.assertThat().hasStatusOk();
        Map<String, Object> root = objectMapper.readValue(bodyOf(listResult), Map.class);
        List<?> list = (List<?>) root.get("data");
        assertThat(list).hasSize(1);

        MvcTestResult urlResult =
                mvc.get()
                        .uri("/api/attachments/{id}/download-url", attachmentId)
                        .header("Authorization", "Bearer " + token)
                        .exchange();
        urlResult.assertThat().hasStatusOk();
        String downloadUrl = (String) extractData(bodyOf(urlResult)).get("url");
        assertThat(downloadUrl).startsWith("/api/attachments/" + attachmentId + "/download?token=");

        String query = downloadUrl.substring(downloadUrl.indexOf('?') + 1);

        MvcTestResult downloadResult =
                mvc.get().uri("/api/attachments/{id}/download?" + query, attachmentId).exchange();
        downloadResult.assertThat().hasStatusOk();
        assertThat(downloadResult.getResponse().getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    void 위조된_토큰으로_다운로드하면_404를_반환한다() {
        String token = signupAndGetToken("attach-forged@example.com");
        Long todoId = createTodo(token, "위조 토큰 테스트");

        MvcTestResult uploadResult = upload(token, todoId, pngFile("real.png", "data".getBytes()));
        uploadResult.assertThat().hasStatusOk();
        Long attachmentId = ((Number) extractData(bodyOf(uploadResult)).get("id")).longValue();

        String forgedToken = attachmentTokenProvider.generateToken(attachmentId + 999, 300);

        mvc.get()
                .uri("/api/attachments/{id}/download?token=" + forgedToken, attachmentId)
                .exchange()
                .assertThat()
                .hasStatus(404);
    }

    @Test
    void 삭제하면_목록에서_제외되고_deleted_at이_기록된다() {
        String token = signupAndGetToken("attach-delete@example.com");
        Long todoId = createTodo(token, "삭제 테스트");

        MvcTestResult uploadResult = upload(token, todoId, pngFile("todelete.png", "data".getBytes()));
        uploadResult.assertThat().hasStatusOk();
        Long attachmentId = ((Number) extractData(bodyOf(uploadResult)).get("id")).longValue();

        mvc.delete()
                .uri("/api/attachments/{id}", attachmentId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .assertThat()
                .hasStatusOk();

        Object deletedAt =
                entityManager
                        .createNativeQuery("select deleted_at from attachments where id = :id")
                        .setParameter("id", attachmentId)
                        .getSingleResult();
        assertThat(deletedAt).isNotNull();

        MvcTestResult listResult =
                mvc.get()
                        .uri("/api/todos/{todoId}/attachments", todoId)
                        .header("Authorization", "Bearer " + token)
                        .exchange();
        listResult.assertThat().hasStatusOk();
        Map<String, Object> root = objectMapper.readValue(bodyOf(listResult), Map.class);
        assertThat((List<?>) root.get("data")).isEmpty();
    }
}
