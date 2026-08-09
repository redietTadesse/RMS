package com.rms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChapaPaymentResponse {
    private String checkoutUrl;
    private String txRef;
    private String message;
    private boolean success;

    // Default constructor (required by Jackson)
    public ChapaPaymentResponse() {
        this.success = true;
    }

    // Constructor with all fields
    public ChapaPaymentResponse(String checkoutUrl, String txRef, String message) {
        this.checkoutUrl = checkoutUrl;
        this.txRef = txRef;
        this.message = message;
        this.success = true;
    }

    // Static factory methods
    public static ChapaPaymentResponse success(String checkoutUrl, String txRef, String message) {
        return new ChapaPaymentResponse(checkoutUrl, txRef, message);
    }

    public static ChapaPaymentResponse error(String message) {
        ChapaPaymentResponse response = new ChapaPaymentResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    // Getters and Setters
    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }

    public String getTxRef() { return txRef; }
    public void setTxRef(String txRef) { this.txRef = txRef; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}