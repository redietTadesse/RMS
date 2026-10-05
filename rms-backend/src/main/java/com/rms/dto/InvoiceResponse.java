package com.rms.dto;

import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.service.TaxCalculationService;
import com.rms.service.TimeTravelService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String tenantName;
    private String spaceUnit;
    private BigDecimal rentAmount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private String paymentMethod;
    private LocalDate paymentDate;
    private String receiptPath;
    private long daysOverdue;
    private boolean isOverdue;
    private String lastPaymentDate;
    private String leaseStartDate;
    private String leaseEndDate;
    private BigDecimal untaxedRent;
    private BigDecimal vatAmount;
        private BigDecimal withholdingTax;
    private boolean withholdingApplicable;
    private BigDecimal amountToCollect;
    private BigDecimal dailyPenaltyRate;
    private BigDecimal penaltyRate;
    private BigDecimal rentInclusive;

    // Plain-English derivations of the numbers above, so an admin can see which
    // formula produced each amount without reading the code.
    private String rentExplanation;        // "Full month rent" / "Prorated: ..."
    private String dueDateExplanation;     // "Due on day 10 of the month"
    private String penaltyExplanation;     // "5 days overdue × ETB 115.94/day"

    private static TimeTravelService timeTravelService;

    public static void setTimeTravelService(TimeTravelService service) {
        timeTravelService = service;
    }

    public static InvoiceResponse from(Invoice invoice) {
        return from(invoice, null);
    }

    public static InvoiceResponse from(Invoice invoice, TaxCalculationService taxCalculationService) {
        if (invoice == null) {
            return null;
        }
        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setTenantName(invoice.getLease().getTenant().getName());
        response.setSpaceUnit(invoice.getLease().getSpace().getUnitNumber());
        response.setRentAmount(invoice.getRentAmount());
        response.setPenaltyAmount(invoice.getPenaltyAmount());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setIssueDate(invoice.getIssueDate());
        response.setDueDate(invoice.getDueDate());
        response.setStatus(invoice.getStatus());
        response.setPaymentMethod(invoice.getPaymentMethod());
        response.setPaymentDate(invoice.getPaymentDate());
        response.setReceiptPath(invoice.getReceiptPath());
        response.setLeaseStartDate(invoice.getLease().getStartDate().toString());
        response.setLeaseEndDate(invoice.getLease().getEndDate().toString());

        // Use virtual time if available, otherwise real time
        LocalDate currentDate = (timeTravelService != null) ? timeTravelService.getCurrentDate() : LocalDate.now();

        // Populate tax breakdown if service is available
        if (taxCalculationService != null && invoice.getRentAmount() != null) {
            com.rms.service.TaxCalculationService.TaxBreakdown breakdown =
                    taxCalculationService.getTaxBreakdown(
                            invoice.getLease().getTenant(),
                            invoice.getRentAmount(),
                            invoice.getIssueDate() != null ? invoice.getIssueDate() : currentDate
                    );
            response.setUntaxedRent(breakdown.getUntaxedRent());
            response.setVatAmount(breakdown.getVatAmount());
            response.setWithholdingTax(breakdown.getWithholdingTax());
                        response.setWithholdingApplicable(breakdown.isWithholdingApplicable());

            // Amount to collect = total - withholding (withholding is deducted at source)
            BigDecimal amountToCollect = breakdown.getTotalAmount();
            if (breakdown.isWithholdingApplicable()) {
                amountToCollect = amountToCollect.subtract(breakdown.getWithholdingTax());
            }
            response.setAmountToCollect(amountToCollect);
            response.setRentInclusive(breakdown.getRentInclusive());
        }

        if (invoice.getStatus() == InvoiceStatus.OVERDUE) {
            response.setDaysOverdue(java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), currentDate));
            response.setOverdue(true);
        }

                if (invoice.getRentAmount() != null && taxCalculationService != null) {
            BigDecimal monthlyPenalty = taxCalculationService.calculatePenalty(
                invoice.getLease().getTenant(),
                invoice.getRentAmount(),
                invoice.getIssueDate() != null ? invoice.getIssueDate() : timeTravelService.getCurrentDate()
            );
                        response.setDailyPenaltyRate(monthlyPenalty.divide(BigDecimal.valueOf(30), 2, java.math.RoundingMode.HALF_UP));
            response.setPenaltyRate(taxCalculationService.getPenaltyRate().multiply(BigDecimal.valueOf(100)));
        }

        BigDecimal rentAmount = invoice.getRentAmount();
        BigDecimal monthlyRent = invoice.getLease().getMonthlyRent();

        // ── Rent explanation ──
        // Anything below the lease's monthly rent was prorated for a part-month
        // start, so spell out the day count and the daily rate behind it.
        if (rentAmount != null && monthlyRent != null && rentAmount.compareTo(monthlyRent) < 0) {
            LocalDate start = invoice.getLease().getStartDate();
            int daysInStartMonth = start.lengthOfMonth();
            int daysRemaining = daysInStartMonth - start.getDayOfMonth() + 1;
            response.setRentExplanation(String.format(
                    "Prorated first month — %d of %d days (daily rate ETB %,.2f)",
                    daysRemaining, daysInStartMonth,
                    monthlyRent.divide(BigDecimal.valueOf(daysInStartMonth), 2, RoundingMode.HALF_UP)));
        } else {
            response.setRentExplanation("Full month rent — no proration");
        }

        // ── Due date explanation ──
        Integer dueDay = invoice.getLease().getDueDay();
        response.setDueDateExplanation(
                "Due on day " + (dueDay != null ? dueDay : 10) + " of the month");

        // ── Penalty explanation ──
        if (invoice.getPenaltyAmount() != null
                && invoice.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0
                && invoice.getStatus() == InvoiceStatus.OVERDUE
                && invoice.getDueDate() != null
                && rentAmount != null) {
            long days = ChronoUnit.DAYS.between(invoice.getDueDate(), currentDate);
            BigDecimal monthly = taxCalculationService != null
                    ? taxCalculationService.calculatePenalty(
                            invoice.getLease().getTenant(),
                            rentAmount,
                            invoice.getIssueDate() != null ? invoice.getIssueDate() : currentDate)
                    : BigDecimal.ZERO;
            BigDecimal daily = monthly.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
            response.setPenaltyExplanation(String.format(
                    "%d day%s overdue × ETB %,.2f/day (monthly ETB %,.2f ÷ 30)",
                    days, days == 1 ? "" : "s", daily, monthly));
        } else {
            response.setPenaltyExplanation("No penalty — invoice is not overdue");
        }

        if (invoice.getPaymentDate() != null) {
            response.setLastPaymentDate(invoice.getPaymentDate().toString());
        }

        return response;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getSpaceUnit() { return spaceUnit; }
    public void setSpaceUnit(String spaceUnit) { this.spaceUnit = spaceUnit; }

    public BigDecimal getRentAmount() { return rentAmount; }
    public void setRentAmount(BigDecimal rentAmount) { this.rentAmount = rentAmount; }

    public BigDecimal getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(BigDecimal penaltyAmount) { this.penaltyAmount = penaltyAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }

    public long getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(long daysOverdue) { this.daysOverdue = daysOverdue; }

    public boolean isOverdue() { return isOverdue; }
    public void setOverdue(boolean overdue) { isOverdue = overdue; }

    public String getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(String lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }

    public String getLeaseStartDate() { return leaseStartDate; }
    public void setLeaseStartDate(String leaseStartDate) { this.leaseStartDate = leaseStartDate; }

    public String getLeaseEndDate() { return leaseEndDate; }
    public void setLeaseEndDate(String leaseEndDate) { this.leaseEndDate = leaseEndDate; }

    public BigDecimal getUntaxedRent() { return untaxedRent; }
    public void setUntaxedRent(BigDecimal untaxedRent) { this.untaxedRent = untaxedRent; }

    public BigDecimal getVatAmount() { return vatAmount; }
    public void setVatAmount(BigDecimal vatAmount) { this.vatAmount = vatAmount; }

    public BigDecimal getWithholdingTax() { return withholdingTax; }
    public void setWithholdingTax(BigDecimal withholdingTax) { this.withholdingTax = withholdingTax; }

        public boolean isWithholdingApplicable() { return withholdingApplicable; }
    public void setWithholdingApplicable(boolean withholdingApplicable) { this.withholdingApplicable = withholdingApplicable; }

    public BigDecimal getAmountToCollect() { return amountToCollect; }
    public void setAmountToCollect(BigDecimal amountToCollect) { this.amountToCollect = amountToCollect; }

    public BigDecimal getDailyPenaltyRate() { return dailyPenaltyRate; }
    public void setDailyPenaltyRate(BigDecimal dailyPenaltyRate) { this.dailyPenaltyRate = dailyPenaltyRate; }

    public BigDecimal getPenaltyRate() { return penaltyRate; }
    public void setPenaltyRate(BigDecimal penaltyRate) { this.penaltyRate = penaltyRate; }

    public BigDecimal getRentInclusive() { return rentInclusive; }
    public void setRentInclusive(BigDecimal rentInclusive) { this.rentInclusive = rentInclusive; }

    public String getRentExplanation() { return rentExplanation; }
    public void setRentExplanation(String rentExplanation) { this.rentExplanation = rentExplanation; }

    public String getDueDateExplanation() { return dueDateExplanation; }
    public void setDueDateExplanation(String dueDateExplanation) { this.dueDateExplanation = dueDateExplanation; }

    public String getPenaltyExplanation() { return penaltyExplanation; }
    public void setPenaltyExplanation(String penaltyExplanation) { this.penaltyExplanation = penaltyExplanation; }
}