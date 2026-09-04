package com.rms.controller;

import com.rms.entity.Invoice;
import com.rms.entity.Lease;
import com.rms.entity.Space;
import com.rms.entity.Tenant;
import com.rms.service.EmailService;
import com.rms.service.InvoiceService;
import com.rms.service.LeaseService;
import com.rms.service.SmsService;
import com.rms.service.TimeTravelService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final EmailService emailService;
    private final SmsService smsService;
    private final TimeTravelService timeTravelService;
    private final InvoiceService invoiceService;
    private final LeaseService leaseService;

    public TestController(EmailService emailService,
                          SmsService smsService,
                          TimeTravelService timeTravelService,
                          InvoiceService invoiceService,
                          LeaseService leaseService) {
        this.emailService = emailService;
        this.smsService = smsService;
        this.timeTravelService = timeTravelService;
        this.invoiceService = invoiceService;
        this.leaseService = leaseService;
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
            testInvoice.setRentAmount(new java.math.BigDecimal("60000"));
            testInvoice.setTotalAmount(new java.math.BigDecimal("60000"));
            LocalDate now = timeTravelService.getCurrentDate();
            testInvoice.setDueDate(now.plusDays(5));

            // Test rent reminder SMS
            smsService.sendRentReminderSms(testTenant, testInvoice, 5);

            // Test overdue SMS
            testInvoice.setDueDate(now.minusDays(2));
            smsService.sendOverdueSms(testTenant, testInvoice, 2, 400.0);

            String mode = smsService.isMockMode() ? "MOCK" : "REAL";
            return "✅ SMS test completed in " + mode + " mode. Check console logs for SMS content.";
        } catch (Exception e) {
            return "❌ SMS test failed: " + e.getMessage();
                }
    }

    // ─── TIME TRAVEL ENDPOINTS ─────────────────────────────────────────

    @GetMapping("/time/current")
    public ResponseEntity<?> getCurrentTime() {
        LocalDate date = timeTravelService.getCurrentDate();
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", date.toString());
        map.put("isVirtual", timeTravelService.isActive());
        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/set-date")
    public ResponseEntity<?> setCurrentDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        timeTravelService.setFixedDate(date);
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", date.toString());
        map.put("isVirtual", true);
        map.put("message", "Virtual time set to " + date);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/advance-days")
    public ResponseEntity<?> advanceDays(@RequestParam int days) {
        LocalDate result = timeTravelService.advanceDays(days);
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", result.toString());
        map.put("isVirtual", true);
        map.put("message", "Advanced " + days + " day(s) to " + result);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/advance-months")
    public ResponseEntity<?> advanceMonths(@RequestParam int months) {
        LocalDate result = timeTravelService.advanceMonths(months);
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", result.toString());
        map.put("isVirtual", true);
        map.put("message", "Advanced " + months + " month(s) to " + result);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/run-scheduled-jobs")
    public ResponseEntity<?> runScheduledJobs() {
        LocalDate date = timeTravelService.getCurrentDate();
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", date.toString());
        map.put("isVirtual", timeTravelService.isActive());

        try {
            invoiceService.generateMonthlyInvoices();
            map.put("invoicesGenerated", true);
        } catch (Exception e) {
            map.put("invoicesGenerated", false);
            map.put("invoiceError", e.getMessage());
        }

        try {
            invoiceService.calculateDailyPenalties();
            map.put("penaltiesCalculated", true);
        } catch (Exception e) {
            map.put("penaltiesCalculated", false);
            map.put("penaltyError", e.getMessage());
        }

        try {
            int expired = leaseService.syncExpiredLeases();
            map.put("leasesExpired", expired);
        } catch (Exception e) {
            map.put("leasesExpired", -1);
            map.put("leaseError", e.getMessage());
        }

        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/reset")
    public ResponseEntity<?> resetTime() {
        timeTravelService.resetTime();
        LocalDate date = timeTravelService.getCurrentDate();
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", date.toString());
        map.put("isVirtual", false);
        map.put("message", "Time reset to real time");
        return ResponseEntity.ok(map);
    }
}