package com.auth.controller;

import com.auth.dto.AuthResponse;
import com.auth.dto.LoginRequest;
import com.auth.dto.SignupRequest;
import com.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController API Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/signup - Success")
    void testSignup_Success() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setMobile("1234567890");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .message("Signup successful. Please check your email to verify your account.")
                .username("testuser")
                .email("test@example.com")
                .build();

        when(authService.signup(any(SignupRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/signup - Validation Error: Empty Username")
    void testSignup_ValidationError_EmptyUsername() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("");
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setMobile("1234567890");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/signup - Validation Error: Invalid Email")
    void testSignup_ValidationError_InvalidEmail() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setName("Test User");
        request.setEmail("invalid-email");
        request.setMobile("1234567890");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/signup - Validation Error: Short Password")
    void testSignup_ValidationError_ShortPassword() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setMobile("1234567890");
        request.setPassword("short");

        // When & Then
        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/signup - Validation Error: Invalid Mobile")
    void testSignup_ValidationError_InvalidMobile() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setMobile("123"); // Too short
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/signup - Username Already Exists")
    void testSignup_UsernameAlreadyExists() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("existinguser");
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setMobile("1234567890");
        request.setPassword("password123");

        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already exists. Please choose a different username."));

        // When & Then
        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username already exists. Please choose a different username."));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Email Already Exists")
    void testSignup_EmailAlreadyExists() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setName("Test User");
        request.setEmail("existing@example.com");
        request.setMobile("1234567890");
        request.setPassword("password123");

        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new IllegalArgumentException("Email already exists. Please use a different email address."));

        // When & Then
        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/signup - Missing Required Fields")
    void testSignup_MissingRequiredFields() throws Exception {
        // Given
        SignupRequest request = new SignupRequest();
        // All fields are null

        // When & Then
        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/verify - Success")
    void testVerifyEmail_Success() throws Exception {
        // Given
        String token = "valid-token";
        AuthResponse response = AuthResponse.builder()
                .message("Email verified successfully. Your account is now activated.")
                .username("testuser")
                .email("test@example.com")
                .build();

        when(authService.verifyEmail(token)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/verify")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully. Your account is now activated."));
    }

    @Test
    @DisplayName("GET /api/v1/verify - Missing Token Parameter")
    void testVerifyEmail_MissingToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/verify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Verification token is required"));
    }

    @Test
    @DisplayName("GET /api/v1/verify - Empty Token")
    void testVerifyEmail_EmptyToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/verify")
                .param("token", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/verify - Invalid Token")
    void testVerifyEmail_InvalidToken() throws Exception {
        // Given
        String token = "invalid-token";
        when(authService.verifyEmail(token))
                .thenThrow(new IllegalArgumentException("Invalid or expired verification token. Please request a new verification email."));

        // When & Then
        mockMvc.perform(get("/api/v1/verify")
                .param("token", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/verify - Expired Token")
    void testVerifyEmail_ExpiredToken() throws Exception {
        // Given
        String token = "expired-token";
        when(authService.verifyEmail(token))
                .thenThrow(new IllegalArgumentException("Verification token has expired. Please request a new verification email."));

        // When & Then
        mockMvc.perform(get("/api/v1/verify")
                .param("token", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/login - Success")
    void testLogin_Success() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .message("Login successful")
                .username("testuser")
                .email("test@example.com")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("POST /api/v1/login - Success with Email")
    void testLogin_SuccessWithEmail() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .message("Login successful")
                .username("testuser")
                .email("test@example.com")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("POST /api/v1/login - Account Not Activated")
    void testLogin_AccountNotActivated() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalStateException("Account not activated. Please verify your email first."));

        // When & Then
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account not activated. Please verify your email first."));
    }

    @Test
    @DisplayName("POST /api/v1/login - Invalid Credentials")
    void testLogin_InvalidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("wrongpassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid username/email or password"));

        // When & Then
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/login - User Not Found")
    void testLogin_UserNotFound() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("nonexistent");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid username/email or password"));

        // When & Then
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/login - Missing Username/Email")
    void testLogin_MissingUsernameOrEmail() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/login - Missing Password")
    void testLogin_MissingPassword() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("");

        // When & Then
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/login - Invalid JSON")
    void testLogin_InvalidJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/login - Wrong Content Type")
    void testLogin_WrongContentType() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");

        // When & Then
        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.TEXT_PLAIN)
                .content("test"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
