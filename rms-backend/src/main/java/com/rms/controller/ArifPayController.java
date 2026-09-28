package com.rms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.rms.service.ArifPayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/arifpay")
public class ArifPayController {

    private final ArifPayService arifPayService;

    public ArifPayController(ArifPayService arifPayService) {
        this.arifPayService = arifPayService;
    }

    public record InitializeRequest(Long invoiceId, String phoneNumber) {}

    /** Tenant starts a payment. Returns { sessionId, paymentUrl }. */
    @PostMapping("/initialize")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<?> initialize(@RequestBody InitializeRequest req) {
        try {
            return ResponseEntity.ok(arifPayService.initialize(req.invoiceId(), req.phoneNumber()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Callback page confirms the payment after the ArifPay redirect. */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('TENANT','ADMIN')")
    public ResponseEntity<?> verify(@RequestParam String sessionId) {
        try {
            return ResponseEntity.ok(arifPayService.verify(sessionId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** ArifPay server to server callback. Public, and always answers 200 so ArifPay stops retrying. */
    @PostMapping("/notify")
    public ResponseEntity<Void> notifyPayment(@RequestBody JsonNode payload) {
        arifPayService.handleNotify(payload);
        return ResponseEntity.ok().build();
    }
}