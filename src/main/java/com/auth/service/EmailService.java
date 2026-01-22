package com.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@authservice.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String username, String token) {
        if (!emailEnabled) {
            logger.warn("Email service is disabled. Skipping email send to: {}", toEmail);
            logger.info("Verification token for {}: {}", username, token);
            return;
        }

        logger.info("Sending verification email to: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Verify Your Account - User Authentication Service");
            
            String verificationUrl = baseUrl + "/api/v1/verify?token=" + token;
            String emailBody = String.format(
                "Hello %s,\n\n" +
                "Thank you for signing up! Please verify your email address by clicking the link below:\n\n" +
                "%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not create an account, please ignore this email.\n\n" +
                "Best regards,\n" +
                "User Authentication Service",
                username, verificationUrl
            );
            
            message.setText(emailBody);
            mailSender.send(message);
            logger.info("Verification email sent successfully to: {}", toEmail);
        } catch (MailAuthenticationException e) {
            logger.error("Email authentication failed for {}: {}. Please check email configuration (username/password).", 
                    toEmail, e.getMessage());
            logger.warn("Signup will continue, but email was not sent. Token: {}", token);
            // Don't throw exception - allow signup to succeed
        } catch (MailException e) {
            logger.error("Failed to send verification email to {}: {}. Error: {}", 
                    toEmail, e.getMessage(), e.getClass().getSimpleName());
            logger.warn("Signup will continue, but email was not sent. Token: {}", token);
            // Don't throw exception - allow signup to succeed
        } catch (Exception e) {
            logger.error("Unexpected error sending verification email to {}: {}", toEmail, e.getMessage(), e);
            logger.warn("Signup will continue, but email was not sent. Token: {}", token);
            // Don't throw exception - allow signup to succeed
        }
    }
}

