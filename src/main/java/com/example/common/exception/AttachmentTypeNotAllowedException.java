package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class AttachmentTypeNotAllowedException extends BusinessException {

    public AttachmentTypeNotAllowedException() {
        super(HttpStatus.BAD_REQUEST, "ATTACHMENT_TYPE_NOT_ALLOWED", "허용되지 않는 파일 형식입니다.");
    }
}
