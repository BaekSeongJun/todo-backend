package com.example.admin.dto;

import com.example.todo.entity.Priority;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminTodoResponse(
        Long id,
        String title,
        boolean completed,
        LocalDate dueDate,
        Priority priority,
        Long authorId,
        String authorEmail,
        String authorName,
        LocalDateTime createdAt) {}
