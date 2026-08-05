package com.rms.dto;

import java.math.BigDecimal;

public record DashboardStats(
        long totalSpaces,
        long vacantSpaces,
        long occupiedSpaces,
        long activeLeases,
        long pendingPayments,
        long overduePayments,
        BigDecimal totalPenalties
) {}
