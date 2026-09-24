package com.rms.controller;

import com.rms.dto.ChapaPaymentRequest;
import com.rms.dto.ChapaPaymentResponse;
import com.rms.entity.Invoice;
import com.rms.service.ChapaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/chapa")
@CrossOrigin(origins = "http://localhost:5173")
public class ChapaController {

    private static final Logger log = LoggerFactory.getLogger(ChapaController.class);

    private final ChapaService chapaService;

    public ChapaController(ChapaService chapaService) {
        this.chapaService = chapaService;
    }

    /**
     * Initialize Chapa payment
     * POST /api/payments/chapa/initialize
     */
    @PostMapping("/initialize")
    public ResponseEntity<ChapaPaymentResponse> initializePayment(
            @Valid @RequestBody ChapaPaymentRequest request) {
        ChapaPaymentResponse response = chapaService.initializePayment(
                request.getInvoiceId(),
                request.getPhoneNumber()  // Only phone number
        );
        return ResponseEntity.ok(response);
    }



    /**
     * Verify Chapa payment
     * GET /api/payments/chapa/verify?tx_ref=xxx
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verifyPayment(
            @RequestParam("tx_ref") String txRef) {
        try {
            boolean verified = chapaService.verifyPayment(txRef);
            return ResponseEntity.ok(Map.of("verified", verified));
        } catch (Exception e) {
            log.error("Verification error", e);
            return ResponseEntity.ok(Map.of("verified", false));
        }
    }
    /**
     * Chapa webhook endpoint
     * POST /api/payments/chapa/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "x-chapa-signature", required = false) String signature) {
        try {
            chapaService.processWebhook(payload, signature);
            return ResponseEntity.ok("Webhook received");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing webhook: " + e.getMessage());
        }
    }

    /**
     * Confirm payment from the frontend callback (fallback to webhook)
     * POST /api/payments/chapa/confirm?tx_ref=xxx
     */
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmPayment(@RequestParam("tx_ref") String txRef) {
        try {
            Invoice invoice = chapaService.confirmPayment(txRef);
            Map<String, Object> result = new HashMap<>();
            result.put("verified", true);
            result.put("invoiceNumber", invoice.getInvoiceNumber());
            result.put("status", invoice.getStatus().toString());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("verified", false, "message", e.getMessage()));
        }
    }
}