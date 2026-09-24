package com.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import com.rms.service.TimeTravelService;
import java.time.LocalDate;

@Entity
@Table(name = "leases")
public class Lease {
    private static TimeTravelService timeTravelService;

    public static void setTimeTravelService(TimeTravelService service) {
        timeTravelService = service;
    }

    /** Returns virtual date if set, otherwise real system date */
    private LocalDate getCurrentDate() {
        return (timeTravelService != null) ? timeTravelService.getCurrentDate() : LocalDate.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Tenant is required")
    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @NotNull(message = "Space is required")
    @ManyToOne(optional = false)
    @JoinColumn(name = "space_id")
    private Space space;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Monthly rent is required")
    @DecimalMin(value = "0.01", message = "Monthly rent must be at least 0.01")
    @DecimalMax(value = "99999999.99", message = "Monthly rent cannot exceed 99,999,999.99")
    @Column(name = "monthly_rent", nullable = false)
    private BigDecimal monthlyRent;

    @DecimalMin(value = "0.00", message = "Deposit cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Deposit cannot exceed 99,999,999.99")
    @Column(name = "deposit")
    private BigDecimal deposit = BigDecimal.ZERO;

    @Column(name = "prepayment_months")
    private Integer initialPaymentMonths = 3;

    @Column(name = "payment_schedule_active", nullable = false)
    private Boolean paymentScheduleActive = false;

    @Column(name = "next_payment_due_date")
    private LocalDate nextPaymentDueDate;

    @NotNull(message = "Due day is required")
    @Min(value = 1, message = "Due day must be between 1 and 31")
    @Max(value = 31, message = "Due day must be between 1 and 31")
    @Column(name = "due_day", nullable = false)
    private Integer dueDay = 1;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaseStatus status = LeaseStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @NotNull(message = "Billing frequency is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_frequency")
    private BillingFrequency billingFrequency = BillingFrequency.MONTHLY;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "first_invoice_prorated")
    private Boolean firstInvoiceProrated = false;

    @Version
    @Column(name = "version")
    private Long version;

    // ─── GETTERS AND SETTERS ───

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Space getSpace() { return space; }
    public void setSpace(Space space) { this.space = space; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getMonthlyRent() { return monthlyRent; }
    public void setMonthlyRent(BigDecimal monthlyRent) { this.monthlyRent = monthlyRent; }

    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }

    public Integer getInitialPaymentMonths() { return initialPaymentMonths; }
    public void setInitialPaymentMonths(Integer initialPaymentMonths) { this.initialPaymentMonths = initialPaymentMonths; }

    public boolean isPaymentScheduleActive() { return Boolean.TRUE.equals(paymentScheduleActive); }
    public void setPaymentScheduleActive(Boolean paymentScheduleActive) { this.paymentScheduleActive = paymentScheduleActive; }

    public LocalDate getNextPaymentDueDate() { return nextPaymentDueDate; }
    public void setNextPaymentDueDate(LocalDate nextPaymentDueDate) { this.nextPaymentDueDate = nextPaymentDueDate; }

