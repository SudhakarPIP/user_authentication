package com.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@authservice.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);  // Enable email for tests
    }

    @Test
    void testSendVerificationEmail_Success() {
        // Given
        String toEmail = "sudhakar.reddy@harman.com";
        String username = "userauthtest";
        String token = "rewylyqamrdllsht";
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendVerificationEmail(toEmail, username, token);

        // Then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendVerificationEmail_Failure() {
        // Given
        String toEmail = "userauth121@gmail.com";
        String username = "userauthtest";
        String token = "rewylyqamrdllsht";
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(SimpleMailMessage.class));

        // When - Email service now catches exceptions and doesn't throw
        emailService.sendVerificationEmail(toEmail, username, token);

        // Then - Verify send was attempted (even though it failed)
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendVerificationEmail_WhenEmailDisabled() {
        // Given
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        String toEmail = "userauth121@gmail.com";
        String username = "userauthtest";
        String token = "rewylyqamrdllsht";

        // When
        emailService.sendVerificationEmail(toEmail, username, token);

        // Then - Email should not be sent when disabled
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendVerificationEmail_AuthenticationFailure() {
        // Given
        String toEmail = "userauth121@gmail.com";
        String username = "userauthtest";
        String token = "rewylyqamrdllsht";
        org.springframework.mail.MailAuthenticationException authException = 
            new org.springframework.mail.MailAuthenticationException("Authentication failed");
        doThrow(authException).when(mailSender).send(any(SimpleMailMessage.class));

        // When - Email service now catches exceptions and doesn't throw
        emailService.sendVerificationEmail(toEmail, username, token);

        // Then - Verify send was attempted (even though it failed)
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}

