package com.rms.service;

import com.rms.entity.Invoice;
import com.rms.entity.Tenant;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class TaxCalculationService {

    private static final BigDecimal VAT_RATE = BigDecimal.valueOf(0.15);
    private static final BigDecimal WITHHOLDING_RATE = BigDecimal.valueOf(0.03);
    private static final BigDecimal VAT_DIVISOR = BigDecimal.valueOf(1.15);

    private final SettingsService settingsService;

    public TaxCalculationService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public BigDecimal getUntaxedRent(BigDecimal rentInclusive) {
        if (rentInclusive == null || rentInclusive.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return rentInclusive.divide(VAT_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateWithholdingTax(Tenant tenant, BigDecimal rentInclusive, LocalDate invoiceDate) {
        if (tenant == null || !isWithholdingApplicableOnDate(tenant, invoiceDate)) {
            return BigDecimal.ZERO;
        }
        BigDecimal untaxedRent = getUntaxedRent(rentInclusive);
        return untaxedRent.multiply(WITHHOLDING_RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isWithholdingApplicableOnDate(Tenant tenant, LocalDate date) {
        if (tenant == null || !tenant.isWithholdingTaxApplicable()) {
            return false;
        }
        if (!tenant.isWithholdingEvidenceVerified()) {
            return false;
        }
        LocalDate startDate = tenant.getWithholdingStartDate();
        if (startDate == null) {
            return false;
        }
        return !date.isBefore(startDate);
    }

    public BigDecimal calculatePenalty(Tenant tenant, BigDecimal rentInclusive, LocalDate invoiceDate) {
        if (rentInclusive == null || rentInclusive.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal untaxedRent = getUntaxedRent(rentInclusive);
        BigDecimal penaltyRate = BigDecimal.valueOf(
            settingsService.getIntSetting("penalty_rate", 20)
        ).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return untaxedRent.multiply(penaltyRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

        public BigDecimal calculateTotalAmount(BigDecimal rentInclusive, BigDecimal penalty) {
        return rentInclusive.add(penalty);
    }

    // ─── Configuration accessors ───
    public BigDecimal getWithholdingRate() { return WITHHOLDING_RATE; }
    public BigDecimal getPenaltyRate() {
        return BigDecimal.valueOf(settingsService.getIntSetting("penalty_rate", 20))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }
    public BigDecimal getVatRate() { return VAT_RATE; }

    public TaxBreakdown getTaxBreakdown(Tenant tenant, BigDecimal rentInclusive, LocalDate invoiceDate) {
        BigDecimal untaxedRent = getUntaxedRent(rentInclusive);
        BigDecimal vatAmount = rentInclusive.subtract(untaxedRent);
        BigDecimal withholdingTax = calculateWithholdingTax(tenant, rentInclusive, invoiceDate);
        boolean withholdingApplicable = isWithholdingApplicableOnDate(tenant, invoiceDate);
        BigDecimal penalty = calculatePenalty(tenant, rentInclusive, invoiceDate);
        BigDecimal totalAmount = rentInclusive.add(penalty);

        return new TaxBreakdown(
                rentInclusive,
                untaxedRent,
                vatAmount,
                withholdingTax,
                withholdingApplicable,
                penalty,
                totalAmount
        );
    }

    public static class TaxBreakdown {
        private final BigDecimal rentInclusive;
        private final BigDecimal untaxedRent;
        private final BigDecimal vatAmount;
        private final BigDecimal withholdingTax;
        private final boolean withholdingApplicable;
        private final BigDecimal penalty;
        private final BigDecimal totalAmount;

        public TaxBreakdown(BigDecimal rentInclusive, BigDecimal untaxedRent,
                           BigDecimal vatAmount, BigDecimal withholdingTax,
                           boolean withholdingApplicable, BigDecimal penalty,
                           BigDecimal totalAmount) {
            this.rentInclusive = rentInclusive;
            this.untaxedRent = untaxedRent;
            this.vatAmount = vatAmount;
            this.withholdingTax = withholdingTax;
            this.withholdingApplicable = withholdingApplicable;
            this.penalty = penalty;
            this.totalAmount = totalAmount;
        }

        public BigDecimal getRentInclusive() { return rentInclusive; }
        public BigDecimal getUntaxedRent() { return untaxedRent; }
        public BigDecimal getVatAmount() { return vatAmount; }
        public BigDecimal getWithholdingTax() { return withholdingTax; }
        public boolean isWithholdingApplicable() { return withholdingApplicable; }
        public BigDecimal getPenalty() { return penalty; }
        public BigDecimal getTotalAmount() { return totalAmount; }
    }
}