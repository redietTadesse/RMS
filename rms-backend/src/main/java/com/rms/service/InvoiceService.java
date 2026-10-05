package com.rms.service;

import com.rms.entity.*;
import com.rms.entity.PaymentSchedule;
import com.rms.entity.PaymentScheduleStatus;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.LeaseRepository;
import com.rms.repository.TenantRepository;
import com.rms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * InvoiceService — the billing engine of the system.
 *
 * This service is responsible for everything that has to do with invoices:
 *   1. Generating a new invoice each billing cycle (usually monthly).
 *   2. Adding a small penalty each day an invoice is late.
 *   3. Sending reminder emails before/on/after due dates.
 *   4. Handling prepayments (when a tenant pays for several months upfront).
 *
 * Four things run automatically on a schedule (see @Scheduled annotations):
 *   - On the 1st of every month at midnight: generate invoices.
 *   - Every night at 1 AM: add a day's worth of penalty to overdue invoices.
 *   - Every morning at 9 AM: send "rent is due today" emails.
 *   - Every morning at 8:30 AM: send "payment due soon" reminders.
 *
 * Every scheduled job has a matching "simulate" method. The difference:
 *   - Scheduled methods refuse to run when the admin has turned on time travel
 *     (the virtual-date testing mode). Otherwise they would fire on the real
 *     wall clock and generate data for the wrong date.
 *   - Simulate methods bypass that guard. The admin test panel calls them
 *     directly to run the job against the virtual date.
 */
