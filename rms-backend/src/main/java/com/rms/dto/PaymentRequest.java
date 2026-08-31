package com.rms.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentRequest {

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDate paymentDate;

    @NotBlank(message = "Payment method is required")
    @Pattern(
            regexp = "CASH|BANK_TRANSFER|CHAPA|RECEIPT",
            message = "Payment method must be CASH, BANK_TRANSFER, CHAPA, or RECEIPT"
    )
    private String paymentMethod;

    @DecimalMin(value = "0.00", message = "Amount cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Amount cannot exceed 99,999,999.99")
    private BigDecimal amount;

    // Getters and Setters
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}