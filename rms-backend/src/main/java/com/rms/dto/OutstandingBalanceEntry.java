package com.rms.dto;

import java.math.BigDecimal;

public record OutstandingBalanceEntry(
        Long tenantId,
        String tenantName,
        String phone,
        long unpaidInvoiceCount,
        BigDecimal totalOwed
) {}