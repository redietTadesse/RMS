package com.rms.dto;

import com.rms.entity.Lease;
import com.rms.entity.LeaseStatus;
import com.rms.entity.BillingFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseResponseDTO {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private Long spaceId;
    private String unitNumber;
    // Nested views of the related rows. The UI reads lease.tenant.name and
    // lease.space.unitNumber / lease.space.category, so these have to stay in the
    // payload alongside the flat fields above.
    private TenantResponseDTO tenant;
    private SpaceResponseDTO space;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private Integer initialPaymentMonths;
    private LeaseStatus status;
    private Integer dueDay;
    private BillingFrequency billingFrequency;
    private LocalDate nextPaymentDueDate;

    public static LeaseResponseDTO from(Lease lease) {
        if (lease == null) return null;
        return LeaseResponseDTO.builder()
                .id(lease.getId())
                .tenantId(lease.getTenant() != null ? lease.getTenant().getId() : null)
                .tenantName(lease.getTenant() != null ? lease.getTenant().getName() : null)
                .spaceId(lease.getSpace() != null ? lease.getSpace().getId() : null)
                .unitNumber(lease.getSpace() != null ? lease.getSpace().getUnitNumber() : null)
                .tenant(TenantResponseDTO.from(lease.getTenant()))
                .space(SpaceResponseDTO.from(lease.getSpace()))
                .startDate(lease.getStartDate())
                .endDate(lease.getEndDate())
                .monthlyRent(lease.getMonthlyRent())
                .deposit(lease.getDeposit())
                .initialPaymentMonths(lease.getInitialPaymentMonths())
                .status(lease.getStatus())
                .dueDay(lease.getDueDay())
                .billingFrequency(lease.getBillingFrequency())
                .nextPaymentDueDate(lease.getNextPaymentDueDate())
                .build();
    }
}
