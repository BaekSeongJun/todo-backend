package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class AttachmentLimitExceededException extends BusinessException {

    public AttachmentLimitExceededException() {
        super(HttpStatus.BAD_REQUEST, "ATTACHMENT_LIMIT_EXCEEDED", "첨부파일은 최대 5개까지 업로드할 수 있습니다.");
    }
}
