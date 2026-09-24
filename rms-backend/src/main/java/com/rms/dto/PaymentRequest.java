package com.rms.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import com.rms.service.TimeTravelService;
import java.time.LocalDate;

public class PaymentRequest {
    private static TimeTravelService timeTravelService;

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDate paymentDate;

    @NotBlank(message = "Payment method is required")
    @Pattern(
            regexp = "(?i)CASH|BANK_TRANSFER|CHAPA|RECEIPT",
            message = "Payment method must be CASH, BANK_TRANSFER, CHAPA, or RECEIPT"
    )
    private String paymentMethod;

    @DecimalMin(value = "0.00", message = "Amount cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Amount cannot exceed 99,999,999.99")
    private BigDecimal amount;


    public static void setTimeTravelService(TimeTravelService service) {
        timeTravelService = service;
    }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }


    /**
     * Validates all fields are present and valid
     */
    public boolean isValid() {
        return paymentDate != null &&
               paymentMethod != null && !paymentMethod.isBlank() &&
               (amount == null || amount.compareTo(BigDecimal.ZERO) >= 0);
    }

    /**
     * Gets validation error messages
     */
    public String getValidationError() {
        if (paymentDate == null) {
            return "Payment date is required";
        }
        if (paymentDate.isAfter((timeTravelService != null) ? timeTravelService.getCurrentDate() : LocalDate.now())) {
            return "Payment date cannot be in the future";
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "Payment method is required";
        }
        
        String method = paymentMethod.toUpperCase();
        if (!"CASH".equals(method) && !"BANK_TRANSFER".equals(method) && 
            !"CHAPA".equals(method) && !"RECEIPT".equals(method)) {
            return "Payment method must be CASH, BANK_TRANSFER, CHAPA, or RECEIPT";
        }
        
        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                return "Payment amount cannot be negative";
            }
            if (amount.compareTo(new BigDecimal("99999999.99")) > 0) {
                return "Payment amount cannot exceed 99,999,999.99";
            }
        }
        
        return null;
    }

    /**
     * Returns the normalized payment method (uppercase)
     */
    public String getNormalizedPaymentMethod() {
        return paymentMethod != null ? paymentMethod.toUpperCase() : null;
    }

    /**
     * Checks if this is a CHAPA payment
     */
    public boolean isChapaPayment() {
        return "CHAPA".equalsIgnoreCase(paymentMethod);
    }

    /**
     * Checks if this is a receipt submission
     */
    public boolean isReceiptPayment() {
        return "RECEIPT".equalsIgnoreCase(paymentMethod);
    }

    /**
     * Creates a copy with validated fields
     */
    public PaymentRequest normalize() {
        PaymentRequest copy = new PaymentRequest();
        copy.setPaymentDate(this.paymentDate);
        copy.setPaymentMethod(getNormalizedPaymentMethod());
        copy.setAmount(this.amount);
        return copy;
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "paymentDate=" + paymentDate +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", amount=" + amount +
                '}';
    }
}
