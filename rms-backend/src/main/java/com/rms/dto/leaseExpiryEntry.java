package com.rms.dto;

import java.time.LocalDate;

public class leaseExpiryEntry {
    private Long leaseId;
    private String tenantName;
    private String spaceUnit;
    private LocalDate endDate;
    private long daysRemaining;
    private String status;

    public leaseExpiryEntry(Long leaseId, String tenantName, String spaceUnit, LocalDate endDate, long daysRemaining, String status) {
        this.leaseId = leaseId;
        this.tenantName = tenantName;
        this.spaceUnit = spaceUnit;
        this.endDate = endDate;
        this.daysRemaining = daysRemaining;
        this.status = status;
    }

    public long daysRemaining() {
        return daysRemaining;
    }

    // Getters
    public Long getLeaseId() { return leaseId; }
    public String getTenantName() { return tenantName; }
    public String getSpaceUnit() { return spaceUnit; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
}