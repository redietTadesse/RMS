package com.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantType type = TenantType.INDIVIDUAL;

    private String contact;

    @NotBlank
    private String phone;

    @Email
    private String email;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "tin_number")
    private String tinNumber;

    @Column(name = "business_license")
    private String businessLicense;

    @Column(name = "has_id_card", nullable = false)
    private boolean hasIdCard = false;

    @Column(name = "has_business_license", nullable = false)
    private boolean hasBusinessLicense = false;

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