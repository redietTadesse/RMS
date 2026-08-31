package com.rms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SettingsRequest {

    @NotNull(message = "Penalty rate is required")
    @Min(value = 0, message = "Penalty rate must be at least 0%")
    @Max(value = 100, message = "Penalty rate must be at most 100%")
    private Integer penaltyRate;

    @NotNull(message = "Payment window end is required")
    @Min(value = 1, message = "Payment window end must be between 1 and 31")
    @Max(value = 31, message = "Payment window end must be between 1 and 31")
    private Integer paymentWindowEnd;

    public Integer getPenaltyRate() { return penaltyRate; }
    public void setPenaltyRate(Integer penaltyRate) { this.penaltyRate = penaltyRate; }

    public Integer getPaymentWindowEnd() { return paymentWindowEnd; }
    public void setPaymentWindowEnd(Integer paymentWindowEnd) { this.paymentWindowEnd = paymentWindowEnd; }
}