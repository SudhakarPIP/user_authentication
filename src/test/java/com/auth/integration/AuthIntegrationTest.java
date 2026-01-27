package com.auth.integration;

import com.auth.dto.AuthResponse;
import com.auth.dto.LoginRequest;
import com.auth.dto.SignupRequest;
import com.auth.entity.User;
import com.auth.entity.VerificationToken;
import com.auth.repository.UserRepository;
import com.auth.repository.VerificationTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tokenRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Flow: Signup -> Verify -> Login")
    void testSignupAndVerifyAndLogin_CompleteFlow() throws Exception {
        // Step 1: Signup
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("integrationtest");
        signupRequest.setName("Integration Test");
        signupRequest.setEmail("integration@test.com");
        signupRequest.setMobile("1234567890");
        signupRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integrationtest"))
                .andExpect(jsonPath("$.email").value("integration@test.com"));

        // Step 2: Get verification token from database
        User user = userRepository.findByUsernameIgnoreCase("integrationtest").orElseThrow();
        VerificationToken token = tokenRepository.findAll().stream()
                .filter(t -> !t.getUsed() && t.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        // Step 3: Verify email
        mockMvc.perform(get("/api/v1/verify")
                .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 4: Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("integrationtest");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("integrationtest"));
    }

    @Test
    @DisplayName("Signup - Duplicate Username (Case Insensitive)")
    void testSignup_DuplicateUsername_CaseInsensitive() throws Exception {
        // Create first user
        User existingUser = User.builder()
                .username("duplicate")
                .name("Existing User")
                .email("existing@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("password"))
                .enabled(false)
                .build();
        userRepository.save(existingUser);

        // Try to signup with same username (different case)
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("DUPLICATE");
        signupRequest.setName("New User");
        signupRequest.setEmail("new@test.com");
        signupRequest.setMobile("0987654321");
        signupRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Signup - Duplicate Email (Case Insensitive)")
    void testSignup_DuplicateEmail_CaseInsensitive() throws Exception {
        // Create first user
        User existingUser = User.builder()
                .username("user1")
                .name("Existing User")
                .email("existing@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("password"))
                .enabled(false)
                .build();
        userRepository.save(existingUser);

        // Try to signup with same email (different case)
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("user2");
        signupRequest.setName("New User");
        signupRequest.setEmail("EXISTING@TEST.COM");
        signupRequest.setMobile("0987654321");
        signupRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Login - Account Not Activated")
    void testLogin_AccountNotActivated() throws Exception {
        // Create user without activation
        User user = User.builder()
                .username("notactivated")
                .name("Not Activated")
                .email("notactivated@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("password123"))
                .enabled(false)
                .build();
        userRepository.save(user);

        // Try to login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("notactivated");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account not activated. Please verify your email first."));
    }

    @Test
    @DisplayName("Login - Wrong Password")
    void testLogin_WrongPassword() throws Exception {
        // Create activated user
        User user = User.builder()
                .username("activateduser")
                .name("Activated User")
                .email("activated@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("correctpassword"))
                .enabled(true)
                .build();
        userRepository.save(user);

        // Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("activateduser");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Login - User Not Found")
    void testLogin_UserNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("nonexistent");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Login - Success with Email")
    void testLogin_SuccessWithEmail() throws Exception {
        // Create activated user
        User user = User.builder()
                .username("emailuser")
                .name("Email User")
                .email("emailuser@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("password123"))
                .enabled(true)
                .build();
        userRepository.save(user);

        // Login with email
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("emailuser@test.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("emailuser"));
    }

    @Test
    @DisplayName("Verify Email - Expired Token")
    void testVerifyEmail_ExpiredToken() throws Exception {
        // Create user
        User user = User.builder()
                .username("expiredtest")
                .name("Expired Test")
                .email("expired@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("password"))
                .enabled(false)
                .build();
        userRepository.save(user);

        // Create expired token
        VerificationToken expiredToken = VerificationToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();
        tokenRepository.save(expiredToken);

        // Try to verify with expired token
        mockMvc.perform(get("/api/v1/verify")
                .param("token", expiredToken.getToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expired")));
    }

    @Test
    @DisplayName("Verify Email - Invalid Token")
    void testVerifyEmail_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/verify")
                .param("token", "invalid-token-12345"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Verify Email - Missing Token Parameter")
    void testVerifyEmail_MissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/verify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Verification token is required"));
    }

    @Test
    @DisplayName("Verify Email - Already Used Token")
    void testVerifyEmail_AlreadyUsedToken() throws Exception {
        // Create user
        User user = User.builder()
                .username("usedtoken")
                .name("Used Token")
                .email("usedtoken@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("password"))
                .enabled(true)
                .build();
        userRepository.save(user);

        // Create used token
        VerificationToken usedToken = VerificationToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(true)
                .build();
        tokenRepository.save(usedToken);

        // Try to verify with already used token
        mockMvc.perform(get("/api/v1/verify")
                .param("token", usedToken.getToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Verify Email - User Already Enabled")
    void testVerifyEmail_UserAlreadyEnabled() throws Exception {
        // Create enabled user
        User user = User.builder()
                .username("alreadyenabled")
                .name("Already Enabled")
                .email("alreadyenabled@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("password"))
                .enabled(true)
                .build();
        userRepository.save(user);

        // Create unused token
        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        tokenRepository.save(token);

        // Try to verify (should return success message about already verified)
        mockMvc.perform(get("/api/v1/verify")
                .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already verified")));
    }

    @Test
    @DisplayName("Signup - Validation: Invalid Email Format")
    void testSignup_InvalidEmailFormat() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setName("Test User");
        signupRequest.setEmail("invalid-email-format");
        signupRequest.setMobile("1234567890");
        signupRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Signup - Validation: Invalid Mobile Format")
    void testSignup_InvalidMobileFormat() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setName("Test User");
        signupRequest.setEmail("test@example.com");
        signupRequest.setMobile("abc123"); // Invalid format
        signupRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Signup - Validation: Short Password")
    void testSignup_ShortPassword() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setName("Test User");
        signupRequest.setEmail("test@example.com");
        signupRequest.setMobile("1234567890");
        signupRequest.setPassword("short"); // Too short

        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Signup - Validation: Short Username")
    void testSignup_ShortUsername() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("ab"); // Too short
        signupRequest.setName("Test User");
        signupRequest.setEmail("test@example.com");
        signupRequest.setMobile("1234567890");
        signupRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login - Case Insensitive Username")
    void testLogin_CaseInsensitiveUsername() throws Exception {
        // Create activated user
        User user = User.builder()
                .username("caseuser")
                .name("Case User")
                .email("caseuser@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("password123"))
                .enabled(true)
                .build();
        userRepository.save(user);

        // Login with different case
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("CASEUSER");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Login - Case Insensitive Email")
    void testLogin_CaseInsensitiveEmail() throws Exception {
        // Create activated user
        User user = User.builder()
                .username("caseemail")
                .name("Case Email")
                .email("caseemail@test.com")
                .mobile("1234567890")
                .passwordHash(passwordEncoder.encode("password123"))
                .enabled(true)
                .build();
        userRepository.save(user);

        // Login with different case email
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("CASEEMAIL@TEST.COM");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Login - Missing Required Fields")
    void testLogin_MissingRequiredFields() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        // All fields are null

        mockMvc.perform(post("/api/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }
}
