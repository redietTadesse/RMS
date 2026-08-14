package com.rms.controller;

import com.rms.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final EmailService emailService;

    // ✅ Constructor injection
    public TestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/email-test")
    public String testEmail() {
        try {
            emailService.sendTestEmail();
            return "✅ Test email sent successfully! Check your inbox.";
        } catch (Exception e) {
            return "❌ Email failed: " + e.getMessage();
        }
    }
}