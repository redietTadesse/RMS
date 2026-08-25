package com.rms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rms.dto.ChapaPaymentResponse;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.Tenant;
import com.rms.entity.User;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ChapaService {

    private final WebClient webClient;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;

    @Value("${chapa.secret-key}")
    private String secretKey;

    @Value("${chapa.redirect-url}")
    private String redirectUrl;

    @Value("${chapa.webhook-secret}")
    private String webhookSecret;

    public ChapaService(InvoiceRepository invoiceRepository,
                        UserRepository userRepository,
                        ObjectMapper objectMapper,
                        CurrentUserService currentUserService,
                        EmailService emailService) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
        this.emailService = emailService;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.chapa.co/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Initialize a Chapa payment for an invoice.
     * Only the tenant who owns the invoice may initialize payment (enforced from the JWT).
     */
    public ChapaPaymentResponse initializePayment(Long invoiceId, String phoneNumber) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        // Ownership: a tenant may only pay their own invoice. Throws 403 otherwise.
        Tenant tenant = currentUserService.requireOwnership(invoice);

        if (invoice.getStatus() == com.rms.entity.InvoiceStatus.PAID) {
            throw new IllegalArgumentException("Invoice already paid");
        }

        User user = userRepository.findById(invoice.getLease().getTenant().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate unique transaction reference
        String txRef = "RMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Build payment payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", invoice.getTotalAmount().toString());
        payload.put("currency", "ETB");
        payload.put("tx_ref", txRef);
        payload.put("first_name", user.getName().split(" ")[0]);
        payload.put("last_name", user.getName().split(" ").length > 1 ?
                user.getName().substring(user.getName().indexOf(" ") + 1) : "Tenant");
        payload.put("email", tenant.getEmail() != null ? tenant.getEmail() : "tenant@rms.com");
        payload.put("phone_number", phoneNumber);
        payload.put("callback_url", redirectUrl + "?status=success&tx_ref=" + txRef);
        payload.put("return_url", redirectUrl + "?status=return&tx_ref=" + txRef);
        
        // Add metadata for webhook processing
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoice_number", invoice.getInvoiceNumber());
        metadata.put("invoice_id", invoice.getId());
        metadata.put("tenant_id", tenant.getId());
        metadata.put("lease_id", invoice.getLease().getId());
        payload.put("meta", metadata);
        
        payload.put("customization", Map.of(
                "title", "RMS Payment",
                "description", "Payment for Invoice " + invoice.getInvoiceNumber()
        ));

        try {
            // Call Chapa API
            String response = webClient.post()
                    .uri("/transaction/initialize")
                    .header("Authorization", "Bearer " + secretKey)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new RuntimeException("Chapa API error: " + error)));
                    })
                    .bodyToMono(String.class)
                    .block();

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("status") && jsonResponse.get("status").asText().equals("success")) {
                String checkoutUrl = jsonResponse.get("data").get("checkout_url").asText();
                return new ChapaPaymentResponse(checkoutUrl, txRef, "Payment initialized successfully");
            } else {
                String message = jsonResponse.has("message") ? jsonResponse.get("message").asText() : "Payment initialization failed";
                throw new RuntimeException(message);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Chapa payment: " + e.getMessage());
        }
    }

    /**
     * Verify a Chapa payment by transaction reference
     */
    public boolean verifyPayment(String txRef) {
        try {
            String response = webClient.get()
                    .uri("/transaction/verify/" + txRef)
                    .header("Authorization", "Bearer " + secretKey)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(error -> {
                                    System.err.println("Chapa verify error: " + error);
                                    return Mono.error(new RuntimeException("Chapa verification failed: " + error));
                                });
                    })
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Chapa verify response: " + response);

            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("status") && jsonResponse.get("status").asText().equals("success")) {
                JsonNode data = jsonResponse.get("data");
                String status = data.get("status").asText();
                System.out.println("Payment status: " + status);
                return "success".equalsIgnoreCase(status);
            }
            return false;

        } catch (Exception e) {
            System.err.println("Failed to verify payment: " + e.getMessage());
            return false;
        }
    }

    @Transactional
    public void processWebhook(Map<String, Object> payload, String signature) {
        if (!verifyWebhookSignature(payload, signature)) {
            System.err.println("❌ Invalid webhook signature");
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        String event = (String) payload.get("event");
        if (event != null && event.equals("charge.success")) {
            String txRef = (String) payload.get("tx_ref");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) payload.get("meta");
            
            if (meta == null) {
                System.err.println("❌ Metadata missing in webhook payload");
                throw new IllegalArgumentException("Metadata missing in webhook payload");
            }
            
            String invoiceNumber = (String) meta.get("invoice_number");

            if (invoiceNumber == null) {
                System.err.println("❌ Invoice number missing in webhook metadata");
                throw new IllegalArgumentException("Invoice number missing in webhook metadata");
            }

            Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                    .orElseThrow(() -> {
                        System.err.println("❌ Invoice not found: " + invoiceNumber);
                        return new IllegalArgumentException("Invoice not found: " + invoiceNumber);
                    });

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                System.out.println("⚠️ Invoice " + invoiceNumber + " already paid, skipping duplicate webhook");
                return;
            }

            // Validate status transition for Chapa payments
            if (!isValidInvoiceStatusTransition(invoice.getStatus(), InvoiceStatus.PAID)) {
                System.err.println("❌ Invalid status transition from " + invoice.getStatus() + " to PAID for Chapa payment");
                throw new IllegalArgumentException("Invalid status transition for invoice " + invoiceNumber);
            }

            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaymentDate(LocalDate.now());
            invoice.setPaymentMethod("CHAPA");
            invoiceRepository.save(invoice);

            User user = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
            if (user != null && user.getEmail() != null) {
                emailService.sendPaymentConfirmationEmail(user, invoice);
            }

            System.out.println("✅ Payment recorded via webhook: " + txRef + " for invoice " + invoiceNumber);
        } else {
            System.out.println("ℹ️ Webhook event not processed: " + event);
        }
    }

    /**
     * Verify webhook signature using HMAC SHA256
     */
    private boolean verifyWebhookSignature(Map<String, Object> payload, String signature) {
        try {
            if (webhookSecret == null || webhookSecret.isEmpty()) {
                System.err.println("Webhook secret not configured");
                return false;
            }

            // Convert payload to JSON string for signature calculation
            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(payload);

            // Calculate HMAC SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
            
            // Convert to Base64 for comparison
            String calculatedSignature = Base64.getEncoder().encodeToString(signatureBytes);
            
            // Compare signatures
            return calculatedSignature.equals(signature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("Error calculating webhook signature: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error verifying webhook signature: " + e.getMessage());
            return false;
        }
    }

    private boolean isValidInvoiceStatusTransition(InvoiceStatus from, InvoiceStatus to) {
        // Valid transitions:
        // PENDING -> SUBMITTED (tenant submits payment)
        // PENDING -> OVERDUE (scheduled task marks overdue)
        // SUBMITTED -> PAID (admin approves payment)
        // OVERDUE -> SUBMITTED (tenant submits late payment)
        // OVERDUE -> PAID (admin approves late payment)
        // PENDING -> PAID (Chapa direct payment)
        // OVERDUE -> PAID (Chapa direct payment for overdue invoices)
        // No other transitions are valid
        switch (from) {
            case PENDING:
                return to == InvoiceStatus.SUBMITTED || to == InvoiceStatus.OVERDUE || to == InvoiceStatus.PAID;
            case SUBMITTED:
                return to == InvoiceStatus.PAID;
            case OVERDUE:
                return to == InvoiceStatus.SUBMITTED || to == InvoiceStatus.PAID;
            case PAID:
                return false; // Already paid, no further transitions
            default:
                return false;
        }
    }
}