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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import io.netty.channel.ChannelOption;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChapaService {

    private final WebClient webClient;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;
    private final Map<String, String> processedWebhooks = new ConcurrentHashMap<>();
    private final Map<String, Long> txToInvoiceMap = new ConcurrentHashMap<>();

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
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                                .responseTimeout(Duration.ofSeconds(30))
                ))
                .build();
    }

    public ChapaPaymentResponse initializePayment(Long invoiceId, String phoneNumber) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        Tenant tenant = currentUserService.requireOwnership(invoice);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("Invoice already paid");
        }

        User user = userRepository.findById(invoice.getLease().getTenant().getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String txRef = "RMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

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

            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("status") && jsonResponse.get("status").asText().equals("success")) {
                String checkoutUrl = jsonResponse.get("data").get("checkout_url").asText();
                txToInvoiceMap.put(txRef, invoice.getId());
                return new ChapaPaymentResponse(checkoutUrl, txRef, "Payment initialized successfully");
            } else {
                String message = jsonResponse.has("message") ? jsonResponse.get("message").asText() : "Payment initialization failed";
                throw new RuntimeException(message);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Chapa payment: " + e.getMessage());
        }
    }

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

            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("status") && jsonResponse.get("status").asText().equals("success")) {
                JsonNode data = jsonResponse.get("data");
                String status = data.get("status").asText();
                return "success".equalsIgnoreCase(status);
            }
            return false;

        } catch (Exception e) {
            System.err.println("Failed to verify payment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Confirm a Chapa payment directly (frontend callback path).
     * Re-verifies with Chapa's API using the API secret key (NOT the webhook
     * secret), then marks the linked invoice as PAID. Provides a reliable
     * fallback so a paid order is recorded even if webhooks are misconfigured.
     */
    @Transactional
    public Invoice confirmPayment(String txRef) {
        if (txRef == null || txRef.isEmpty()) {
            throw new IllegalArgumentException("Transaction reference is missing");
        }

        boolean verified = verifyPayment(txRef);
        if (!verified) {
            throw new IllegalArgumentException("Payment could not be verified with Chapa. Please contact admin.");
        }

        Long invoiceId = txToInvoiceMap.get(txRef);
        if (invoiceId == null) {
            throw new IllegalArgumentException("No invoice linked to transaction " + txRef + ". Please contact admin.");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return invoice;
        }

        if (!isValidInvoiceStatusTransition(invoice.getStatus(), InvoiceStatus.PAID)) {
            throw new IllegalArgumentException("Invalid status transition from " + invoice.getStatus() + " to PAID");
        }

        try {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaymentDate(LocalDate.now());
            invoice.setPaymentMethod("CHAPA");
            Invoice saved = invoiceRepository.save(invoice);

            processedWebhooks.put(txRef, "processed");
            // Keep the txRef→invoice mapping so an idempotent retry (e.g. React
            // StrictMode double-invoke, webhook + callback overlap) short-circuits
            // on the PAID check above instead of failing a lookup.

            User user = userRepository.findById(saved.getLease().getTenant().getUserId()).orElse(null);
            if (user != null && user.getEmail() != null) {
                emailService.sendPaymentConfirmationEmail(user, saved);
            }

            System.out.println("Payment confirmed via callback: " + txRef + " for invoice " + saved.getInvoiceNumber());
            return saved;
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalArgumentException("Invoice was updated by another process. Please try again.");
        }
    }

    @Transactional
    public void processWebhook(Map<String, Object> payload, String signature) {
        String txRef = (String) payload.get("tx_ref");

        if (txRef != null && processedWebhooks.containsKey(txRef)) {
            System.out.println("Duplicate webhook ignored: " + txRef);
            return;
        }

        if (!verifyWebhookSignature(payload, signature)) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        String event = (String) payload.get("event");
        if (event != null && event.equals("charge.success")) {
            if (txRef == null) {
                throw new IllegalArgumentException("Transaction reference missing in webhook");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) payload.get("meta");

            if (meta == null) {
                throw new IllegalArgumentException("Metadata missing in webhook payload");
            }

            String invoiceNumber = (String) meta.get("invoice_number");

            if (invoiceNumber == null) {
                throw new IllegalArgumentException("Invoice number missing in webhook metadata");
            }

            Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceNumber));

            if (invoice.getStatus() == InvoiceStatus.PAID) {
                processedWebhooks.put(txRef, "processed");
                return;
            }

            if (!isValidInvoiceStatusTransition(invoice.getStatus(), InvoiceStatus.PAID)) {
                throw new IllegalArgumentException("Invalid status transition from " + invoice.getStatus() + " to PAID");
            }

            try {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaymentDate(LocalDate.now());
                invoice.setPaymentMethod("CHAPA");
                invoiceRepository.save(invoice);

                processedWebhooks.put(txRef, "processed");

                User user = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
                if (user != null && user.getEmail() != null) {
                    emailService.sendPaymentConfirmationEmail(user, invoice);
                }

                System.out.println("Payment recorded via webhook: " + txRef + " for invoice " + invoiceNumber);
            } catch (Exception e) {
                processedWebhooks.remove(txRef);
                throw new RuntimeException("Failed to process webhook: " + e.getMessage());
            }
        }
    }

    private boolean verifyWebhookSignature(Map<String, Object> payload, String signature) {
        try {
            if (webhookSecret == null || webhookSecret.isEmpty()) {
                return false;
            }

            String payloadJson = new ObjectMapper().writeValueAsString(payload);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));

            String calculatedSignature = Base64.getEncoder().encodeToString(signatureBytes);

            return calculatedSignature.equals(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidInvoiceStatusTransition(InvoiceStatus from, InvoiceStatus to) {
        switch (from) {
            case PENDING:
                return to == InvoiceStatus.SUBMITTED || to == InvoiceStatus.OVERDUE || to == InvoiceStatus.PAID;
            case SUBMITTED:
                return to == InvoiceStatus.PAID;
            case OVERDUE:
                return to == InvoiceStatus.SUBMITTED || to == InvoiceStatus.PAID;
            case PAID:
                return false;
            default:
                return false;
        }
    }
}