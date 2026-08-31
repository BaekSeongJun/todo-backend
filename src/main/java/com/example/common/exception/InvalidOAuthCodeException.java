package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOAuthCodeException extends BusinessException {

    public InvalidOAuthCodeException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_CODE", "유효하지 않거나 만료된 코드입니다.");
    }
}
