package com.example.auth.dto;

import com.example.user.entity.User;
import com.example.user.entity.UserRole;

public record UserResponse(Long id, String email, String name, UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
