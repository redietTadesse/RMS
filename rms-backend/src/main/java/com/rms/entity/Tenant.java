package com.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.'-]+$", message = "Name can only contain letters, spaces, dots, apostrophes, and hyphens")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Tenant type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantType type = TenantType.INDIVIDUAL;

    @Size(max = 100, message = "Contact must not exceed 100 characters")
    private String contact;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?251|0)?[79]\\d{8}$", message = "Invalid Ethiopian phone number format. Expected: 09XXXXXXXX or 07XXXXXXXX")
    private String phone;

    @Email(message = "Invalid email address format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Positive(message = "User ID must be a positive number")
    @Column(name = "user_id", unique = true)
    private Long userId;

    @Pattern(regexp = "^\\d{10}$", message = "TIN must be exactly 10 digits")
    @Column(name = "tin_number")
    private String tinNumber;

    @Size(max = 50, message = "Business license must not exceed 50 characters")
    @Column(name = "business_license")
    private String businessLicense;

    // Boolean wrappers (not primitives) so Hibernate can load legacy rows where
    // the column is NULL without throwing "Null value was assigned to a property
    // of primitive type". Getters unbox null-safely.
    @Column(name = "has_id_card", nullable = false)
    private Boolean hasIdCard = false;

    @Column(name = "has_business_license", nullable = false)
    private Boolean hasBusinessLicense = false;

    // Withholding tax fields
    @Column(name = "is_withholding_tax_applicable", nullable = false)
    private Boolean withholdingTaxApplicable = false;

    @Column(name = "withholding_start_date")
    private LocalDate withholdingStartDate;

    @Column(name = "withholding_evidence_verified")
    private Boolean withholdingEvidenceVerified = false;

    @Column(name = "withholding_verified_by")
    private String withholdingVerifiedBy;

    @Column(name = "withholding_verified_at")
    private LocalDateTime withholdingVerifiedAt;

    @Column(name = "withholding_document_id")
    private Long withholdingDocumentId;

    // Getters and Setters (existing)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TenantType getType() { return type; }
    public void setType(TenantType type) { this.type = type; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTinNumber() { return tinNumber; }
    public void setTinNumber(String tinNumber) { this.tinNumber = tinNumber; }

    public String getBusinessLicense() { return businessLicense; }
    public void setBusinessLicense(String businessLicense) { this.businessLicense = businessLicense; }

    public boolean isHasIdCard() { return Boolean.TRUE.equals(hasIdCard); }
    public void setHasIdCard(Boolean hasIdCard) { this.hasIdCard = hasIdCard; }

    public boolean isHasBusinessLicense() { return Boolean.TRUE.equals(hasBusinessLicense); }
    public void setHasBusinessLicense(Boolean hasBusinessLicense) { this.hasBusinessLicense = hasBusinessLicense; }

    // Withholding tax getters and setters
    public boolean isWithholdingTaxApplicable() { return Boolean.TRUE.equals(withholdingTaxApplicable); }
    public void setWithholdingTaxApplicable(Boolean withholdingTaxApplicable) { this.withholdingTaxApplicable = withholdingTaxApplicable; }

    public LocalDate getWithholdingStartDate() { return withholdingStartDate; }
    public void setWithholdingStartDate(LocalDate withholdingStartDate) { this.withholdingStartDate = withholdingStartDate; }

    public boolean isWithholdingEvidenceVerified() { return Boolean.TRUE.equals(withholdingEvidenceVerified); }
    public void setWithholdingEvidenceVerified(Boolean withholdingEvidenceVerified) { this.withholdingEvidenceVerified = withholdingEvidenceVerified; }

    public String getWithholdingVerifiedBy() { return withholdingVerifiedBy; }
    public void setWithholdingVerifiedBy(String withholdingVerifiedBy) { this.withholdingVerifiedBy = withholdingVerifiedBy; }

    public LocalDateTime getWithholdingVerifiedAt() { return withholdingVerifiedAt; }
    public void setWithholdingVerifiedAt(LocalDateTime withholdingVerifiedAt) { this.withholdingVerifiedAt = withholdingVerifiedAt; }

    public Long getWithholdingDocumentId() { return withholdingDocumentId; }
    public void setWithholdingDocumentId(Long withholdingDocumentId) { this.withholdingDocumentId = withholdingDocumentId; }
}