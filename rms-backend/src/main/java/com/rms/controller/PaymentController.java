package com.rms.controller;

import com.rms.dto.PaymentRequest;
import com.rms.entity.Invoice;
import com.rms.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * Record a payment for an invoice
     * POST /api/payments/{invoiceId}
     */
    @PostMapping("/{invoiceId}")
    @ResponseStatus(HttpStatus.OK)
    public Invoice recordPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestPart("payment") PaymentRequest request,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt) {
        return paymentService.recordPayment(invoiceId, request, receipt);
    }

    /**
     * Preview penalty amount before recording payment
     * GET /api/payments/preview?invoiceId=1&paymentDate=2026-08-15
     */
    @GetMapping("/preview")
    public ResponseEntity<BigDecimal> previewPenalty(
            @RequestParam Long invoiceId,
            @RequestParam LocalDate paymentDate) {
        BigDecimal penalty = paymentService.calculatePenaltyPreview(invoiceId, paymentDate);
        return ResponseEntity.ok(penalty);
    }
}