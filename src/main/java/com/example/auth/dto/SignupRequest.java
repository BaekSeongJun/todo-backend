package com.example.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email @NotBlank String email,
        @Size(min = 6) @NotBlank String password,
        @Size(min = 1, max = 50) @NotBlank String name) {}
