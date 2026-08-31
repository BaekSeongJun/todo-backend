package com.example.todo.dto;

import com.example.todo.entity.Priority;
import com.example.todo.entity.Todo;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodoResponse(
        Long id,
        String title,
        String content,
        boolean completed,
        LocalDate dueDate,
        Priority priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContent(),
                todo.isCompleted(),
                todo.getDueDate(),
                todo.getPriority(),
                todo.getCreatedAt(),
                todo.getUpdatedAt());
    }
}
