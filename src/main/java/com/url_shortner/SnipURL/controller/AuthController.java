package com.url_shortner.SnipURL.controller;

import com.url_shortner.SnipURL.dto.AuthResponse;
import com.url_shortner.SnipURL.dto.LoginRequest;
import com.url_shortner.SnipURL.dto.RegisterRequest;
import com.url_shortner.SnipURL.entity.User;
import com.url_shortner.SnipURL.security.JwtUtil;
import com.url_shortner.SnipURL.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registering user: {}", request.getEmail());

        User user = userService.register(request);
        String token = jwtUtil.generateToken(user.getEmail());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .expiresAt(System.currentTimeMillis() + 86400000)
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt: {}", request.getEmail());

        // 1. Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Generate JWT token
        String token = jwtUtil.generateToken(request.getEmail());

        // 3. Get user details
        User user = userService.findByEmail(request.getEmail());

        // 4. Return token
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .expiresAt(System.currentTimeMillis() + 86400000)
                .build());
    }
}