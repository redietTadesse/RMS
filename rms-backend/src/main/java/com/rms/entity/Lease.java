package com.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import com.rms.service.TimeTravelService;
import java.time.LocalDate;

@Entity
@Table(name = "leases")
public class Lease {
    // The "time travel" demo needs lease dates to follow a simulated date instead of
    // the real one. Entities can't have services injected into them, so we keep one
    // static reference here and set it at startup / whenever the demo date changes.
    private static TimeTravelService timeTravelService;

    public static void setTimeTravelService(TimeTravelService service) {
        timeTravelService = service;
    }

    // Today according to the demo clock, or the real today when time travel is off.
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

    // How many months the tenant pays up front when the lease starts (3 by default).
    // No rent invoice is raised until those months have passed.
    @Column(name = "prepayment_months")
    private Integer initialPaymentMonths = 3;

    // Flips to true once the up-front months are used up and normal monthly rent begins.
    @Column(name = "payment_schedule_active", nullable = false)
    private Boolean paymentScheduleActive = false;

    // First normal rent payment date. Null while the tenant is still in the up-front period.
    @Column(name = "next_payment_due_date")
    private LocalDate nextPaymentDueDate;

    // Day of the month rent falls due (1-31). Clamped to the last day in short months,
    // so a due day of 31 simply means "the last day" in February.
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

    // When true, the first invoice only bills for the days left in that month
    // instead of a full month's rent.
    @Column(name = "first_invoice_prorated")
    private Boolean firstInvoiceProrated = false;

    // Optimistic locking. Hibernate bumps this number on every save so two people
    // editing the same lease can't silently overwrite each other's changes.
    @Version
    @Column(name = "version")
    private Long version;

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

    // Active means: status is ACTIVE and today sits between the start and end date.
    public boolean isActive() {
        if (status != LeaseStatus.ACTIVE) {
            return false;
        }
        LocalDate today = getCurrentDate();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    // Expired either because it was marked expired, or because the end date passed
    // while it was still ACTIVE (the nightly job normally flips that for us).
    public boolean isExpired() {
        return status == LeaseStatus.EXPIRED || 
               (status == LeaseStatus.ACTIVE && getCurrentDate().isAfter(endDate));
    }

    // Still inside the up-front months the tenant paid for when the lease started.
    public boolean isInPrepaymentPeriod() {
        if (status != LeaseStatus.ACTIVE) {
            return false;
        }
        return paymentScheduleActive && nextPaymentDueDate != null &&
               getCurrentDate().isBefore(nextPaymentDueDate);
    }

    // Days left on the lease, or -1 when it isn't active.
    public long getDaysUntilExpiry() {
        if (status != LeaseStatus.ACTIVE) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(getCurrentDate(), endDate);
    }

    // Days until the next billing date, or -1 when it isn't active or unset.
    public long getDaysUntilNextBilling() {
        if (status != LeaseStatus.ACTIVE || nextBillingDate == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(getCurrentDate(), nextBillingDate);
    }

    // Part-month rent: charges from the given day to the end of that month, using a
    // daily rate of monthlyRent / days-in-month. Falls back to today if monthDate is null.
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

    // Start date must not fall after the end date.
    public boolean hasValidDates() {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !startDate.isAfter(endDate);
    }

    // The up-front period has to be somewhere between 1 and 24 months.
    public boolean hasValidInitialPayment() {
        return initialPaymentMonths != null && initialPaymentMonths >= 1 && initialPaymentMonths <= 24;
    }

    // Due day has to be a day number a calendar month can have.
    public boolean hasValidDueDay() {
        return dueDay != null && dueDay >= 1 && dueDay <= 31;
    }

    // Rent has to be more than zero.
    public boolean hasValidRent() {
        return monthlyRent != null && monthlyRent.compareTo(BigDecimal.ZERO) > 0;
    }

    // Deposit is optional, but it can never be negative.
    public boolean hasValidDeposit() {
        return deposit != null && deposit.compareTo(BigDecimal.ZERO) >= 0;
    }

    // When rent is next due. If we already have a date stored we use it, otherwise we
    // roll forward to the next time the due day comes around (which lands next month
    // if that day has already passed this month).
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

    // Days left until rent is next due, or -1 when there is no due date to count to.
    public long getDaysUntilDue() {
        LocalDate nextDue = getNextDueDate();
        if (nextDue == null) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(getCurrentDate(), nextDue);
    }

    // True when rent's due date has already gone by.
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