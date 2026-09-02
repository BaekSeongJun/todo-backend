package com.example.attachment.service;

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
import com.example.todo.entity.Todo;
import com.example.todo.repository.TodoRepository;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * FR-F01(5개·10MB)·FR-F02(확장자+Content-Type 화이트리스트)·FR-F04(UUID 저장키)·
 * FR-F05(서명 URL)·FR-F06(Soft Delete)·FR-T07(타인 Todo 404)을 구현한다. 모든 메서드가
 * Todo 소유권을 먼저 검증한 뒤에만 Attachment를 다뤄, Attachment.todo(ToOne)가 Soft
 * Delete된 Todo를 가리킬 때 발생하는 EntityNotFoundException 위험을 원천 차단한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentService {

    private static final int MAX_ATTACHMENTS_PER_TODO = 5;
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_EXTENSION_CONTENT_TYPES =
            Map.ofEntries(
                    Map.entry("jpg", "image/jpeg"),
                    Map.entry("png", "image/png"),
                    Map.entry("webp", "image/webp"),
                    Map.entry("gif", "image/gif"),
                    Map.entry("pdf", "application/pdf"),
                    Map.entry(
                            "docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                    Map.entry(
                            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    Map.entry("txt", "text/plain"),
                    Map.entry("zip", "application/zip"));

    private final TodoRepository todoRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileStorage fileStorage;
    private final AttachmentTokenProvider attachmentTokenProvider;

    @Value("${app.download-url-ttl-seconds}")
    private long downloadUrlTtlSeconds;

    @Transactional
    public AttachmentResponse upload(Long userId, Long todoId, MultipartFile file) {
        Todo todo = getOwnedTodo(userId, todoId);

        if (attachmentRepository.countByTodoId(todoId) >= MAX_ATTACHMENTS_PER_TODO) {
            throw new AttachmentLimitExceededException();
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AttachmentTooLargeException();
        }

        String extension = extractExtension(file.getOriginalFilename());
        String expectedContentType = ALLOWED_EXTENSION_CONTENT_TYPES.get(extension);
        if (expectedContentType == null || !expectedContentType.equals(file.getContentType())) {
            throw new AttachmentTypeNotAllowedException();
        }

        String storedKey = fileStorage.store(file);
        Attachment attachment =
                Attachment.create(
                        todo, file.getOriginalFilename(), storedKey, file.getContentType(), file.getSize());

        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    public List<AttachmentResponse> getList(Long userId, Long todoId) {
        getOwnedTodo(userId, todoId);
        return attachmentRepository.findAllByTodoIdOrderByCreatedAtAsc(todoId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    public String getDownloadUrl(Long userId, Long attachmentId) {
        Attachment attachment = getOwnedAttachment(userId, attachmentId);
        return fileStorage.generateDownloadUrl(
                attachment.getId(), attachment.getStoredKey(), downloadUrlTtlSeconds);
    }

    public AttachmentDownload streamDownload(Long attachmentId, String token) {
        Long tokenAttachmentId;
        try {
            tokenAttachmentId = attachmentTokenProvider.parseAttachmentId(token);
        } catch (JwtException e) {
            throw new AttachmentNotFoundException();
        }
        if (!tokenAttachmentId.equals(attachmentId)) {
            throw new AttachmentNotFoundException();
        }

        Attachment attachment =
                attachmentRepository.findById(attachmentId).orElseThrow(AttachmentNotFoundException::new);
        Resource resource = fileStorage.load(attachment.getStoredKey());

        return new AttachmentDownload(resource, attachment.getContentType(), attachment.getOriginalName());
    }

    @Transactional
    public void delete(Long userId, Long attachmentId) {
        Attachment attachment = getOwnedAttachment(userId, attachmentId);
        attachment.markDeleted();
    }

    private Todo getOwnedTodo(Long userId, Long todoId) {
        return todoRepository.findByIdAndUserId(todoId, userId).orElseThrow(TodoNotFoundException::new);
    }

    private Attachment getOwnedAttachment(Long userId, Long attachmentId) {
        return attachmentRepository
                .findByIdAndTodo_UserId(attachmentId, userId)
                .orElseThrow(AttachmentNotFoundException::new);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) return "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return originalFilename.substring(dotIndex + 1).toLowerCase();
    }

    public record AttachmentDownload(Resource resource, String contentType, String originalName) {}
}