@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    // ─── Dependencies (things this service needs to do its job) ─────────────

    private final InvoiceRepository invoiceRepository;              // read/write invoices
    private final LeaseRepository leaseRepository;                  // read/write leases
    private final TenantRepository tenantRepository;                // look up tenants
    private final UserRepository userRepository;                    // look up login accounts
    private final NotificationService notificationService;          // in-app bell notifications
    private final EmailService emailService;                        // outgoing emails
    private final SettingsService settingsService;                  // admin-configurable values
    private final TaxCalculationService taxCalculationService;      // VAT / withholding / penalty math
    private final PaymentScheduleService paymentScheduleService;    // prepayment handling
    private final TimeTravelService timeTravelService;              // virtual "today" for testing
    private final InvoiceNumberService invoiceNumberService;        // unique sequential invoice numbers

    /** Standard constructor — Spring injects every dependency above. */
    public InvoiceService(InvoiceRepository invoiceRepository,
                          LeaseRepository leaseRepository,
                          TenantRepository tenantRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          EmailService emailService,
                          SettingsService settingsService,
                          TaxCalculationService taxCalculationService,
                          PaymentScheduleService paymentScheduleService,
                          TimeTravelService timeTravelService,
                          InvoiceNumberService invoiceNumberService) {
        this.invoiceRepository = invoiceRepository;
        this.leaseRepository = leaseRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.settingsService = settingsService;
        this.taxCalculationService = taxCalculationService;
        this.paymentScheduleService = paymentScheduleService;
        this.timeTravelService = timeTravelService;
        this.invoiceNumberService = invoiceNumberService;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 1 — MONTHLY INVOICE GENERATION
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Runs automatically on the 1st of every month at midnight.
     * Skips entirely if time travel is active — in that case the admin
     * triggers invoice generation manually via the test panel (see
     * simulateMonthlyInvoices below).
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void generateMonthlyInvoices() {
        if (timeTravelService.isActive()) {
            return;   // virtual time is on — don't fire on the real clock
        }
        generateMonthlyInvoicesInternal();
    }

    /**
     * Manual version — called by the test panel when the admin wants to
     * simulate a month passing without waiting for the real cron.
     */
    @Transactional
    public void simulateMonthlyInvoices() {
        generateMonthlyInvoicesInternal();
    }

    /**
     * The actual invoice generation work. Walks every active lease and
     * creates an invoice for each one that is due — with two skip conditions:
     *
     *   1. `nextBillingDate` is still in the future (this lease isn't due yet).
     *   2. The lease is covered by a prepayment (tenant already paid ahead).
     */
    private void generateMonthlyInvoicesInternal() {
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);
        LocalDate today = timeTravelService.getCurrentDate();

        for (Lease lease : activeLeases) {
            // Skip if the lease's next billing date hasn't arrived yet.
            // This is how we honor billingFrequency — a quarterly lease
            // only generates an invoice every three months, not every month.
            LocalDate nextBillingDate = lease.getNextBillingDate();
            if (nextBillingDate != null && today.isBefore(nextBillingDate)) {
                continue;
            }

            // Skip if the tenant already paid for this period.
            if (paymentScheduleService.isCoveredByPrepayment(lease, today)) {
                continue;
            }

            generateInvoiceForLease(lease);
        }
    }

    /**
     * Creates ONE invoice for ONE lease. This is the most important method
     * in the service — most "walk me through the code" questions end up here.
     *
     * Returns the saved Invoice, or null if the invoice was skipped (because
     * one already exists for this month, or the tenant is prepaid).
     */
    @Transactional
    public Invoice generateInvoiceForLease(Lease lease) {
        // Sanity checks — this method shouldn't be called with bad input.
        if (lease == null) {
            throw new IllegalArgumentException("Lease cannot be null");
        }
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active. Cannot generate invoice.");
        }

        LocalDate today = timeTravelService.getCurrentDate();

        // Skip if the tenant has prepaid the current period.
        if (isCoveredByPrepayment(lease, today)) {
            return null;
        }

        // Skip if this lease already has an invoice for the current month.
        // (Without this, the monthly job would generate a duplicate invoice
        // every time it ran.)
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        if (invoiceRepository.existsByLeaseAndIssueDateBetween(lease, monthStart, monthEnd)) {
            return null;
        }

        // Compute the due date. Example: if lease.dueDay = 10, the due date
        // is the 10th of the current month. If the month doesn't have that
        // day (e.g. February and dueDay = 31), clamp to the last day.
        int dueDay = lease.getDueDay() != null ? lease.getDueDay() : 10;
        LocalDate dueDate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));
        // If the due day has already passed this month, the invoice would be born
        // overdue. Roll it to next month's due day so it starts life as PENDING.
        if (dueDate.isBefore(today)) {
            LocalDate nextMonth = today.plusMonths(1);
            dueDate = nextMonth.withDayOfMonth(Math.min(dueDay, nextMonth.lengthOfMonth()));
        }

        // Get a unique sequential invoice number, e.g. INV-2026-0001.
        String invoiceNumber = invoiceNumberService.nextInvoiceNumber(today);

        // Start with the full monthly rent.
        BigDecimal rentAmount = lease.getMonthlyRent();

        // If this is the very first invoice for a lease that started mid-month,
        // prorate it — but only when we are actually billing for that start month.
        // Then flip the flag so it only happens once.
        if (Boolean.TRUE.equals(lease.getFirstInvoiceProrated()) && !hasFirstInvoiceBeenGenerated(lease)) {
            if (shouldProrateFirstInvoice(lease, today)) {
                rentAmount = calculateProratedRent(lease, today);
            }
            lease.setFirstInvoiceProrated(false);   // clear either way — one-shot
            leaseRepository.save(lease);
        }

        // Build the new invoice. Starts as PENDING (unpaid).
        // penaltyAmount = 0 for now — penalties get added later by the daily job.
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setLease(lease);
        invoice.setRentAmount(rentAmount);
        invoice.setPenaltyAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(rentAmount);
        invoice.setIssueDate(today);
        invoice.setDueDate(dueDate);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setCreatedAt(today);

        Invoice saved = invoiceRepository.save(invoice);

        // Move the lease's nextBillingDate forward so we don't bill again
        // for the same period.
        updateNextBillingDate(lease);

        // Notify the tenant — once-only bell notification plus an email.
        User user = userRepository.findById(lease.getTenant().getUserId()).orElse(null);
        if (user != null) {
            notificationService.createForUserOnce(
                    user.getId(),
                    "INVOICE_GENERATED",
                    saved.getId(),
                    "New Invoice Generated",
                    "Invoice " + saved.getInvoiceNumber() + " for ETB " + saved.getTotalAmount() +
                            " is due on " + saved.getDueDate(),
                    "/payments"
            );
            emailService.sendInvoiceGeneratedEmail(user, saved, lease);
        }

        return saved;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 2 — REMINDER EMAILS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Runs every morning at 9 AM. Sends a "rent is due today" email to any
     * tenant whose invoice is due TODAY and is still unpaid.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendRentDueEmails() {
        LocalDate today = timeTravelService.getCurrentDate();
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);

        for (Lease lease : activeLeases) {
            // Skip if the lease is prepaid for the current period.
            if (isCoveredByPrepayment(lease, today)) {
                continue;
            }

            // Find this month's invoice for this lease.
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            Invoice currentInvoice = invoiceRepository.findByLease(lease).stream()
                    .filter(i -> i.getIssueDate().isAfter(monthStart.minusDays(1)) &&
                            i.getIssueDate().isBefore(monthEnd.plusDays(1)))
                    .filter(i -> i.getStatus() == InvoiceStatus.PENDING)
                    .findFirst()
                    .orElse(null);

            if (currentInvoice == null) {
                continue;   // no unpaid invoice this month — nothing to remind
            }

            // Only send if today IS the due date. Otherwise skip.
            if (!currentInvoice.getDueDate().equals(today)) {
                continue;
            }

            User user = userRepository.findById(lease.getTenant().getUserId()).orElse(null);
            if (user != null && user.getEmail() != null) {
                emailService.sendRentDueEmail(user, lease);
                log.info("Rent due email sent to {} for invoice {}",
                        user.getEmail(), currentInvoice.getInvoiceNumber());
            }
        }
    }

    /**
     * Runs every morning at 8:30 AM. Sends a "payment due soon" reminder
     * N days before the due date (N is configurable in Settings, default 3).
     */
    @Scheduled(cron = "0 30 8 * * ?")
    @Transactional
    public void sendUpcomingPaymentReminders() {
        LocalDate today = timeTravelService.getCurrentDate();
        int reminderDays = settingsService.getIntSetting("reminder_days_before", 3);
        LocalDate targetDueDate = today.plusDays(reminderDays);
        List<Invoice> pendingInvoices = invoiceRepository.findByStatus(InvoiceStatus.PENDING);

        for (Invoice invoice : pendingInvoices) {
            Lease lease = invoice.getLease();
            if (lease == null || !invoice.getDueDate().equals(targetDueDate)) {
                continue;   // not due within the reminder window
            }

            User user = userRepository.findById(lease.getTenant().getUserId()).orElse(null);
            if (user != null && user.getEmail() != null) {
                emailService.sendPaymentReminderEmail(user, invoice);
                notificationService.createForUserOnce(
                        user.getId(),
                        "PAYMENT_REMINDER",
                        invoice.getId(),
                        "Payment Due Soon",
                        "Invoice " + invoice.getInvoiceNumber() + " for ETB " + invoice.getTotalAmount() +
                                " is due on " + invoice.getDueDate() + ". Please pay to avoid late penalties.",
                        "/payments"
                );
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 3 — DAILY PENALTY ACCRUAL
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Runs automatically every night at 1 AM. Adds one day's worth of penalty
     * to every unpaid invoice that's past its due date.
     *
     * Note: SKIPS when time travel is active — the admin triggers the manual
     * version from the test panel instead.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void calculateDailyPenalties() {
        if (timeTravelService.isActive()) {
            return;
        }
        calculateDailyPenaltiesInternal();
    }

    /** Manual version — called from the test panel. */
    @Transactional
    public void simulateDailyPenalties() {
        calculateDailyPenaltiesInternal();
    }

    /**
     * The actual penalty accrual work.
     *
     * IMPORTANT: the query includes both PENDING and OVERDUE invoices.
     * An earlier version only looked at PENDING, which meant once an
     * invoice flipped to OVERDUE it dropped out of the query and
     * penalties stopped accruing after day 1. Fixed by including both.
     */
    private void calculateDailyPenaltiesInternal() {
        LocalDate today = timeTravelService.getCurrentDate();
        List<Invoice> candidates = invoiceRepository.findByStatusIn(
                List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE));

        for (Invoice invoice : candidates) {
            // Only touch invoices that are actually late.
            if (invoice.getDueDate().isBefore(today)) {

                // The first time we see a PENDING invoice go past its due date,
                // flip it to OVERDUE. On subsequent runs the status is already
                // OVERDUE so this is a no-op.
                if (invoice.getStatus() == InvoiceStatus.PENDING) {
                    invoice.setStatus(InvoiceStatus.OVERDUE);
                }

                // Add one day's penalty. Penalty formula is:
                //   (monthly penalty rate applied to untaxed rent) / 30.
                BigDecimal dailyPenalty = calculateDailyPenalty(invoice);
                BigDecimal newPenalty = invoice.getPenaltyAmount().add(dailyPenalty);
                invoice.setPenaltyAmount(newPenalty);
                invoice.setTotalAmount(invoice.getRentAmount().add(newPenalty));

                invoiceRepository.save(invoice);

                // Send the tenant a notification on the FIRST overdue day only.
                long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
                if (daysOverdue == 1) {
                    notifyOverdue(invoice);
                }
            }
        }
    }

    /**
     * One day's penalty for an invoice:
     *   monthly penalty (from TaxCalculationService) ÷ 30.
     *
     * Example: if rent is 10,000 and the penalty rate is 20%, then
     *   monthly penalty = (10,000 / 1.15) × 20% ≈ 1,739.13
     *   daily penalty  = 1,739.13 / 30 ≈ 57.97
     */
    private BigDecimal calculateDailyPenalty(Invoice invoice) {
        Tenant tenant = invoice.getLease().getTenant();
        BigDecimal rentInclusive = invoice.getRentAmount();
        LocalDate invoiceDate = invoice.getIssueDate();

        BigDecimal monthlyPenalty = taxCalculationService.calculatePenalty(tenant, rentInclusive, invoiceDate);
        return monthlyPenalty.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
    }

    /**
     * Sends the tenant a notification + email when an invoice first goes overdue.
     * Only called once per invoice (guarded by the daysOverdue == 1 check above).
     */
    private void notifyOverdue(Invoice invoice) {
        Tenant tenant = invoice.getLease().getTenant();

        notificationService.createForUserOnce(
                tenant.getUserId(),
                "INVOICE_OVERDUE",
                invoice.getId(),
                "Invoice overdue",
                "Invoice " + invoice.getInvoiceNumber() + " is overdue. Penalty is accumulating daily.",
                "/payments"
        );

        User user = userRepository.findById(tenant.getUserId()).orElse(null);
        if (user != null && user.getEmail() != null) {
            emailService.sendOverdueEmail(user, invoice);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 4 — HELPER METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /** Delegates to PaymentScheduleService — kept as a wrapper for readability. */
    private boolean isCoveredByPrepayment(Lease lease, LocalDate date) {
        return paymentScheduleService.isCoveredByPrepayment(lease, date);
    }

    /** True if this lease has ever had an invoice (used for the prorate check). */
    private boolean hasFirstInvoiceBeenGenerated(Lease lease) {
        return invoiceRepository.countByLease(lease) > 0;
    }

    /**
     * Prorate only when the invoice being generated is for the same month the
     * lease started in. A lease that started Sep 28 with 3 months prepaid must
     * NOT prorate its December invoice — December is a full month.
     */
    private boolean shouldProrateFirstInvoice(Lease lease, LocalDate billingDate) {
        LocalDate start = lease.getStartDate();
        return start.getDayOfMonth() > 15
                && start.getYear() == billingDate.getYear()
                && start.getMonth() == billingDate.getMonth();
    }

    /**
     * Calculates the prorated amount for a partial month.
     * Example: lease starts on the 20th of a 30-day month.
     *   Days remaining (including start day) = 11.
     *   Daily rate = monthly rent ÷ 30.
     *   Prorated rent = daily rate × 11.
     *
     * The month length MUST come from the lease's start month, not the billing
     * date's month: daysRemaining is derived from startDate.getDayOfMonth(), so
     * the denominator has to be the same month. (E.g. a Sep 24 start billed in
     * December used December's 31 days → wrong daily rate.)
     */
    private BigDecimal calculateProratedRent(Lease lease, LocalDate billingDate) {
        LocalDate startDate = lease.getStartDate();
        int daysInMonth = startDate.lengthOfMonth();
        int daysRemaining = daysInMonth - startDate.getDayOfMonth() + 1;

        BigDecimal dailyRate = lease.getMonthlyRent()
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Advances the lease's nextBillingDate by its billing frequency after
     * an invoice is generated. Example: a MONTHLY lease whose next billing
     * date was 2026-12-01 becomes 2027-01-01.
     */
    private void updateNextBillingDate(Lease lease) {
        LocalDate currentNext = lease.getNextBillingDate();
        if (currentNext == null) {
            lease.setNextBillingDate(lease.getStartDate().plusMonths(lease.getInitialPaymentMonths()));
        } else {
            int monthsToAdd = lease.getBillingFrequency().getMonths();
            lease.setNextBillingDate(currentNext.plusMonths(monthsToAdd));
        }
        leaseRepository.save(lease);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 5 — READ METHODS (used by controllers)
    // ═════════════════════════════════════════════════════════════════════════

    /** All invoices for a specific tenant (by tenant ID). */
    public List<Invoice> getInvoicesByTenant(Long tenantId) {
        return invoiceRepository.findByLeaseTenantId(tenantId);
    }

    /**
     * All invoices for the logged-in tenant. Resolves the username to a
     * tenant record first, then returns that tenant's invoices.
     */
    public List<Invoice> getInvoicesByTenantUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tenant found for user: " + username));
        return invoiceRepository.findByLeaseTenantId(tenant.getId());
    }

    /** Filter invoices by status (PENDING, PAID, OVERDUE, SUBMITTED). */
    public List<Invoice> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

    /** Every invoice in the system (admin only). */
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    /** Single invoice by ID — throws if not found. */
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));
    }

    /** Persist any changes to an invoice. */
    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 6 — MANUAL OPERATIONS (called from controllers / admin panel)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Admin-triggered version of invoice generation — creates ONE invoice
     * for the given lease. Used by the "Generate Invoice" button on the
     * leases page.
     */
    @Transactional
    public Invoice generateInvoice(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new IllegalArgumentException("Lease not found with id: " + leaseId));
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active. Cannot generate invoice.");
        }
        return generateInvoiceForLease(lease);
    }

    /** Public wrapper — lets external code trigger the overdue notification. */
    public void notifyInvoiceOverdue(Invoice invoice) {
        notifyOverdue(invoice);
    }

    /**
     * The total penalty an invoice would have if it were paid today.
     * Used for showing "you'll owe X if you pay now" previews.
     */
    public BigDecimal calculateTotalPenalty(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return invoice.getPenaltyAmount();   // paid — penalty is fixed
        }
        long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), timeTravelService.getCurrentDate());
        if (daysOverdue <= 0) {
            return invoice.getPenaltyAmount();   // not overdue yet
        }
        BigDecimal dailyPenalty = calculateDailyPenalty(invoice);
        return invoice.getPenaltyAmount().add(dailyPenalty.multiply(BigDecimal.valueOf(daysOverdue)));
    }

    /** True if the invoice is overdue (by status OR by past-due date). */
    public boolean isOverdue(Invoice invoice) {
        return invoice.getStatus() == InvoiceStatus.OVERDUE ||
                (invoice.getStatus() == InvoiceStatus.PENDING &&
                        invoice.getDueDate().isBefore(timeTravelService.getCurrentDate()));
    }

    /** How many days overdue (returns 0 for non-overdue invoices). */
    public long getDaysOverdue(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
            return 0;
        }
        return ChronoUnit.DAYS.between(invoice.getDueDate(), timeTravelService.getCurrentDate());
    }

    /**
     * A display-friendly status string. Adds one extra case that the raw
     * status doesn't have: if the invoice is PENDING but the due date has
     * passed, this returns "OVERDUE" so the UI can show it correctly even
     * before the daily job flips the status.
     */
    public String getInvoiceStatusDisplay(Invoice invoice) {
        LocalDate today = timeTravelService.getCurrentDate();
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return "PAID";
        }
        if (invoice.getStatus() == InvoiceStatus.OVERDUE) {
            return "OVERDUE";
        }
        if (invoice.getStatus() == InvoiceStatus.SUBMITTED) {
            return "SUBMITTED";
        }
        if (invoice.getStatus() == InvoiceStatus.PENDING) {
            if (invoice.getDueDate().isBefore(today)) {
                return "OVERDUE";
            }
            return "PENDING";
        }
        return invoice.getStatus().name();
    }

    /**
     * Record a prepayment — the tenant pays N months in advance.
     *
     * Validates:
     *   1. The lease exists and is ACTIVE.
     *   2. The amount paid is at least N × monthly rent (with 1% tolerance).
     *
     * Then delegates to PaymentScheduleService, which records the schedule
     * and generates the invoices for those months marked PAID.
     */
    @Transactional
    public PaymentSchedule processPrepayment(Long leaseId, Integer monthsToPay,
                                             BigDecimal amountPaid, LocalDate paymentDate) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new IllegalArgumentException("Lease not found"));

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active");
        }

        BigDecimal expectedAmount = lease.getMonthlyRent()
                .multiply(BigDecimal.valueOf(monthsToPay));

        BigDecimal tolerance = expectedAmount.multiply(BigDecimal.valueOf(0.01));
        if (amountPaid.compareTo(expectedAmount.subtract(tolerance)) < 0) {
            throw new IllegalArgumentException("Payment amount is less than expected");
        }

        return paymentScheduleService.createPaymentSchedule(lease, monthsToPay, amountPaid, paymentDate);
    }
}