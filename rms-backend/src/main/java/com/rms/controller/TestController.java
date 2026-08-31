package com.rms.controller;

import com.rms.entity.Invoice;
import com.rms.entity.Lease;
import com.rms.entity.Space;
import com.rms.entity.Tenant;
import com.rms.service.EmailService;
import com.rms.service.SmsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final EmailService emailService;
    private final SmsService smsService;

    // ✅ Constructor injection
    public TestController(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
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

    @GetMapping("/sms-test")
    public String testSms() {
        try {
            // Create test data
            Tenant testTenant = new Tenant();
            testTenant.setPhone("+251911000000");
            testTenant.setName("Test Tenant");

            Space space = new Space();
            space.setUnitNumber("G-01");

            Lease lease = new Lease();
            lease.setSpace(space);

            Invoice testInvoice = new Invoice();
            testInvoice.setInvoiceNumber("TEST-001");
            testInvoice.setLease(lease);
            testInvoice.setRentAmount(new BigDecimal("60000"));
            testInvoice.setTotalAmount(new BigDecimal("60000"));
            testInvoice.setDueDate(LocalDate.now().plusDays(5));

            // Test rent reminder SMS
            smsService.sendRentReminderSms(testTenant, testInvoice, 5);

            // Test overdue SMS
            testInvoice.setDueDate(LocalDate.now().minusDays(2));
            smsService.sendOverdueSms(testTenant, testInvoice, 2, 400.0);

            String mode = smsService.isMockMode() ? "MOCK" : "REAL";
            return "✅ SMS test completed in " + mode + " mode. Check console logs for SMS content.";
        } catch (Exception e) {
            return "❌ SMS test failed: " + e.getMessage();
        }
    }
}