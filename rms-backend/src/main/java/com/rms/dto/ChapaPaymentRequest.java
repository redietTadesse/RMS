package com.rms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class ChapaPaymentRequest {

    @NotNull(message = "Invoice ID is required")
    @Positive(message = "Invoice ID must be a positive number")
    private Long invoiceId;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?251|0)?[79]\\d{8}$", message = "Invalid Ethiopian phone number format. Expected: 09XXXXXXXX or 07XXXXXXXX")
    private String phoneNumber;

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}