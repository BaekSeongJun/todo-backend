package com.example.todo.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TodoCreateRequestTest {

    private final Validator validator;

    TodoCreateRequestTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Test
    void title이_빈문자열이면_검증에_실패한다() {
        TodoCreateRequest request = new TodoCreateRequest("", null, null, null);

        Set<ConstraintViolation<TodoCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void title이_존재하면_검증을_통과한다() {
        TodoCreateRequest request = new TodoCreateRequest("할 일 제목", null, null, null);

        Set<ConstraintViolation<TodoCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
