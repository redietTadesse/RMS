package com.rms.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    @Value("${afromessage.api-url}")
    private String apiUrl;

    @Value("${afromessage.token}")
    private String token;

    @Value("${afromessage.sender-name}")
    private String senderName;

    @Value("${afromessage.identifier-id}")
    private String identifierId;

    /** Sends an SMS. Never throws: a failed SMS must not break a payment. */
    public boolean send(String phone, String message) {
        String to = normalizePhone(phone);
        if (to == null) {
            log.warn("SMS skipped, invalid phone: {}", phone);
            return false;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("from", identifierId);
            body.put("sender", senderName);
            body.put("to", to);
            body.put("message", message);

            JsonNode res = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            boolean ok = res != null && "success".equalsIgnoreCase(res.path("acknowledge").asText());
            if (!ok) log.warn("AfroMessage rejected SMS to {}: {}", to, res);
            return ok;
        } catch (Exception e) {
            log.error("AfroMessage call failed for {}: {}", to, e.getMessage());
            return false;
        }
    }

    /** 0912345678 / +251912345678 / 912345678 -> 251912345678 */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("\\D", "");
        if (d.startsWith("251") && d.length() == 12) return d;
        if (d.startsWith("0") && d.length() == 10) return "251" + d.substring(1);
        if ((d.startsWith("9") || d.startsWith("7")) && d.length() == 9) return "251" + d;
        return null;
    }
}