// service/SmsService.java
package com.rms.service;

import com.rms.entity.Invoice;
import com.rms.entity.Tenant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsService {

    @Value("${afromessage.api-key}")
    private String apiKey;

    @Value("${afromessage.sender-id}")
    private String senderId;

    @Value("${afromessage.api-url:https://api.afromessage.com/api/send}")
    private String apiUrl;

    @Value("${app.base.url}")
    private String baseUrl;

    @Value("${app.sms.mode:mock}")
    private String smsMode;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendSms(Tenant tenant, String message) {
        if ("mock".equalsIgnoreCase(smsMode)) {
            sendMockSms(tenant, message);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("from", senderId);
            payload.put("to", tenant.getPhone());
            payload.put("text", message);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            String response = restTemplate.postForObject(apiUrl, request, String.class);
            System.out.println("✅ SMS sent to: " + tenant.getPhone());
            System.out.println("   Response: " + response);

        } catch (Exception e) {
            System.err.println("❌ Failed to send SMS: " + e.getMessage());
        }
    }

    private void sendMockSms(Tenant tenant, String message) {
        System.out.println("📱 [MOCK SMS] SMS would be sent to: " + tenant.getPhone());
        System.out.println("   From: " + senderId);
        System.out.println("   Message: " + message);
        System.out.println("   [Mock mode enabled - SMS not actually sent]");
    }

    public void sendRentReminderSms(Tenant tenant, Invoice invoice, int daysUntilDue) {
        String message = String.format(
                "🏢 RMS: Your rent of ETB %.2f for %s is due in %d days on %s. Pay via: %s/payments",
                invoice.getTotalAmount(),
                invoice.getLease().getSpace().getUnitNumber(),
                daysUntilDue,
                invoice.getDueDate(),
                baseUrl
        );
        sendSms(tenant, message);
    }

    public void sendOverdueSms(Tenant tenant, Invoice invoice, long daysOverdue, double dailyPenalty) {
        String message = String.format(
                "⚠️ URGENT: Your rent of ETB %.2f for %s is %d days overdue. Daily penalty: ETB %.2f. Total due: ETB %.2f. Pay now: %s/payments",
                invoice.getRentAmount(),
                invoice.getLease().getSpace().getUnitNumber(),
                daysOverdue,
                dailyPenalty,
                invoice.getTotalAmount(),
                baseUrl
        );
        sendSms(tenant, message);
    }

    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(smsMode);
    }
}