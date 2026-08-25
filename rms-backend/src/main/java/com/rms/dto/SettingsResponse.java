package com.rms.dto;

import java.time.LocalDateTime;

public class SettingsResponse {
    private Integer penaltyRate;
    private Integer paymentWindowEnd;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public SettingsResponse() {}

    public SettingsResponse(Integer penaltyRate, Integer paymentWindowEnd, LocalDateTime updatedAt, String updatedBy) {
        this.penaltyRate = penaltyRate;
        this.paymentWindowEnd = paymentWindowEnd;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Integer getPenaltyRate() { return penaltyRate; }
    public void setPenaltyRate(Integer penaltyRate) { this.penaltyRate = penaltyRate; }

    public Integer getPaymentWindowEnd() { return paymentWindowEnd; }
    public void setPaymentWindowEnd(Integer paymentWindowEnd) { this.paymentWindowEnd = paymentWindowEnd; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}