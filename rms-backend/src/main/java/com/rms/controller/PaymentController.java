package com.rms.controller;

import com.rms.dto.PaymentRequest;
import com.rms.entity.Invoice;
import com.rms.repository.InvoiceRepository;
import com.rms.service.CurrentUserService;
import com.rms.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;
    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService,
                             InvoiceRepository invoiceRepository,
                             CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.invoiceRepository = invoiceRepository;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
    @ResponseStatus(HttpStatus.OK)
    public Invoice recordPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestPart("payment") PaymentRequest request,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt,
            Authentication authentication) {
        requireInvoiceOwnership(invoiceId, authentication);
        return paymentService.recordPayment(invoiceId, request, receipt);
    }

    /** Tenants may only submit payments for their own invoices; admins may pay any. */
    private void requireInvoiceOwnership(Long invoiceId, Authentication authentication) {
        if (currentUserService.isAdmin()) return;
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));
        currentUserService.requireOwnership(invoice);
    }

    @PutMapping("/{invoiceId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Invoice approvePayment(@PathVariable Long invoiceId) {
        return paymentService.approvePayment(invoiceId);
    }

    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
    public ResponseEntity<BigDecimal> previewPenalty(
            @RequestParam Long invoiceId,
            @RequestParam LocalDate paymentDate,
            Authentication authentication) {
        requireInvoiceOwnership(invoiceId, authentication);
        BigDecimal penalty = paymentService.calculatePenaltyPreview(invoiceId, paymentDate);
        return ResponseEntity.ok(penalty);
    }
}