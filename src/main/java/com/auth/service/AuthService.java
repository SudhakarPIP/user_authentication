package com.auth.service;

import com.auth.dto.AuthResponse;
import com.auth.dto.LoginRequest;
import com.auth.dto.SignupRequest;
import com.auth.entity.User;
import com.auth.entity.VerificationToken;
import com.auth.repository.UserRepository;
import com.auth.repository.VerificationTokenRepository;
import com.auth.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final int TOKEN_EXPIRATION_HOURS = 24;

    public AuthService(UserRepository userRepository,
                      VerificationTokenRepository tokenRepository,
                      EmailService emailService,
                      BCryptPasswordEncoder passwordEncoder,
                      JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        logger.info("Processing signup request for username: {}, email: {}", 
                request.getUsername(), request.getEmail());

        // Check username uniqueness (case-insensitive)
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername().trim())) {
            logger.warn("Signup failed: Username already exists (case-insensitive) - {}", 
                    request.getUsername());
            throw new IllegalArgumentException("Username already exists. Please choose a different username.");
        }

        // Check email uniqueness (case-insensitive)
        if (userRepository.existsByEmailIgnoreCase(request.getEmail().trim().toLowerCase())) {
            logger.warn("Signup failed: Email already exists (case-insensitive) - {}", 
                    request.getEmail());
            throw new IllegalArgumentException("Email already exists. Please use a different email address.");
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername().trim())
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .mobile(request.getMobile().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(false)
                .build();

        user = userRepository.save(user);
        logger.info("User created successfully with ID: {}, username: {}", 
                user.getId(), user.getUsername());

        // Generate verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS))
                .used(false)
                .build();

        tokenRepository.save(verificationToken);
        logger.info("Verification token created for user ID: {}, expires at: {}", 
                user.getId(), verificationToken.getExpiresAt());

        // Send verification email (non-blocking - signup succeeds even if email fails)
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);
        } catch (Exception e) {
            logger.error("Email sending failed for user ID: {}, but signup will continue. Token: {}", 
                    user.getId(), token, e);
            // Continue with signup even if email fails
        }

        logger.info("Signup completed successfully for user: {}, email: {}", 
                user.getUsername(), user.getEmail());
        return AuthResponse.builder()
                .message("Signup successful. Please check your email to verify your account.")
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        logger.info("Processing email verification request for token");

        Optional<VerificationToken> tokenOpt = tokenRepository.findByTokenAndUsedFalse(token.trim());
        
        if (tokenOpt.isEmpty()) {
            logger.warn("Verification failed: Token not found or already used");
            throw new IllegalArgumentException("Invalid or expired verification token. Please request a new verification email.");
        }

        VerificationToken verificationToken = tokenOpt.get();
        User user = verificationToken.getUser();

        // Check if token is expired
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Verification failed: Token expired for user ID: {}, expired at: {}", 
                    user.getId(), verificationToken.getExpiresAt());
            throw new IllegalArgumentException("Verification token has expired. Please request a new verification email.");
        }

        // Check if user is already enabled
        if (user.getEnabled()) {
            logger.info("User account already enabled for user ID: {}", user.getId());
            verificationToken.setUsed(true);
            tokenRepository.save(verificationToken);
            return AuthResponse.builder()
                    .message("Your account is already verified and activated.")
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();
        }

        // Enable user account
        user.setEnabled(true);
        userRepository.save(user);
        logger.info("User account enabled successfully for user ID: {}, username: {}", 
                user.getId(), user.getUsername());

        // Mark token as used
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        logger.info("Email verification completed successfully for user: {}, email: {}", 
                user.getUsername(), user.getEmail());
        return AuthResponse.builder()
                .message("Email verified successfully. Your account is now activated.")
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        logger.info("Processing login request for: {}", request.getUsernameOrEmail());

        // Find user by username or email (case-insensitive)
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(request.getUsernameOrEmail().trim());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmailIgnoreCase(request.getUsernameOrEmail().trim());
        }

        if (userOpt.isEmpty()) {
            logger.warn("Login failed: User not found - {}", request.getUsernameOrEmail());
            throw new IllegalArgumentException("Invalid username/email or password");
        }

        User user = userOpt.get();

        // Check if account is enabled
        if (!user.getEnabled()) {
            logger.warn("Login failed: Account not activated for user ID: {}, username: {}", 
                    user.getId(), user.getUsername());
            throw new IllegalStateException("Account not activated. Please verify your email first.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Login failed: Invalid password for user ID: {}, username: {}", 
                    user.getId(), user.getUsername());
            throw new IllegalArgumentException("Invalid username/email or password");
        }

        // Generate JWT token
        String jwtToken = jwtUtil.generateToken(user.getUsername(), user.getId());
        logger.info("Login successful for user ID: {}, username: {}, email: {}", 
                user.getId(), user.getUsername(), user.getEmail());

        return AuthResponse.builder()
                .token(jwtToken)
                .message("Login successful")
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}

