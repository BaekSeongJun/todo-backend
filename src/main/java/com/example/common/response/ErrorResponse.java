package com.example.common.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        OffsetDateTime timestamp, int status, String code, String message, List<FieldError> errors) {

    public record FieldError(String field, String message) {}

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(OffsetDateTime.now(), status, code, message, null);
    }

    public static ErrorResponse of(int status, String code, String message, List<FieldError> errors) {
        return new ErrorResponse(OffsetDateTime.now(), status, code, message, errors);
    }
}
