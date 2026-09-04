package com.rms.dto;

import java.math.BigDecimal;

/**
 * Financial accounting summary for the dashboard/reports.
 * Provides full transparency on revenue, taxes, withholding, and penalties.
 */
public record AccountingSummary(
    BigDecimal totalRevenue,
    BigDecimal totalRent,
    BigDecimal totalVat,
    BigDecimal totalPenalties,
    BigDecimal totalWithholdingTax,
    BigDecimal amountToCollect,
    long paidInvoicesCount,
    long penaltyInvoicesCount,
    long withholdingInvoicesCount,
    long vatInvoicesCount
) {}