package com.example.attachment.controller;

import com.example.attachment.dto.AttachmentResponse;
import com.example.attachment.service.AttachmentService;
import com.example.common.response.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/api/todos/{todoId}/attachments")
    public ApiResponse<AttachmentResponse> upload(
            Authentication authentication,
            @PathVariable Long todoId,
            @RequestParam("file") MultipartFile file) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(attachmentService.upload(userId, todoId, file));
    }

    @GetMapping("/api/todos/{todoId}/attachments")
    public ApiResponse<List<AttachmentResponse>> list(
            Authentication authentication, @PathVariable Long todoId) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(attachmentService.getList(userId, todoId));
    }

    @GetMapping("/api/attachments/{id}/download-url")
    public ApiResponse<Map<String, String>> downloadUrl(
            Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(Map.of("url", attachmentService.getDownloadUrl(userId, id)));
    }

    @GetMapping("/api/attachments/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, @RequestParam String token) {
        AttachmentService.AttachmentDownload download = attachmentService.streamDownload(id, token);

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(download.originalName(), StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.resource());
    }

    @DeleteMapping("/api/attachments/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        attachmentService.delete(userId, id);
        return ApiResponse.success(null);
    }
}
