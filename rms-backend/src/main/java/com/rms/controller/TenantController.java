package com.rms.controller;

import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.Lease;
import com.rms.entity.User;
import com.rms.repository.UserRepository;
import com.rms.service.InvoiceService;
import com.rms.service.LeaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private final LeaseService leaseService;
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;

    public TenantController(LeaseService leaseService,
                            InvoiceService invoiceService,
                            UserRepository userRepository) {
        this.leaseService = leaseService;
        this.invoiceService = invoiceService;
        this.userRepository = userRepository;
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
}