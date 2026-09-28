package com.rms.dto;

import com.rms.entity.TenantType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TenantCreateRequest {
    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Tenant type is required")
    private TenantType type;

    @Size(max = 100, message = "Contact must not exceed 100 characters")
    private String contact;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?251|0)?[79]\\d{8}$", message = "Invalid Ethiopian phone number format")
    private String phone;

    @Email(message = "Invalid email address format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Pattern(regexp = "^\\d{10}$", message = "TIN must be exactly 10 digits")
    private String tinNumber;

    @Size(max = 50, message = "Business license must not exceed 50 characters")
    private String businessLicense;
}
