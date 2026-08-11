// src/main/java/com/rms/entity/DocumentType.java
package com.rms.entity;

public enum DocumentType {
    LEASE_AGREEMENT("Lease Agreement"),
    ID_CARD("ID Card / Passport"),
    BUSINESS_LICENSE("Business License"),
    TIN_CERTIFICATE("TIN Certificate"),
    BANK_DETAILS("Bank Details"),
    OTHER("Other");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}