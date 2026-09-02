package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class AttachmentTooLargeException extends BusinessException {

    public AttachmentTooLargeException() {
        super(HttpStatus.BAD_REQUEST, "ATTACHMENT_TOO_LARGE", "파일 크기는 10MB를 초과할 수 없습니다.");
    }
}
