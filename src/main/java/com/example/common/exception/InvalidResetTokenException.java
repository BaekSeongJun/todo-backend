package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidResetTokenException extends BusinessException {

    public InvalidResetTokenException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "유효하지 않거나 만료된 링크입니다.");
    }
}
