package com.rms.controller;

import com.rms.dto.InvoiceResponse;
import com.rms.dto.PaymentRequest;
import com.rms.entity.Lease;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.repository.InvoiceRepository;
import com.rms.service.EmailService;
import com.rms.service.InvoiceService;
import com.rms.service.LeaseService;
import com.rms.service.TimeTravelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
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
    private final InvoiceRepository invoiceRepository;

    public TestController(EmailService emailService,
                          TimeTravelService timeTravelService,
                          InvoiceService invoiceService,
                          LeaseService leaseService,
                          InvoiceRepository invoiceRepository) {
        this.emailService = emailService;
        this.timeTravelService = timeTravelService;
        this.invoiceService = invoiceService;
        this.leaseService = leaseService;
        this.invoiceRepository = invoiceRepository;
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
    @Transactional
    public ResponseEntity<?> runScheduledJobs(@RequestParam(defaultValue = "true") boolean persist) {
        LocalDate date = timeTravelService.getCurrentDate();
        Map<String, Object> map = new HashMap<>();
        map.put("currentDate", date.toString());
        map.put("isVirtual", timeTravelService.isActive());

        if (!timeTravelService.isActive()) {
            map.put("warning", "Time travel is not active. Set a virtual date before running the demo jobs.");
        }

        // Set TimeTravelService on DTOs so they use virtual time
        InvoiceResponse.setTimeTravelService(timeTravelService);
        PaymentRequest.setTimeTravelService(timeTravelService);
        Lease.setTimeTravelService(timeTravelService);

        try {
            invoiceService.simulateMonthlyInvoices();
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
            invoiceService.simulateDailyPenalties();
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
            int expired = leaseService.simulateExpiredLeases();
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

        try {
            int upcoming = invoiceService.simulateUpcomingReminders();
            map.put("upcomingRemindersSent", upcoming);
        } catch (Exception e) {
            map.put("upcomingRemindersSent", -1);
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            map.put("upcomingReminderError", root.toString());
            log.error("Upcoming reminder job failed", e);
        }

        try {
            int due = invoiceService.simulateDueReminders();
            map.put("dueRemindersSent", due);
        } catch (Exception e) {
            map.put("dueRemindersSent", -1);
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            map.put("dueReminderError", root.toString());
            log.error("Due-today reminder job failed", e);
        }

        List<Map<String, Object>> simulatedOverdue = invoiceRepository
                .findByStatusIn(List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE))
                .stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.OVERDUE
                        || invoice.getDueDate().isBefore(date))
                .map(invoice -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("invoiceNumber", invoice.getInvoiceNumber());
                    item.put("status", invoice.getStatus().name());
                    item.put("dueDate", invoice.getDueDate());
                    item.put("penaltyAmount", invoice.getPenaltyAmount());
                    item.put("daysOverdue", Math.max(0,
                            java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), date)));
                    return item;
                })
                .collect(Collectors.toList());
        BigDecimal simulatedPenaltyTotal = simulatedOverdue.stream()
                .map(item -> (BigDecimal) item.get("penaltyAmount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        map.put("simulatedOverdueInvoices", simulatedOverdue);
        map.put("simulatedOverdueCount", simulatedOverdue.size());
        map.put("simulatedPenaltyTotal", simulatedPenaltyTotal);

        if (persist) {
            // Default mode: commit the jobs' work so the admin can see the
            // generated invoices, penalties, and lease expiries across the app.
            map.put("persisted", true);
            map.put("message", "Scheduled jobs completed; database changes saved");
        } else {
            // Simulation mode: keep the response, but discard all database
            // changes made by invoice, penalty, and lease processing.
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            map.put("persisted", false);
            map.put("message", "Scheduled jobs simulated; database changes rolled back");
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