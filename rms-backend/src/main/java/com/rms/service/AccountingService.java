package com.rms.service;

import com.rms.dto.AccountingSummary;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.Lease;
import com.rms.entity.LeaseStatus;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.LeaseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class AccountingService {

    private final InvoiceRepository invoiceRepository;
    private final LeaseRepository leaseRepository;
    private final TaxCalculationService taxCalculationService;
    private final SettingsService settingsService;

    public AccountingService(InvoiceRepository invoiceRepository,
                             LeaseRepository leaseRepository,
                             TaxCalculationService taxCalculationService,
                             SettingsService settingsService) {
        this.invoiceRepository = invoiceRepository;
        this.leaseRepository = leaseRepository;
        this.taxCalculationService = taxCalculationService;
        this.settingsService = settingsService;
    }

    /**
     * Build an accounting summary for paid invoices within an optional date range.
     */
    public AccountingSummary getSummary(LocalDate start, LocalDate end) {
        List<Invoice> invoices;
        if (start != null && end != null) {
            invoices = invoiceRepository.findPaidBetween(start, end);
        } else {
            invoices = invoiceRepository.findByStatus(InvoiceStatus.PAID);
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRent = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalPenalties = BigDecimal.ZERO;
        BigDecimal totalWithholdingTax = BigDecimal.ZERO;
        long paidCount = 0;
        long penaltyCount = 0;
        long withholdingCount = 0;
        long vatCount = 0;

        for (Invoice inv : invoices) {
            paidCount++;

            // Revenue = total amount (rent + penalty)
            BigDecimal total = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
            totalRevenue = totalRevenue.add(total);

            // Rent (inclusive)
            BigDecimal rent = inv.getRentAmount() != null ? inv.getRentAmount() : BigDecimal.ZERO;
            totalRent = totalRent.add(rent);

            // Penalties
            BigDecimal penalty = inv.getPenaltyAmount() != null ? inv.getPenaltyAmount() : BigDecimal.ZERO;
            if (penalty.compareTo(BigDecimal.ZERO) > 0) {
                totalPenalties = totalPenalties.add(penalty);
                penaltyCount++;
            }

            // Tax breakdown using TaxCalculationService
            var breakdown = taxCalculationService.getTaxBreakdown(
                inv.getLease().getTenant(),
                rent,
                inv.getIssueDate() != null ? inv.getIssueDate() : LocalDate.now()
            );

            // VAT
            if (breakdown.getVatAmount().compareTo(BigDecimal.ZERO) > 0) {
                totalVat = totalVat.add(breakdown.getVatAmount());
                vatCount++;
            }

            // Withholding tax
            if (breakdown.isWithholdingApplicable() &&
                breakdown.getWithholdingTax().compareTo(BigDecimal.ZERO) > 0) {
                totalWithholdingTax = totalWithholdingTax.add(breakdown.getWithholdingTax());
                withholdingCount++;
            }
        }

        // Amount to collect = total revenue - withholding (since withholding is deducted at source)
        BigDecimal amountToCollect = totalRevenue.subtract(totalWithholdingTax);

        return new AccountingSummary(
            totalRevenue,
            totalRent,
            totalVat,
            totalPenalties,
            totalWithholdingTax,
            amountToCollect,
            paidCount,
            penaltyCount,
            withholdingCount,
            vatCount
        );
    }

    /**
     * Get active leases count.
     */
    public long getActiveLeasesCount() {
        return leaseRepository.findByStatus(LeaseStatus.ACTIVE).size();
    }

    /**
     * Get total outstanding balance (pending + overdue invoices).
     */
    public BigDecimal getTotalOutstanding() {
        List<Invoice> pending = invoiceRepository.findByStatusIn(
            List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE));
        BigDecimal sum = BigDecimal.ZERO;
        for (Invoice inv : pending) {
            sum = sum.add(inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO);
        }
        return sum;
    }
}