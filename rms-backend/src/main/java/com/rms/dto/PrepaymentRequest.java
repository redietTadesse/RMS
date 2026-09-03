package com.rms.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PrepaymentRequest {

    @NotNull(message = "Lease ID is required")
    @Positive(message = "Lease ID must be positive")
    private Long leaseId;

    @NotNull(message = "Months to pay is required")
    @Min(value = 1, message = "Must pay at least 1 month")
    @Max(value = 24, message = "Cannot pay more than 24 months at once")
    private Integer monthsToPay;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amountPaid;

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDate paymentDate;

    public Long getLeaseId() { return leaseId; }
    public void setLeaseId(Long leaseId) { this.leaseId = leaseId; }

    public Integer getMonthsToPay() { return monthsToPay; }
    public void setMonthsToPay(Integer monthsToPay) { this.monthsToPay = monthsToPay; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
}