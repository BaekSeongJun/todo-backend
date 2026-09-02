package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class AttachmentNotFoundException extends BusinessException {

    public AttachmentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "ATTACHMENT_NOT_FOUND", "첨부파일을 찾을 수 없습니다.");
    }
}
