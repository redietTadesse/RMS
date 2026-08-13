package com.rms.controller;

import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.Lease;
import com.rms.entity.User;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.UserRepository;
import com.rms.service.CurrentUserService;
import com.rms.service.InvoiceService;
import com.rms.service.LeaseService;
import com.rms.service.PaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private final LeaseService leaseService;
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;
    private final InvoiceRepository invoiceRepository;

    public TenantController(LeaseService leaseService,
                            InvoiceService invoiceService,
                            UserRepository userRepository,
                            PaymentService paymentService,
                            CurrentUserService currentUserService,
                            InvoiceRepository invoiceRepository) {
        this.leaseService = leaseService;
        this.invoiceService = invoiceService;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/my-lease")
    public ResponseEntity<Lease> getMyLease(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Lease lease = leaseService.getLeaseByTenantUsername(username);
        return ResponseEntity.ok(lease);
    }

    @GetMapping("/my-invoices")
    public ResponseEntity<List<Invoice>> getMyInvoices(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Invoice> invoices = invoiceService.getInvoicesByTenantUsername(username);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Lease lease = leaseService.getLeaseByTenantUsername(username);
        List<Invoice> invoices = invoiceService.getInvoicesByTenantUsername(username);

        Map<String, Object> stats = new HashMap<>();
        stats.put("hasLease", lease != null);
        stats.put("leaseStatus", lease != null ? lease.getStatus().name() : "NONE");
        stats.put("totalInvoices", invoices.size());
        stats.put("pendingInvoices", invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PENDING).count());
        stats.put("paidInvoices", invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count());
        stats.put("overdueInvoices", invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.OVERDUE).count());

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/invoices/{invoiceId}/receipt")
    public ResponseEntity<Invoice> submitReceipt(
            @PathVariable Long invoiceId,
            @RequestParam("paymentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            @RequestParam(value = "paymentMethod", required = false, defaultValue = "receipt") String paymentMethod,
            @RequestPart("receipt") MultipartFile receipt) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        currentUserService.requireOwnership(invoice);

        Invoice updated = paymentService.submitReceipt(invoiceId, paymentDate, paymentMethod, receipt);
        return ResponseEntity.ok(updated);
    }
}