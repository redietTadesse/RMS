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

    @NotNull(message = "Initial payment months is required")
    @Min(value = 1, message = "Initial payment must be at least 1 month")
    @Max(value = 24, message = "Initial payment cannot exceed 24 months")
    private Integer initialPaymentMonths;

    @NotNull(message = "Due day is required")
    @Min(value = 1, message = "Due day must be between 1 and 31")
    @Max(value = 31, message = "Due day must be between 1 and 31")
    private Integer dueDay = 1;

    @NotNull(message = "Billing frequency is required")
    private BillingFrequency billingFrequency = BillingFrequency.MONTHLY;


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

    public Integer getInitialPaymentMonths() { return initialPaymentMonths; }
    public void setInitialPaymentMonths(Integer initialPaymentMonths) { this.initialPaymentMonths = initialPaymentMonths; }

    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }

    public BillingFrequency getBillingFrequency() { return billingFrequency; }
    public void setBillingFrequency(BillingFrequency billingFrequency) { this.billingFrequency = billingFrequency; }


    /**
     * Validates that the start date is not after the end date
     */
    public boolean hasValidDateRange() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * Validates that the lease duration is at least 1 month
     */
    public boolean hasValidDuration() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return endDate.isAfter(startDate) || endDate.isEqual(startDate.plusMonths(1));
    }

    /**
     * Validates all fields are present and valid
     */
    public boolean isValid() {
        return tenantId != null && tenantId > 0 &&
               spaceId != null && spaceId > 0 &&
               startDate != null &&
               endDate != null &&
               monthlyRent != null && monthlyRent.compareTo(BigDecimal.ZERO) > 0 &&
               initialPaymentMonths != null && initialPaymentMonths >= 1 && initialPaymentMonths <= 24 &&
               dueDay != null && dueDay >= 1 && dueDay <= 31 &&
               billingFrequency != null &&
               hasValidDateRange();
    }

    /**
     * Gets validation error messages
     */
    public String getValidationError() {
        if (tenantId == null || tenantId <= 0) {
            return "Valid Tenant ID is required";
        }
        if (spaceId == null || spaceId <= 0) {
            return "Valid Space ID is required";
        }
        if (startDate == null) {
            return "Start date is required";
        }
        if (endDate == null) {
            return "End date is required";
        }
        if (!hasValidDateRange()) {
            return "Start date cannot be after end date";
        }
        if (!hasValidDuration()) {
            return "Lease duration must be at least 1 month";
        }
        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            return "Monthly rent must be a positive amount";
        }
        if (monthlyRent.compareTo(new BigDecimal("1.00")) < 0) {
            return "Monthly rent must be at least 1.00";
        }
        if (initialPaymentMonths == null || initialPaymentMonths < 1) {
            return "Initial payment must be at least 1 month";
        }
        if (initialPaymentMonths > 24) {
            return "Initial payment cannot exceed 24 months";
        }
        if (dueDay == null || dueDay < 1 || dueDay > 31) {
            return "Due day must be between 1 and 31";
        }
        if (billingFrequency == null) {
            return "Billing frequency is required";
        }
        return null;
    }

    /**
     * Creates a copy of this request with updated values
     */
    public LeaseRequest copyWith(
            Long tenantId,
            Long spaceId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal monthlyRent,
            BigDecimal deposit,
            Integer initialPaymentMonths,
            Integer dueDay,
            BillingFrequency billingFrequency) {
        
        LeaseRequest copy = new LeaseRequest();
        copy.setTenantId(tenantId != null ? tenantId : this.tenantId);
        copy.setSpaceId(spaceId != null ? spaceId : this.spaceId);
        copy.setStartDate(startDate != null ? startDate : this.startDate);
        copy.setEndDate(endDate != null ? endDate : this.endDate);
        copy.setMonthlyRent(monthlyRent != null ? monthlyRent : this.monthlyRent);
        copy.setDeposit(deposit != null ? deposit : this.deposit);
        copy.setInitialPaymentMonths(initialPaymentMonths != null ? initialPaymentMonths : this.initialPaymentMonths);
        copy.setDueDay(dueDay != null ? dueDay : this.dueDay);
        copy.setBillingFrequency(billingFrequency != null ? billingFrequency : this.billingFrequency);
        return copy;
    }

    @Override
    public String toString() {
        return "LeaseRequest{" +
                "tenantId=" + tenantId +
                ", spaceId=" + spaceId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", monthlyRent=" + monthlyRent +
                ", deposit=" + deposit +
                ", initialPaymentMonths=" + initialPaymentMonths +
                ", dueDay=" + dueDay +
                ", billingFrequency=" + billingFrequency +
                '}';
    }
}
