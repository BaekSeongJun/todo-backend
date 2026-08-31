package com.example.todo.dto;

import com.example.todo.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TodoCreateRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        String content,
        LocalDate dueDate,
        Priority priority) {}
