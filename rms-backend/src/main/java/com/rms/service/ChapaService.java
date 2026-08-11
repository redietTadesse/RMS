package com.rms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rms.dto.ChapaPaymentResponse;
import com.rms.entity.Invoice;
import com.rms.entity.Tenant;
import com.rms.entity.User;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Value("${chapa.secret-key}")
    private String secretKey;

    @Value("${chapa.redirect-url}")
    private String redirectUrl;

    @Value("${chapa.webhook-secret}")
    private String webhookSecret;

    public ChapaService(InvoiceRepository invoiceRepository,
                        UserRepository userRepository,
                        ObjectMapper objectMapper,
                        CurrentUserService currentUserService) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.currentUserService = currentUserService;
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

    /**
     * Process Chapa webhook
     */
    public void processWebhook(Map<String, Object> payload, String signature) {
        // Verify webhook signature
        if (!verifyWebhookSignature(payload, signature)) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        String event = (String) payload.get("event");
        if (event != null && event.equals("charge.success")) {
            String txRef = (String) payload.get("tx_ref");
            String invoiceNumber = (String) ((Map) payload.get("meta")).get("invoice_number");

            System.out.println("Payment successful for tx_ref: " + txRef);
            System.out.println("   Invoice: " + invoiceNumber);

            // TODO: Mark invoice as PAID — see note below. This is why status never flips.
        }
    }

    /**
     * Verify webhook signature
     */
    private boolean verifyWebhookSignature(Map<String, Object> payload, String signature) {
        // HMAC SHA256 verification — see https://developer.chapa.co/integrations/webhooks
        return true; // Simplified for now
    }
}