    public Integer getDueDay() { return dueDay; }
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }

    public LeaseStatus getStatus() { return status; }
    public void setStatus(LeaseStatus status) { this.status = status; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDate updatedAt) { this.updatedAt = updatedAt; }

    public BillingFrequency getBillingFrequency() { return billingFrequency; }
    public void setBillingFrequency(BillingFrequency billingFrequency) { this.billingFrequency = billingFrequency; }

    public LocalDate getNextBillingDate() { return nextBillingDate; }
    public void setNextBillingDate(LocalDate nextBillingDate) { this.nextBillingDate = nextBillingDate; }

    public Boolean getFirstInvoiceProrated() { return firstInvoiceProrated; }
    public void setFirstInvoiceProrated(Boolean firstInvoiceProrated) { this.firstInvoiceProrated = firstInvoiceProrated; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    // ─── CUSTOM VALIDATION HELPERS ───

    /**
     * Checks if the lease is currently active based on status and dates
     */
    public boolean isActive() {
        if (status != LeaseStatus.ACTIVE) {
            return false;
        }
        LocalDate today = getCurrentDate();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /**
     * Checks if the lease has expired
     */
    public boolean isExpired() {
        return status == LeaseStatus.EXPIRED || 
               (status == LeaseStatus.ACTIVE && getCurrentDate().isAfter(endDate));
    }

    /**
     * Checks if the lease is currently in the prepayment period
     */
    public boolean isInPrepaymentPeriod() {
        if (status != LeaseStatus.ACTIVE) {
            return false;
        }
        return paymentScheduleActive && nextPaymentDueDate != null &&
               getCurrentDate().isBefore(nextPaymentDueDate);
    }

    /**
     * Gets the number of days until the lease expires
     */
    public long getDaysUntilExpiry() {
        if (status != LeaseStatus.ACTIVE) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(getCurrentDate(), endDate);
    }

    /**
     * Gets the number of days until the next billing date
     */
    public long getDaysUntilNextBilling() {
        if (status != LeaseStatus.ACTIVE || nextBillingDate == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(getCurrentDate(), nextBillingDate);
    }

    /**
     * Calculates the prorated rent for a given month
     */
    public BigDecimal calculateProratedRent(LocalDate monthDate) {
        if (monthDate == null) {
            monthDate = getCurrentDate();
        }
        
        int daysInMonth = monthDate.lengthOfMonth();
        int dayOfMonth = monthDate.getDayOfMonth();
        int daysRemaining = daysInMonth - dayOfMonth + 1;
        
        BigDecimal dailyRate = monthlyRent.divide(BigDecimal.valueOf(daysInMonth), 2, 
                java.math.RoundingMode.HALF_UP);
        
        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Validates the lease dates
     */
    public boolean hasValidDates() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * Validates the initial payment months
     */
    public boolean hasValidInitialPayment() {
        return initialPaymentMonths != null && initialPaymentMonths >= 1 && initialPaymentMonths <= 24;
    }

    /**
     * Validates the due day
     */
    public boolean hasValidDueDay() {
        return dueDay != null && dueDay >= 1 && dueDay <= 31;
    }

    /**
     * Validates the rent amount
     */
    public boolean hasValidRent() {
        return monthlyRent != null && monthlyRent.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Validates the deposit amount
     */
    public boolean hasValidDeposit() {
        return deposit != null && deposit.compareTo(BigDecimal.ZERO) >= 0;
    }

    // ─── BUSINESS LOGIC METHODS ───

    /**
     * Calculates the next due date after the prepayment period
     */
    public LocalDate getNextDueDate() {
        if (status != LeaseStatus.ACTIVE) {
            return null;
        }

        LocalDate today = getCurrentDate();

        if (nextPaymentDueDate != null) {
            return nextPaymentDueDate;
        }

        LocalDate dueDate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));

        while (dueDate.isBefore(today)) {
            dueDate = dueDate.plusMonths(1);
            dueDate = dueDate.withDayOfMonth(Math.min(dueDay, dueDate.lengthOfMonth()));
        }
        
        return dueDate;
    }

    /**
     * Gets the number of days until the next due date
     */
    public long getDaysUntilDue() {
        LocalDate nextDue = getNextDueDate();
        if (nextDue == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(getCurrentDate(), nextDue);
    }

    /**
     * Checks if the lease is overdue
     */
    public boolean isOverdue() {
        if (status != LeaseStatus.ACTIVE) {
            return false;
        }
        LocalDate nextDue = getNextDueDate();
        return nextDue != null && getCurrentDate().isAfter(nextDue);
    }

    @Override
    public String toString() {
        return "Lease{" +
                "id=" + id +
                ", tenant=" + (tenant != null ? tenant.getName() : "null") +
                ", space=" + (space != null ? space.getUnitNumber() : "null") +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", monthlyRent=" + monthlyRent +
                ", status=" + status +
                '}';
    }
}