package com.auth.service;

import com.auth.dto.AuthResponse;
import com.auth.dto.LoginRequest;
import com.auth.dto.SignupRequest;
import com.auth.entity.User;
import com.auth.entity.VerificationToken;
import com.auth.repository.UserRepository;
import com.auth.repository.VerificationTokenRepository;
import com.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private User user;
    private VerificationToken verificationToken;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setName("Test User");
        signupRequest.setEmail("test@example.com");
        signupRequest.setMobile("1234567890");
        signupRequest.setPassword("password123");

        user = User.builder()
                .id(1L)
                .username("testuser")
                .name("Test User")
                .email("test@example.com")
                .mobile("1234567890")
                .passwordHash("$2a$10$encoded")
                .enabled(false)
                .build();

        verificationToken = VerificationToken.builder()
                .id(1L)
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
    }

    @Test
    @DisplayName("Signup - Success")
    void testSignup_Success() {
        // Given
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // When
        AuthResponse response = authService.signup(signupRequest);

        // Then
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getMessage().contains("successful"));
        
        verify(userRepository, times(1)).existsByUsernameIgnoreCase(anyString());
        verify(userRepository, times(1)).existsByEmailIgnoreCase(anyString());
        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Signup - Username Already Exists")
    void testSignup_UsernameAlreadyExists() {
        // Given
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.signup(signupRequest));
        assertEquals("Username already exists. Please choose a different username.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(tokenRepository, never()).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("Signup - Email Already Exists")
    void testSignup_EmailAlreadyExists() {
        // Given
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.signup(signupRequest));
        assertEquals("Email already exists. Please use a different email address.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Signup - Null Username")
    void testSignup_NullUsername() {
        // Given
        signupRequest.setUsername(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.signup(signupRequest));
        assertEquals("Username cannot be null or empty", exception.getMessage());
        verify(userRepository, never()).existsByUsernameIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Signup - Empty Username")
    void testSignup_EmptyUsername() {
        // Given
        signupRequest.setUsername("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.signup(signupRequest));
        assertEquals("Username cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Signup - Null Email")
    void testSignup_NullEmail() {
        // Given
        signupRequest.setEmail(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.signup(signupRequest));
        assertEquals("Email cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Signup - Email Service Failure (Non-blocking)")
    void testSignup_EmailServiceFailure() {
        // Given
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);
        doThrow(new RuntimeException("SMTP error")).when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        // When - Email service now catches exceptions, so signup should succeed
        AuthResponse response = authService.signup(signupRequest);

        // Then - Signup succeeds even if email fails
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getMessage().contains("successful"));
        
        // Verify email service was called (even though it failed)
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Verify Email - Success")
    void testVerifyEmail_Success() {
        // Given
        String token = verificationToken.getToken();
        when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(verificationToken));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

        // When
        AuthResponse response = authService.verifyEmail(token);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("verified"));
        assertTrue(user.getEnabled());
        assertTrue(verificationToken.getUsed());
        
        verify(tokenRepository, times(1)).findByTokenAndUsedFalse(token);
        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenRepository, times(1)).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("Verify Email - Token Not Found")
    void testVerifyEmail_TokenNotFound() {
        // Given
        String token = "invalid-token";
        when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.verifyEmail(token));
        assertTrue(exception.getMessage().contains("Invalid or expired verification token"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Verify Email - Null Token")
    void testVerifyEmail_NullToken() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.verifyEmail(null));
        assertEquals("Verification token is required", exception.getMessage());
        verify(tokenRepository, never()).findByTokenAndUsedFalse(anyString());
    }

    @Test
    @DisplayName("Verify Email - Empty Token")
    void testVerifyEmail_EmptyToken() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.verifyEmail("   "));
        assertEquals("Verification token is required", exception.getMessage());
    }

    @Test
    @DisplayName("Verify Email - Expired Token")
    void testVerifyEmail_ExpiredToken() {
        // Given
        String token = verificationToken.getToken();
        verificationToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(verificationToken));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.verifyEmail(token));
        assertTrue(exception.getMessage().contains("expired"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Verify Email - Already Used Token")
    void testVerifyEmail_AlreadyUsedToken() {
        // Given
        String token = verificationToken.getToken();
        verificationToken.setUsed(true);
        when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.verifyEmail(token));
        assertTrue(exception.getMessage().contains("Invalid or expired verification token"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Verify Email - User Already Enabled")
    void testVerifyEmail_UserAlreadyEnabled() {
        // Given
        String token = verificationToken.getToken();
        user.setEnabled(true);
        when(tokenRepository.findByTokenAndUsedFalse(token)).thenReturn(Optional.of(verificationToken));
        when(tokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

        // When
        AuthResponse response = authService.verifyEmail(token);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("already verified"));
        assertTrue(verificationToken.getUsed());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login - Success")
    void testLogin_Success() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        user.setEnabled(true);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken("testuser", 1L)).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertTrue(response.getMessage().contains("successful"));
        
        verify(userRepository, times(1)).findByUsernameIgnoreCase("testuser");
        verify(passwordEncoder, times(1)).matches("password123", user.getPasswordHash());
        verify(jwtUtil, times(1)).generateToken("testuser", 1L);
    }

    @Test
    @DisplayName("Login - Success with Email")
    void testLogin_SuccessWithEmail() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("test@example.com");
        loginRequest.setPassword("password123");

        user.setEnabled(true);
        when(userRepository.findByUsernameIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken("testuser", 1L)).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        verify(userRepository, times(1)).findByEmailIgnoreCase("test@example.com");
    }

    @Test
    @DisplayName("Login - Account Not Activated")
    void testLogin_AccountNotActivated() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        user.setEnabled(false);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
                () -> authService.login(loginRequest));
        assertEquals("Account not activated. Please verify your email first.", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("Login - Invalid Password")
    void testLogin_InvalidPassword() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("wrongpassword");

        user.setEnabled(true);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", user.getPasswordHash())).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.login(loginRequest));
        assertEquals("Invalid username/email or password", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("Login - User Not Found")
    void testLogin_UserNotFound() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("nonexistent");
        loginRequest.setPassword("password123");

        when(userRepository.findByUsernameIgnoreCase("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.login(loginRequest));
        assertEquals("Invalid username/email or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Login - Null Username/Email")
    void testLogin_NullUsernameOrEmail() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(null);
        loginRequest.setPassword("password123");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.login(loginRequest));
        assertEquals("Username or email is required", exception.getMessage());
        verify(userRepository, never()).findByUsernameIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Login - Empty Username/Email")
    void testLogin_EmptyUsernameOrEmail() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("   ");
        loginRequest.setPassword("password123");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.login(loginRequest));
        assertEquals("Username or email is required", exception.getMessage());
    }

    @Test
    @DisplayName("Login - Null Password")
    void testLogin_NullPassword() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.login(loginRequest));
        assertEquals("Password is required", exception.getMessage());
        verify(userRepository, never()).findByUsernameIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Login - Empty Password")
    void testLogin_EmptyPassword() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> authService.login(loginRequest));
        assertEquals("Password is required", exception.getMessage());
    }

    @Test
    @DisplayName("Login - Case Insensitive Username")
    void testLogin_CaseInsensitiveUsername() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("TESTUSER");
        loginRequest.setPassword("password123");

        user.setEnabled(true);
        when(userRepository.findByUsernameIgnoreCase("TESTUSER")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken("testuser", 1L)).thenReturn("jwt-token");

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
    }
}
