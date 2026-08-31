package com.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

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

    @Column(name = "has_id_card", nullable = false)
    private boolean hasIdCard = false;

    @Column(name = "has_business_license", nullable = false)
    private boolean hasBusinessLicense = false;

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

    public boolean isHasIdCard() { return hasIdCard; }
    public void setHasIdCard(boolean hasIdCard) { this.hasIdCard = hasIdCard; }

    public boolean isHasBusinessLicense() { return hasBusinessLicense; }
    public void setHasBusinessLicense(boolean hasBusinessLicense) { this.hasBusinessLicense = hasBusinessLicense; }
}