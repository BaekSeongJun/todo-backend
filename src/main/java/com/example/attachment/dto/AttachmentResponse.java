package com.example.attachment.dto;

import com.example.attachment.entity.Attachment;
import java.time.LocalDateTime;

/** storedKey는 응답에 노출하지 않는다(FR-F04 저장 키 비공개 원칙). */
public record AttachmentResponse(
        Long id, String originalName, String contentType, Long sizeBytes, LocalDateTime createdAt) {

    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getCreatedAt());
    }
}
