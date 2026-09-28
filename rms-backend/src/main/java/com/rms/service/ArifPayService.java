package com.rms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.Tenant;
import com.rms.entity.User;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class ArifPayService {

    private static final Logger log = LoggerFactory.getLogger(ArifPayService.class);
    private static final DateTimeFormatter EXPIRE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;
    private final TimeTravelService timeTravelService;
    private final SmsService smsService;
    private final WebClient webClient;

    @Value("${arifpay.checkout-path}") private String checkoutPath;
    @Value("${arifpay.beneficiary-account}") private String beneficiaryAccount;
    @Value("${arifpay.beneficiary-bank}") private String beneficiaryBank;
    @Value("${arifpay.payment-methods}") private List<String> paymentMethods;
    @Value("${arifpay.notify-url}") private String notifyUrl;
    @Value("${arifpay.success-url}") private String successUrl;
    @Value("${arifpay.cancel-url}") private String cancelUrl;
    @Value("${arifpay.error-url}") private String errorUrl;

    public ArifPayService(InvoiceRepository invoiceRepository,
                          UserRepository userRepository,
                          CurrentUserService currentUserService,
                          EmailService emailService,
                          TimeTravelService timeTravelService,
                          SmsService smsService,
                          @Value("${arifpay.base-url}") String baseUrl,
                          @Value("${arifpay.api-key}") String apiKey) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.emailService = emailService;
        this.timeTravelService = timeTravelService;
        this.smsService = smsService;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-arifpay-key", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Creates an ArifPay checkout session for the logged-in tenant's own invoice. */
    public Map<String, String> initialize(Long invoiceId, String phoneNumber) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        Tenant tenant = currentUserService.requireOwnership(invoice);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("Invoice already paid");
        }

        String phone = SmsService.normalizePhone(phoneNumber);
        if (phone == null) {
            throw new IllegalArgumentException("Invalid phone number");
        }

        double amount = invoice.getTotalAmount().setScale(2, RoundingMode.HALF_UP).doubleValue();
        String nonce = "INV-" + invoice.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> item = new HashMap<>();
        item.put("name", "Invoice " + invoice.getInvoiceNumber());
        item.put("quantity", 1);
        item.put("price", amount);
        item.put("description", "Payment for Invoice " + invoice.getInvoiceNumber());

        Map<String, Object> beneficiary = new HashMap<>();
        beneficiary.put("accountNumber", beneficiaryAccount);
        beneficiary.put("bank", beneficiaryBank);
        beneficiary.put("amount", amount);

        Map<String, Object> body = new HashMap<>();
        body.put("nonce", nonce);
        body.put("phone", phone);
        body.put("email", tenant.getEmail() != null ? tenant.getEmail() : "tenant@rms.com");
        body.put("lang", "EN");
        body.put("paymentMethods", paymentMethods);
        body.put("expireDate", LocalDateTime.now().plusHours(2).format(EXPIRE_FMT));
        body.put("items", List.of(item));
        body.put("beneficiaries", List.of(beneficiary));
        body.put("notifyUrl", notifyUrl);
        body.put("successUrl", successUrl);
        body.put("cancelUrl", cancelUrl);
        body.put("errorUrl", errorUrl);

        JsonNode data = call(() -> webClient.post()
                .uri(checkoutPath)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());

        Map<String, String> result = new HashMap<>();
        result.put("sessionId", data.path("sessionId").asText());
        result.put("paymentUrl", data.path("paymentUrl").asText());
        log.info("ArifPay session {} created for invoice {}", result.get("sessionId"), invoice.getInvoiceNumber());
        return result;
    }

    /**
     * Checks a session with ArifPay and marks the invoice PAID if it succeeded.
     * Used by the frontend callback page and by the notify webhook.
     * Never trusts the caller: the payment status always comes from ArifPay.
     */
    @Transactional
    public Map<String, Object> verify(String sessionId) {
        JsonNode session = call(() -> webClient.get()
                .uri(checkoutPath + "/" + sessionId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block());

        Long invoiceId = invoiceIdFromNonce(session.path("nonce").asText(""));
        if (invoiceId == null) {
            throw new IllegalArgumentException("Unknown payment session");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        String status = session.path("transaction").path("transactionStatus").asText("");

        Map<String, Object> result = new HashMap<>();
        result.put("invoiceNumber", invoice.getInvoiceNumber());
        result.put("status", status);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            result.put("verified", true);
            return result;
        }

        if (!"SUCCESS".equalsIgnoreCase(status)) {
            log.info("ArifPay session {} for invoice {} not successful: {}", sessionId, invoiceId, status);
            result.put("verified", false);
            return result;
        }

        BigDecimal paid = new BigDecimal(session.path("totalAmount").asText("0"));
        if (paid.compareTo(invoice.getTotalAmount()) < 0) {
            log.warn("ArifPay underpayment on invoice {}: paid {} expected {}",
                    invoiceId, paid, invoice.getTotalAmount());
            result.put("verified", false);
            return result;
        }

        markPaid(invoice, session.path("phone").asText(""), paid, sessionId);
        result.put("verified", true);
        return result;
    }

    /** ArifPay server to server callback. */
    @Transactional
    public void handleNotify(JsonNode payload) {
        log.info("ArifPay notify: {}", payload);

        String sessionId = payload.path("sessionId").asText("");
        if (sessionId.isBlank()) sessionId = payload.path("uuid").asText("");
        if (sessionId.isBlank()) {
            log.warn("ArifPay notify without sessionId, ignored");
            return;
        }

        try {
            verify(sessionId);
        } catch (Exception e) {
            log.error("ArifPay notify for session {} failed: {}", sessionId, e.getMessage());
        }
    }

    private void markPaid(Invoice invoice, String phone, BigDecimal paid, String sessionId) {
        if (!invoice.getStatus().canTransitionTo(InvoiceStatus.PAID)) {
            throw new IllegalArgumentException("Invalid status transition from " + invoice.getStatus() + " to PAID");
        }

        try {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaymentDate(timeTravelService.getCurrentDate());
            invoice.setPaymentMethod("ARIFPAY");
            Invoice saved = invoiceRepository.save(invoice);

            log.info("Payment confirmed via ArifPay session {} for invoice {}", sessionId, saved.getInvoiceNumber());

            // Notifications must never undo a confirmed payment
            try {
                User user = userRepository.findById(saved.getLease().getTenant().getUserId()).orElse(null);
                if (user != null && user.getEmail() != null) {
                    emailService.sendPaymentConfirmationEmail(user, saved);
                }
            } catch (Exception e) {
                log.error("Payment email failed for invoice {}: {}", saved.getInvoiceNumber(), e.getMessage());
            }

            if (phone != null && !phone.isBlank()) {
                smsService.send(phone, "RMS: Payment of ETB " + paid.setScale(2, RoundingMode.HALF_UP)
                        + " for invoice " + saved.getInvoiceNumber() + " received. Thank you.");
            }
        } catch (OptimisticLockingFailureException e) {
            log.info("Invoice {} was updated concurrently (webhook and callback overlap)", invoice.getId());
        }
    }

    private Long invoiceIdFromNonce(String nonce) {
        String[] parts = nonce.split("-");
        if (parts.length < 2 || !"INV".equals(parts[0])) return null;
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private JsonNode call(Supplier<JsonNode> request) {
        try {
            JsonNode res = request.get();
            if (res == null) {
                throw new IllegalStateException("Empty response from ArifPay");
            }
            if (res.path("error").asBoolean(false)) {
                throw new IllegalStateException("ArifPay error: " + res.path("msg").asText());
            }
            return res.path("data");
        } catch (WebClientResponseException e) {
            log.error("ArifPay HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("ArifPay request failed: " + e.getResponseBodyAsString());
        }
    }
}