package com.example.common.exception;

import org.springframework.http.HttpStatus;

public class SelfStatusChangeException extends BusinessException {

    public SelfStatusChangeException() {
        super(HttpStatus.BAD_REQUEST, "SELF_STATUS_CHANGE_NOT_ALLOWED", "자기 자신의 계정 상태는 변경할 수 없습니다.");
    }
}
