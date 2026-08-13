package com.rms.dto;

import java.math.BigDecimal;

public record RevenueReportEntry(
        String month,       // e.g. "2026-03"
        String label,       // e.g. "Mar 2026"
        BigDecimal revenue
) {}