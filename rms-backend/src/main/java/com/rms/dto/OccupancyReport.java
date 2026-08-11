package com.rms.dto;

public record OccupancyReport(
        long totalSpaces,
        long vacantSpaces,
        long occupiedSpaces,
        double vacantRate,   // 0.0 - 100.0
        double occupiedRate
) {}