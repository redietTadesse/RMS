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
    private static final BigDecimal PENALTY_RATE = BigDecimal.valueOf(0.20);
    private static final BigDecimal VAT_DIVISOR = BigDecimal.valueOf(1.15);

    public BigDecimal getRentExclusiveOfVat(BigDecimal rentInclusive) {
        if (rentInclusive == null || rentInclusive.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return rentInclusive.divide(VAT_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateWithholdingTax(Tenant tenant, BigDecimal rentInclusive, LocalDate invoiceDate) {
        if (tenant == null || !isWithholdingApplicableOnDate(tenant, invoiceDate)) {
            return BigDecimal.ZERO;
        }
        BigDecimal rentExclusive = getRentExclusiveOfVat(rentInclusive);
        return rentExclusive.multiply(WITHHOLDING_RATE).setScale(2, RoundingMode.HALF_UP);
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

    public BigDecimal getPenaltyBase(Tenant tenant, BigDecimal rentInclusive, LocalDate invoiceDate) {
        BigDecimal rentExclusive = getRentExclusiveOfVat(rentInclusive);
        BigDecimal withholdingTax = calculateWithholdingTax(tenant, rentInclusive, invoiceDate);
        return rentExclusive.subtract(withholdingTax);
    }

    public BigDecimal calculatePenalty(Tenant tenant, BigDecimal rentInclusive, LocalDate invoiceDate) {
        if (tenant == null || rentInclusive == null || rentInclusive.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal penaltyBase = getPenaltyBase(tenant, rentInclusive, invoiceDate);
        return penaltyBase.multiply(PENALTY_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalAmount(BigDecimal rentInclusive, BigDecimal penalty) {
        return rentInclusive.add(penalty);
    }

    public TaxBreakdown getTaxBreakdown(Tenant tenant, BigDecimal rentInclusive, LocalDate invoiceDate) {
        BigDecimal rentExclusive = getRentExclusiveOfVat(rentInclusive);
        BigDecimal vatAmount = rentInclusive.subtract(rentExclusive);
        BigDecimal withholdingTax = calculateWithholdingTax(tenant, rentInclusive, invoiceDate);
        boolean withholdingApplicable = isWithholdingApplicableOnDate(tenant, invoiceDate);
        BigDecimal penaltyBase = getPenaltyBase(tenant, rentInclusive, invoiceDate);
        BigDecimal penalty = calculatePenalty(tenant, rentInclusive, invoiceDate);
        BigDecimal totalAmount = rentInclusive.add(penalty);

        return new TaxBreakdown(rentInclusive, rentExclusive, vatAmount, withholdingTax, withholdingApplicable, penaltyBase, penalty, totalAmount);
    }

    public static class TaxBreakdown {
        private final BigDecimal rentInclusive;
        private final BigDecimal rentExclusive;
        private final BigDecimal vatAmount;
        private final BigDecimal withholdingTax;
        private final boolean withholdingApplicable;
        private final BigDecimal penaltyBase;
        private final BigDecimal penalty;
        private final BigDecimal totalAmount;

        public TaxBreakdown(BigDecimal rentInclusive, BigDecimal rentExclusive,
                           BigDecimal vatAmount, BigDecimal withholdingTax,
                           boolean withholdingApplicable, BigDecimal penaltyBase,
                           BigDecimal penalty, BigDecimal totalAmount) {
            this.rentInclusive = rentInclusive;
            this.rentExclusive = rentExclusive;
            this.vatAmount = vatAmount;
            this.withholdingTax = withholdingTax;
            this.withholdingApplicable = withholdingApplicable;
            this.penaltyBase = penaltyBase;
            this.penalty = penalty;
            this.totalAmount = totalAmount;
        }

        public BigDecimal getRentInclusive() { return rentInclusive; }
        public BigDecimal getRentExclusive() { return rentExclusive; }
        public BigDecimal getVatAmount() { return vatAmount; }
        public BigDecimal getWithholdingTax() { return withholdingTax; }
        public boolean isWithholdingApplicable() { return withholdingApplicable; }
        public BigDecimal getPenaltyBase() { return penaltyBase; }
        public BigDecimal getPenalty() { return penalty; }
        public BigDecimal getTotalAmount() { return totalAmount; }
    }
}