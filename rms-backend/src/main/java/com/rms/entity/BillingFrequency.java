package com.rms.entity;

public enum BillingFrequency {
    MONTHLY(1, "Monthly"),
    BI_MONTHLY(2, "Every 2 Months"),
    QUARTERLY(3, "Quarterly"),
    SEMI_ANNUAL(6, "Every 6 Months"),
    ANNUAL(12, "Annually");

    private final int months;
    private final String displayName;

    BillingFrequency(int months, String displayName) {
        this.months = months;
        this.displayName = displayName;
    }

    public int getMonths() { return months; }
    public String getDisplayName() { return displayName; }
}