package com.example.auth.controller;

import com.example.auth.dto.ForgotPasswordRequest;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.OAuthExchangeRequest;
import com.example.auth.dto.ResetPasswordRequest;
import com.example.auth.dto.SignupRequest;
import com.example.auth.dto.TokenResponse;
import com.example.auth.dto.UserResponse;
import com.example.auth.service.AuthService;
import com.example.auth.service.PasswordResetService;
import com.example.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    public ApiResponse<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success(authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/oauth/exchange")
    public ApiResponse<TokenResponse> exchangeOAuthCode(
            @Valid @RequestBody OAuthExchangeRequest request) {
        return ApiResponse.success(authService.exchangeCode(request.code()));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(authService.getMe(userId));
    }

    @PostMapping("/password/forgot")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request.email());
        return ApiResponse.success(null, "등록된 이메일이라면 재설정 링크를 보냈습니다.");
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.success(null, "비밀번호가 변경되었습니다.");
    }
}
