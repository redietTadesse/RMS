package com.rms.controller;

import com.rms.dto.SettingsRequest;
import com.rms.dto.SettingsResponse;
import com.rms.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:5173")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
    public ResponseEntity<SettingsResponse> getSettings() {
        int penaltyRate = settingsService.getIntSetting("penalty_rate", 20);
        int paymentWindowEnd = settingsService.getIntSetting("payment_window_end", 10);

        SettingsResponse response = new SettingsResponse(
                penaltyRate,
                paymentWindowEnd,
                LocalDateTime.now(),
                "system"
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SettingsResponse> updateSettings(@RequestBody SettingsRequest request) {
        settingsService.updateSetting(
                "penalty_rate",
                String.valueOf(request.getPenaltyRate()),
                "Penalty rate percentage",
                "admin"
        );
        settingsService.updateSetting(
                "payment_window_end",
                String.valueOf(request.getPaymentWindowEnd()),
                "Day of month after which penalties apply",
                "admin"
        );

        SettingsResponse response = new SettingsResponse(
                request.getPenaltyRate(),
                request.getPaymentWindowEnd(),
                LocalDateTime.now(),
                "admin"
        );
        return ResponseEntity.ok(response);
    }
}