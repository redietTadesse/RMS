package com.rms.controller;

import com.rms.dto.InvoiceResponse;
import com.rms.dto.PaymentRequest;
import com.rms.entity.Lease;
import com.rms.service.EmailService;
import com.rms.service.InvoiceService;
import com.rms.service.LeaseService;
import com.rms.service.TimeTravelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    private final EmailService emailService;
    private final TimeTravelService timeTravelService;
    private final InvoiceService invoiceService;
    private final LeaseService leaseService;

    public TestController(EmailService emailService,
                          TimeTravelService timeTravelService,
                          InvoiceService invoiceService,
                          LeaseService leaseService) {
        this.emailService = emailService;
        this.timeTravelService = timeTravelService;
        this.invoiceService = invoiceService;
        this.leaseService = leaseService;
    }

    @GetMapping("/email-test")
    @PreAuthorize("hasRole('ADMIN')")
    public String testEmail() {
        try {
            emailService.sendTestEmail();
            return "✅ Test email sent successfully! Check your inbox.";
        } catch (Exception e) {
            return "❌ Email failed: " + e.getMessage();
        }
    }

    // ─── TIME TRAVEL ENDPOINTS ─────────────────────────────────────────

    @GetMapping("/time/current")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCurrentTime() {
        LocalDate date = timeTravelService.getCurrentDate();
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", date.toString());
        map.put("isVirtual", timeTravelService.isActive());
        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/set-date")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setCurrentDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        timeTravelService.setFixedDate(date);
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", date.toString());
        map.put("isVirtual", true);
        map.put("message", "Virtual time set to " + date);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/advance-days")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> advanceDays(@RequestParam int days) {
        LocalDate result = timeTravelService.advanceDays(days);
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", result.toString());
        map.put("isVirtual", true);
        map.put("message", "Advanced " + days + " day(s) to " + result);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/advance-months")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> advanceMonths(@RequestParam int months) {
        LocalDate result = timeTravelService.advanceMonths(months);
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", result.toString());
        map.put("isVirtual", true);
        map.put("message", "Advanced " + months + " month(s) to " + result);
        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/run-scheduled-jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> runScheduledJobs() {
        LocalDate date = timeTravelService.getCurrentDate();
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", date.toString());
        map.put("isVirtual", timeTravelService.isActive());

        // Set TimeTravelService on DTOs so they use virtual time
        InvoiceResponse.setTimeTravelService(timeTravelService);
        PaymentRequest.setTimeTravelService(timeTravelService);
        Lease.setTimeTravelService(timeTravelService);

        try {
            invoiceService.generateMonthlyInvoices();
            map.put("invoicesGenerated", true);
        } catch (Exception e) {
            map.put("invoicesGenerated", false);
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            map.put("invoiceError", root.toString());
            log.error("Scheduled invoice generation failed", e);
        }

        try {
            invoiceService.calculateDailyPenalties();
            map.put("penaltiesCalculated", true);
        } catch (Exception e) {
            map.put("penaltiesCalculated", false);
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            map.put("penaltyError", root.toString());
            log.error("Scheduled penalty calculation failed", e);
        }

        try {
            int expired = leaseService.syncExpiredLeases();
            map.put("leasesExpired", expired);
        } catch (Exception e) {
            map.put("leasesExpired", -1);
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            map.put("leaseError", root.toString());
            log.error("Scheduled lease expiry sync failed", e);
        }

        return ResponseEntity.ok(map);
    }

    @PostMapping("/time/reset")
    @PreAuthorize("hasRole('ADMIN')")
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