package com.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestMailController {
    private static final Logger logger = LoggerFactory.getLogger(TestMailController.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.email.from:noreply@authservice.com}")
    private String fromEmail;

    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestParam(required = false) String to) {
        Map<String, String> response = new HashMap<>();
        
        try {
            String recipientEmail = (to != null && !to.trim().isEmpty()) 
                    ? to.trim() 
                    : "userauth121@gmail.com";
            
            logger.info("Sending test email to: {}", recipientEmail);
            
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(recipientEmail);
            msg.setSubject("SMTP Test - User Authentication Service");
            msg.setText("Gmail SMTP is working! This is a test email from the User Authentication Service.");
            
            mailSender.send(msg);
            
            logger.info("Test email sent successfully to: {}", recipientEmail);
            
            response.put("status", "success");
            response.put("message", "Test email sent successfully to " + recipientEmail);
            response.put("recipient", recipientEmail);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to send test email: {}", e.getMessage(), e);
            
            response.put("status", "error");
            response.put("message", "Failed to send test email: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/email")
    public ResponseEntity<Map<String, String>> sendTestEmailGet(@RequestParam(required = false) String to) {
        return sendTestEmail(to);
    }

    @PostMapping("/email/simple")
    public ResponseEntity<Map<String, String>> sendSimpleTest() {
        Map<String, String> response = new HashMap<>();
        
        try {
            logger.info("Sending simple test email");
            
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo("userauth121@gmail.com");
            msg.setSubject("SMTP Test");
            msg.setText("Gmail SMTP is working");
            
            mailSender.send(msg);
            
            logger.info("Simple test email sent successfully");
            
            response.put("status", "success");
            response.put("message", "Test email sent successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to send simple test email: {}", e.getMessage(), e);
            
            response.put("status", "error");
            response.put("message", "Failed to send test email: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}

