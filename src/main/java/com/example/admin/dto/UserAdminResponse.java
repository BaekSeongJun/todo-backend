package com.example.admin.dto;

import com.example.user.entity.AuthProvider;
import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import java.time.LocalDateTime;

public record UserAdminResponse(
        Long id,
        String email,
        String name,
        AuthProvider provider,
        UserRole role,
        boolean enabled,
        LocalDateTime createdAt) {

    public static UserAdminResponse from(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProvider(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt());
    }
}
