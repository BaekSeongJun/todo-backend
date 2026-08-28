package com.example.auth.service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.SignupRequest;
import com.example.auth.dto.TokenResponse;
import com.example.auth.dto.UserResponse;
import com.example.auth.jwt.JwtTokenProvider;
import com.example.common.exception.DuplicateEmailException;
import com.example.common.exception.InvalidCredentialsException;
import com.example.common.exception.UnauthorizedException;
import com.example.user.entity.AuthProvider;
import com.example.user.entity.User;
import com.example.user.entity.UserRole;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new DuplicateEmailException();
        }

        User user =
                User.builder()
                        .email(request.email())
                        .password(passwordEncoder.encode(request.password()))
                        .name(request.name())
                        .provider(AuthProvider.LOCAL)
                        .role(UserRole.USER)
                        .enabled(true)
                        .build();
        userRepository.save(user);

        return new TokenResponse(jwtTokenProvider.generateToken(user.getId(), user.getRole()));
    }

    public TokenResponse login(LoginRequest request) {
        User user =
                userRepository
                        .findByEmailAndDeletedAtIsNull(request.email())
                        .orElseThrow(InvalidCredentialsException::new);

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())
                || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        return new TokenResponse(jwtTokenProvider.generateToken(user.getId(), user.getRole()));
    }

    public UserResponse getMe(Long userId) {
        User user =
                userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(UnauthorizedException::new);
        return UserResponse.from(user);
    }
}
