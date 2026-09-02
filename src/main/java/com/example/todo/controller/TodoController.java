package com.example.todo.controller;

import com.example.common.response.ApiResponse;
import com.example.common.response.PageResponse;
import com.example.todo.dto.TodoCreateRequest;
import com.example.todo.dto.TodoResponse;
import com.example.todo.dto.TodoSortBy;
import com.example.todo.dto.TodoUpdateRequest;
import com.example.todo.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public ApiResponse<PageResponse<TodoResponse>> getList(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(required = false) String title) {
        Long userId = (Long) authentication.getPrincipal();

        Boolean completed = toCompleted(status);
        Pageable pageable = PageRequest.of(page, size, toSort(sort));

        return ApiResponse.success(
                PageResponse.from(todoService.getList(userId, completed, title, pageable)));
    }

    @PostMapping
    public ApiResponse<TodoResponse> create(
            Authentication authentication, @Valid @RequestBody TodoCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(todoService.create(userId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TodoResponse> getOne(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(todoService.getOne(userId, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TodoResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody TodoUpdateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(todoService.update(userId, id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<TodoResponse> toggle(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(todoService.toggle(userId, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        todoService.delete(userId, id);
        return ApiResponse.success(null);
    }

    private Boolean toCompleted(String status) {
        return switch (status) {
            case "completed" -> Boolean.TRUE;
            case "pending" -> Boolean.FALSE;
            default -> null;
        };
    }

    private Sort toSort(String sort) {
        TodoSortBy sortBy = "dueDate".equals(sort) ? TodoSortBy.DUE_DATE : TodoSortBy.CREATED_AT;
        String property = sortBy == TodoSortBy.DUE_DATE ? "dueDate" : "createdAt";
        return Sort.by(Sort.Direction.DESC, property);
    }
}
