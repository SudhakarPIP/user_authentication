package com.auth.controller;

import com.auth.dto.ApiResponse;
import com.auth.dto.AuthResponse;
import com.auth.dto.LoginRequest;
import com.auth.dto.SignupRequest;
import com.auth.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        logger.info("Received signup request for username: {}, email: {}", 
                request.getUsername(), request.getEmail());
        AuthResponse response = authService.signup(request);
        logger.info("Signup request processed successfully for username: {}", request.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestParam String token) {
        logger.info("Received email verification request");
        AuthResponse response = authService.verifyEmail(token);
        logger.info("Email verification request processed successfully");
        ApiResponse apiResponse = ApiResponse.builder()
                .message(response.getMessage())
                .success(true)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Received login request for: {}", request.getUsernameOrEmail());
        AuthResponse response = authService.login(request);
        logger.info("Login request processed successfully for: {}", request.getUsernameOrEmail());
        return ResponseEntity.ok(response);
    }
}

