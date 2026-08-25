package com.rms.controller;

import com.rms.dto.PaymentRequest;
import com.rms.entity.Invoice;
import com.rms.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
    @ResponseStatus(HttpStatus.OK)
    public Invoice recordPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestPart("payment") PaymentRequest request,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt) {
        return paymentService.recordPayment(invoiceId, request, receipt);
    }

    @PutMapping("/{invoiceId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Invoice approvePayment(@PathVariable Long invoiceId) {
        return paymentService.approvePayment(invoiceId);
    }

    @GetMapping("/preview")
    public ResponseEntity<BigDecimal> previewPenalty(
            @RequestParam Long invoiceId,
            @RequestParam LocalDate paymentDate) {
        BigDecimal penalty = paymentService.calculatePenaltyPreview(invoiceId, paymentDate);
        return ResponseEntity.ok(penalty);
    }
}