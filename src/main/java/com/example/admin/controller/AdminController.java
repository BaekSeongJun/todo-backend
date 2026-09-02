package com.example.admin.controller;

import com.example.admin.dto.AdminTodoResponse;
import com.example.admin.dto.StatsResponse;
import com.example.admin.dto.UserAdminResponse;
import com.example.admin.dto.UserStatusUpdateRequest;
import com.example.admin.service.AdminService;
import com.example.common.response.ApiResponse;
import com.example.common.response.PageResponse;
import com.example.todo.entity.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserAdminResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String email) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(PageResponse.from(adminService.getUsers(email, pageable)));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<UserAdminResponse> updateUserStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody UserStatusUpdateRequest request) {
        Long adminUserId = (Long) authentication.getPrincipal();
        return ApiResponse.success(
                adminService.updateUserStatus(adminUserId, id, request.enabled()));
    }

    @GetMapping("/todos")
    public ApiResponse<PageResponse<AdminTodoResponse>> getTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Priority priority) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(
                PageResponse.from(adminService.getTodos(title, author, completed, priority, pageable)));
    }

    @GetMapping("/stats")
    public ApiResponse<StatsResponse> getStats() {
        return ApiResponse.success(adminService.getStats());
    }
}
