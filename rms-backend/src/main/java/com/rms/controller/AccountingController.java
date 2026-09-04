package com.rms.controller;

import com.rms.dto.AccountingSummary;
import com.rms.service.AccountingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/accounting")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:5173")
public class AccountingController {

    private final AccountingService accountingService;

    public AccountingController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    /**
     * Get accounting summary for a date range (defaults to all-time).
     */
    @GetMapping("/summary")
    public AccountingSummary getSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return accountingService.getSummary(start, end);
    }

    /**
     * Get a simplified key-value version for the dashboard widget.
     */
    @GetMapping("/dashboard-widget")
    public Map<String, Object> getDashboardWidget() {
        AccountingSummary summary = accountingService.getSummary(null, null);
        Map<String, Object> result = new HashMap<>();
        result.put("totalRevenue", summary.totalRevenue());
        result.put("totalPenalties", summary.totalPenalties());
        result.put("totalWithholdingTax", summary.totalWithholdingTax());
        result.put("totalVat", summary.totalVat());
        result.put("amountToCollect", summary.amountToCollect());
        result.put("activeLeases", accountingService.getActiveLeasesCount());
        result.put("totalOutstanding", accountingService.getTotalOutstanding());
        return result;
    }
}