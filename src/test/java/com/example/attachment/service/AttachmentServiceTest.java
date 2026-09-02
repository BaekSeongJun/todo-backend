package com.example.attachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.attachment.dto.AttachmentResponse;
import com.example.attachment.entity.Attachment;
import com.example.attachment.repository.AttachmentRepository;
import com.example.common.exception.AttachmentLimitExceededException;
import com.example.common.exception.AttachmentNotFoundException;
import com.example.common.exception.AttachmentTooLargeException;
import com.example.common.exception.AttachmentTypeNotAllowedException;
import com.example.common.exception.TodoNotFoundException;
import com.example.common.storage.AttachmentTokenProvider;
import com.example.common.storage.FileStorage;
import com.example.todo.entity.Priority;
import com.example.todo.entity.Todo;
import com.example.todo.repository.TodoRepository;
import com.example.user.entity.AuthProvider;
import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class AttachmentServiceTest {

    private final TodoRepository todoRepository = Mockito.mock(TodoRepository.class);
    private final AttachmentRepository attachmentRepository = Mockito.mock(AttachmentRepository.class);
    private final FileStorage fileStorage = Mockito.mock(FileStorage.class);
    private final AttachmentTokenProvider tokenProvider = Mockito.mock(AttachmentTokenProvider.class);
    private final AttachmentService attachmentService =
            new AttachmentService(todoRepository, attachmentRepository, fileStorage, tokenProvider);

    AttachmentServiceTest() {
        ReflectionTestUtils.setField(attachmentService, "downloadUrlTtlSeconds", 300L);
    }

    private User user(Long id) {
        User user =
                User.builder()
                        .email("user" + id + "@test.com")
                        .password("encoded")
                        .name("테스터")
                        .provider(AuthProvider.LOCAL)
                        .role(UserRole.USER)
                        .enabled(true)
                        .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Todo todo(Long id, User owner) {
        Todo todo = Todo.builder().user(owner).title("제목").priority(Priority.MEDIUM).build();
        ReflectionTestUtils.setField(todo, "id", id);
        return todo;
    }

    private Attachment attachment(Long id, Todo todo, String originalName, String contentType, long size) {
        Attachment attachment = Attachment.create(todo, originalName, "stored-key.txt", contentType, size);
        ReflectionTestUtils.setField(attachment, "id", id);
        return attachment;
    }

    @Test
    void upload_이미_5개면_AttachmentLimitExceededException이_발생한다() {
        User owner = user(1L);
        Todo todo = todo(10L, owner);
        Mockito.when(todoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(todo));
        Mockito.when(attachmentRepository.countByTodoId(10L)).thenReturn(5L);

        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());

        assertThatThrownBy(() -> attachmentService.upload(1L, 10L, file))
                .isInstanceOf(AttachmentLimitExceededException.class);
    }

    @Test
    void upload_10MB를_초과하면_AttachmentTooLargeException이_발생한다() {
        User owner = user(1L);
        Todo todo = todo(10L, owner);
        Mockito.when(todoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(todo));
        Mockito.when(attachmentRepository.countByTodoId(10L)).thenReturn(0L);

        byte[] tooLarge = new byte[(int) (10 * 1024 * 1024) + 1];
        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", tooLarge);

        assertThatThrownBy(() -> attachmentService.upload(1L, 10L, file))
                .isInstanceOf(AttachmentTooLargeException.class);
    }

    @Test
    void upload_허용되지_않는_확장자이면_AttachmentTypeNotAllowedException이_발생한다() {
        User owner = user(1L);
        Todo todo = todo(10L, owner);
        Mockito.when(todoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(todo));
        Mockito.when(attachmentRepository.countByTodoId(10L)).thenReturn(0L);

        MultipartFile file =
                new MockMultipartFile("file", "malware.exe", "application/octet-stream", "x".getBytes());

        assertThatThrownBy(() -> attachmentService.upload(1L, 10L, file))
                .isInstanceOf(AttachmentTypeNotAllowedException.class);
    }

    @Test
    void upload_확장자와_ContentType이_불일치하면_AttachmentTypeNotAllowedException이_발생한다() {
        User owner = user(1L);
        Todo todo = todo(10L, owner);
        Mockito.when(todoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(todo));
        Mockito.when(attachmentRepository.countByTodoId(10L)).thenReturn(0L);

        MultipartFile file =
                new MockMultipartFile("file", "fake.jpg", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> attachmentService.upload(1L, 10L, file))
                .isInstanceOf(AttachmentTypeNotAllowedException.class);
    }

    @Test
    void upload_타인_소유_Todo이면_TodoNotFoundException이_발생한다() {
        Mockito.when(todoRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());

        assertThatThrownBy(() -> attachmentService.upload(2L, 10L, file))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void upload_정상_요청이면_저장되고_응답DTO를_반환한다() {
        User owner = user(1L);
        Todo todo = todo(10L, owner);
        Mockito.when(todoRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(todo));
        Mockito.when(attachmentRepository.countByTodoId(10L)).thenReturn(0L);
        Mockito.when(fileStorage.store(Mockito.any())).thenReturn("uuid-key.txt");
        Mockito.when(attachmentRepository.save(Mockito.any(Attachment.class)))
                .thenAnswer(
                        invocation -> {
                            Attachment saved = invocation.getArgument(0);
                            ReflectionTestUtils.setField(saved, "id", 100L);
                            return saved;
                        });

        MultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());

        AttachmentResponse response = attachmentService.upload(1L, 10L, file);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.originalName()).isEqualTo("a.txt");
    }

    @Test
    void getList_타인_소유_Todo이면_TodoNotFoundException이_발생한다() {
        Mockito.when(todoRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.getList(2L, 10L))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void getDownloadUrl_타인_소유_첨부이면_AttachmentNotFoundException이_발생한다() {
        Mockito.when(attachmentRepository.findByIdAndTodo_UserId(99L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.getDownloadUrl(2L, 99L))
                .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    void getDownloadUrl_소유자가_맞으면_FileStorage가_생성한_URL을_반환한다() {
        User owner = user(1L);
        Todo todo = todo(10L, owner);
        Attachment attachment = attachment(50L, todo, "a.txt", "text/plain", 10L);
        Mockito.when(attachmentRepository.findByIdAndTodo_UserId(50L, 1L))
                .thenReturn(Optional.of(attachment));
        Mockito.when(fileStorage.generateDownloadUrl(50L, "stored-key.txt", 300L))
                .thenReturn("/api/attachments/50/download?token=abc");

        String url = attachmentService.getDownloadUrl(1L, 50L);

        assertThat(url).isEqualTo("/api/attachments/50/download?token=abc");
    }

    @Test
    void delete_타인_소유_첨부이면_AttachmentNotFoundException이_발생한다() {
        Mockito.when(attachmentRepository.findByIdAndTodo_UserId(99L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.delete(2L, 99L))
                .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    void delete_소유자가_맞으면_softDelete_상태가_된다() {
        User owner = user(1L);
        Todo todo = todo(10L, owner);
        Attachment attachment = attachment(50L, todo, "a.txt", "text/plain", 10L);
        Mockito.when(attachmentRepository.findByIdAndTodo_UserId(50L, 1L))
                .thenReturn(Optional.of(attachment));

        attachmentService.delete(1L, 50L);

        assertThat(attachment.isDeleted()).isTrue();
    }
}
