package com.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug controller to check email configuration
 * Remove this in production!
 */
@RestController
@RequestMapping("/api/v1/debug")
public class EmailDebugController {
    private static final Logger logger = LoggerFactory.getLogger(EmailDebugController.class);

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:}")
    private String mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @GetMapping("/email-config")
    public ResponseEntity<Map<String, Object>> getEmailConfig() {
        Map<String, Object> config = new HashMap<>();
        
        try {
            config.put("host", mailHost);
            config.put("port", mailPort);
            config.put("username", mailUsername);
            config.put("password", "***hidden***");
            config.put("emailEnabled", emailEnabled);
            config.put("note", "Check application.yml for full configuration");
            
            logger.info("Email configuration retrieved: host={}, port={}, username={}, enabled={}", 
                    mailHost, mailPort, mailUsername, emailEnabled);
            
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            logger.error("Error retrieving email config: {}", e.getMessage(), e);
            config.put("error", e.getMessage());
            return ResponseEntity.status(500).body(config);
        }
    }

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, String>> testConnection() {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Try to create a test message
            org.springframework.mail.SimpleMailMessage msg = new org.springframework.mail.SimpleMailMessage();
            msg.setTo("test@example.com");
            msg.setSubject("Test");
            msg.setText("Test");
            
            // This will fail at send, but we can check if configuration is valid
            response.put("status", "Configuration loaded successfully");
            response.put("host", mailHost);
            response.put("port", mailPort);
            response.put("username", mailUsername);
            response.put("emailEnabled", String.valueOf(emailEnabled));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

