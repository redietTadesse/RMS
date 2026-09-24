package com.rms.dto;

import com.rms.entity.Tenant;
import com.rms.entity.TenantType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponseDTO {
    private Long id;
    private String name;
    private TenantType type;
    private String contact;
    private String phone;
    private String email;
    private String tinNumber;
    private String businessLicense;
    private boolean hasIdCard;
    private boolean hasBusinessLicense;
    private boolean withholdingTaxApplicable;
    private LocalDate withholdingStartDate;
    private boolean withholdingEvidenceVerified;
    private String withholdingVerifiedBy;
    private LocalDateTime withholdingVerifiedAt;

    public static TenantResponseDTO from(Tenant tenant) {
        if (tenant == null) return null;
        return TenantResponseDTO.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .type(tenant.getType())
                .contact(tenant.getContact())
                .phone(tenant.getPhone())
                .email(tenant.getEmail())
                .tinNumber(tenant.getTinNumber())
                .businessLicense(tenant.getBusinessLicense())
                .hasIdCard(tenant.isHasIdCard())
                .hasBusinessLicense(tenant.isHasBusinessLicense())
                .withholdingTaxApplicable(tenant.isWithholdingTaxApplicable())
                .withholdingStartDate(tenant.getWithholdingStartDate())
                .withholdingEvidenceVerified(tenant.isWithholdingEvidenceVerified())
                .withholdingVerifiedBy(tenant.getWithholdingVerifiedBy())
                .withholdingVerifiedAt(tenant.getWithholdingVerifiedAt())
                .build();
    }
}
