package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class TodoNotFoundException extends BusinessException {

    public TodoNotFoundException() {
        super(HttpStatus.NOT_FOUND, "TODO_NOT_FOUND", "할 일을 찾을 수 없습니다.");
    }
}
