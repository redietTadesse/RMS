package com.rms.dto;

import com.rms.entity.BillingFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LeaseRequest {

    @NotNull(message = "Tenant ID is required")
    @Positive(message = "Tenant ID must be a positive number")
    private Long tenantId;

    @NotNull(message = "Space ID is required")
    @Positive(message = "Space ID must be a positive number")
    private Long spaceId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    @NotNull(message = "Monthly rent is required")
    @Positive(message = "Monthly rent must be a positive amount")
    @DecimalMin(value = "1.00", message = "Monthly rent must be at least 1.00")
    @DecimalMax(value = "99999999.99", message = "Monthly rent cannot exceed 99,999,999.99")
    private BigDecimal monthlyRent;

    @DecimalMin(value = "0.00", message = "Deposit cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Deposit cannot exceed 99,999,999.99")
    private BigDecimal deposit = BigDecimal.ZERO;

    @NotNull(message = "Prepayment months is required")
    @Min(value = 3, message = "Prepayment must be at least 3 months")
    @Max(value = 24, message = "Prepayment cannot exceed 24 months")
    private Integer prepaymentMonths;

    @NotNull(message = "Due day is required")
    @Min(value = 1, message = "Due day must be between 1 and 31")
    @Max(value = 31, message = "Due day must be between 1 and 31")
    private Integer dueDay = 1;

    @NotNull(message = "Billing frequency is required")
    private BillingFrequency billingFrequency = BillingFrequency.MONTHLY;

    // Getters and Setters
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getSpaceId() { return spaceId; }
    public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public void setMonthlyRent(BigDecimal monthlyRent) { this.monthlyRent = monthlyRent; }

    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }

    public Integer getPrepaymentMonths() { return prepaymentMonths; }
    public void setPrepaymentMonths(Integer prepaymentMonths) { this.prepaymentMonths = prepaymentMonths; }

    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }

    public BillingFrequency getBillingFrequency() { return billingFrequency; }
    public void setBillingFrequency(BillingFrequency billingFrequency) { this.billingFrequency = billingFrequency; }
}