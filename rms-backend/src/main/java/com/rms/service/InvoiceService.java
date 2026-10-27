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
 *   3. Sending reminder SMS/emails before/on/after due dates.
 *   4. Handling prepayments (when a tenant pays for several months upfront).
 *
 * Four things run automatically on a schedule (see @Scheduled annotations):
 *   - On the 1st of every month at midnight: generate invoices.
 *   - Every night at 1 AM: add a day's worth of penalty to overdue invoices.
 *   - Every morning at 9 AM: send "rent is due today" reminders.
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

    private final InvoiceRepository invoiceRepository;
    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SettingsService settingsService;
    private final TaxCalculationService taxCalculationService;
    private final PaymentScheduleService paymentScheduleService;
    private final TimeTravelService timeTravelService;
    private final InvoiceNumberService invoiceNumberService;
    private final SmsService smsService;

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
                          InvoiceNumberService invoiceNumberService,
                          SmsService smsService) {
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
        this.smsService = smsService;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 1 — MONTHLY INVOICE GENERATION
    // ═════════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void generateMonthlyInvoices() {
        if (timeTravelService.isActive()) {
            return;
        }
        generateMonthlyInvoicesInternal();
    }

    @Transactional
    public void simulateMonthlyInvoices() {
        generateMonthlyInvoicesInternal();
    }

    private void generateMonthlyInvoicesInternal() {
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);
        LocalDate today = timeTravelService.getCurrentDate();

        for (Lease lease : activeLeases) {
            LocalDate nextBillingDate = lease.getNextBillingDate();
            if (nextBillingDate != null && today.isBefore(nextBillingDate)) {
                continue;
            }
            if (paymentScheduleService.isCoveredByPrepayment(lease, today)) {
                continue;
            }
            generateInvoiceForLease(lease);
        }
    }

    @Transactional
    public Invoice generateInvoiceForLease(Lease lease) {
        if (lease == null) {
            throw new IllegalArgumentException("Lease cannot be null");
        }
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active. Cannot generate invoice.");
        }

        LocalDate today = timeTravelService.getCurrentDate();

        if (isCoveredByPrepayment(lease, today)) {
            return null;
        }

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        if (invoiceRepository.existsByLeaseAndIssueDateBetween(lease, monthStart, monthEnd)) {
            return null;
        }

        int dueDay = lease.getDueDay() != null ? lease.getDueDay() : 10;
        LocalDate dueDate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));
        // If the due day has already passed this month, the invoice would be born
        // overdue. Roll it to next month's due day so it starts life as PENDING.
        if (dueDate.isBefore(today)) {
            LocalDate nextMonth = today.plusMonths(1);
            dueDate = nextMonth.withDayOfMonth(Math.min(dueDay, nextMonth.lengthOfMonth()));
        }

        String invoiceNumber = invoiceNumberService.nextInvoiceNumber(today);

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

        updateNextBillingDate(lease);

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
    // SECTION 2 — REMINDERS (upcoming + due today)
    // ═════════════════════════════════════════════════════════════════════════

    /** Runs every morning at 9 AM — "rent is due today". */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendRentDueEmails() {
        if (timeTravelService.isActive()) {
            return;
        }
        sendDueRemindersInternal();
    }

    @Transactional
    public int simulateDueReminders() {
        return sendDueRemindersInternal();
    }

    private int sendDueRemindersInternal() {
        LocalDate today = timeTravelService.getCurrentDate();
        List<Invoice> pending = invoiceRepository.findByStatus(InvoiceStatus.PENDING);
        int sent = 0;

        for (Invoice invoice : pending) {
            if (invoice.getDueReminderSentOn() != null) continue;
            if (invoice.getLease() == null) continue;
            if (!invoice.getDueDate().equals(today)) continue;

            Tenant tenant = invoice.getLease().getTenant();
            if (tenant == null) continue;

            notificationService.createForUserOnce(
                    tenant.getUserId(),
                    "PAYMENT_DUE_TODAY",
                    invoice.getId(),
                    "Payment Due Today",
                    "Invoice " + invoice.getInvoiceNumber() + " for ETB " + invoice.getTotalAmount()
                            + " is due today. Pay now to avoid late penalties.",
                    "/payments"
            );

            String phone = tenant.getPhone();
            if (phone != null && !phone.isBlank()) {
                String msg = "RMS: Invoice " + invoice.getInvoiceNumber()
                        + " for ETB " + invoice.getTotalAmount()
                        + " is due TODAY (" + invoice.getDueDate()
                        + "). Pay now to avoid daily penalties.";
                try {
                    boolean ok = smsService.send(phone, msg);
                    log.info("Due-today SMS to {} for invoice {}: {}",
                            phone, invoice.getInvoiceNumber(), ok ? "sent" : "rejected");
                } catch (Exception e) {
                    log.error("Due-today SMS failed for invoice {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            }

            User user = userRepository.findById(tenant.getUserId()).orElse(null);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                try {
                    emailService.sendRentDueEmail(user, invoice.getLease());
                } catch (Exception e) {
                    log.error("Due-today email failed for invoice {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            } else if (tenant.getEmail() != null && !tenant.getEmail().isBlank()) {
                // Fallback: send to tenant email directly if no user account or user has no email
                try {
                    emailService.sendRentDueEmailToTenant(tenant, invoice);
                } catch (Exception e) {
                    log.error("Due-today email to tenant failed for invoice {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            }

            invoice.setDueReminderSentOn(today);
            invoiceRepository.save(invoice);
            sent++;
        }

        log.info("Due-today reminders sent: {}", sent);
        return sent;
    }

    /** Runs every morning at 8:30 AM — "payment due soon" (3 days before). */
    @Scheduled(cron = "0 30 8 * * ?")
    @Transactional
    public void sendUpcomingPaymentReminders() {
        if (timeTravelService.isActive()) {
            return;
        }
        sendUpcomingRemindersInternal();
    }

    @Transactional
    public int simulateUpcomingReminders() {
        return sendUpcomingRemindersInternal();
    }

    private int sendUpcomingRemindersInternal() {
        LocalDate today = timeTravelService.getCurrentDate();
        int reminderDays = settingsService.getIntSetting("reminder_days_before", 3);
        LocalDate targetDueDate = today.plusDays(reminderDays);

        List<Invoice> pending = invoiceRepository.findByStatus(InvoiceStatus.PENDING);
        int sent = 0;

        for (Invoice invoice : pending) {
            if (invoice.getUpcomingReminderSentOn() != null) continue;
            if (invoice.getLease() == null) continue;
            if (!invoice.getDueDate().equals(targetDueDate)) continue;

            Tenant tenant = invoice.getLease().getTenant();
            if (tenant == null) continue;

            notificationService.createForUserOnce(
                    tenant.getUserId(),
                    "PAYMENT_REMINDER",
                    invoice.getId(),
                    "Payment Due Soon",
                    "Invoice " + invoice.getInvoiceNumber() + " for ETB " + invoice.getTotalAmount()
                            + " is due on " + invoice.getDueDate() + ". Please pay to avoid late penalties.",
                    "/payments"
            );

            String phone = tenant.getPhone();
            if (phone != null && !phone.isBlank()) {
                String msg = "RMS: Invoice " + invoice.getInvoiceNumber()
                        + " for ETB " + invoice.getTotalAmount()
                        + " is due on " + invoice.getDueDate()
                        + ". Please pay to avoid penalties.";
                try {
                    boolean ok = smsService.send(phone, msg);
                    log.info("Upcoming-reminder SMS to {} for invoice {}: {}",
                            phone, invoice.getInvoiceNumber(), ok ? "sent" : "rejected");
                } catch (Exception e) {
                    log.error("Upcoming-reminder SMS failed for invoice {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            }

            User user = userRepository.findById(tenant.getUserId()).orElse(null);
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                try {
                    emailService.sendPaymentReminderEmail(user, invoice);
                } catch (Exception e) {
                    log.error("Upcoming-reminder email failed for invoice {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            } else if (tenant.getEmail() != null && !tenant.getEmail().isBlank()) {
                // Fallback: send to tenant email directly if no user account or user has no email
                try {
                    emailService.sendPaymentReminderEmailToTenant(tenant, invoice);
                } catch (Exception e) {
                    log.error("Upcoming-reminder email to tenant failed for invoice {}: {}",
                            invoice.getInvoiceNumber(), e.getMessage());
                }
            }

            invoice.setUpcomingReminderSentOn(today);
            invoiceRepository.save(invoice);
            sent++;
        }

        log.info("Upcoming reminders sent (targetDueDate={}): {}", targetDueDate, sent);
        return sent;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 3 — DAILY PENALTY ACCRUAL + OVERDUE NOTIFICATION
    // ═════════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void calculateDailyPenalties() {
        if (timeTravelService.isActive()) {
            return;
        }
        calculateDailyPenaltiesInternal();
    }

    @Transactional
    public void simulateDailyPenalties() {
        calculateDailyPenaltiesInternal();
    }

    /**
     * Walks every PENDING or OVERDUE invoice past its due date and:
     *   1. Flips PENDING → OVERDUE the first time.
     *   2. Adds one day's penalty.
     *   3. Fires the overdue notification (SMS + email + in-app) once,
     *      guarded by the overdueReminderSentOn marker on the invoice itself.
     *      This is more robust than the old `daysOverdue == 1` check, which
     *      silently skipped the notification if virtual time jumped past
     *      the first day.
     */
    private void calculateDailyPenaltiesInternal() {
        LocalDate today = timeTravelService.getCurrentDate();
        List<Invoice> candidates = invoiceRepository.findByStatusIn(
                List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE));

        for (Invoice invoice : candidates) {
            if (invoice.getDueDate().isBefore(today)) {

                if (invoice.getStatus() == InvoiceStatus.PENDING) {
                    invoice.setStatus(InvoiceStatus.OVERDUE);
                }

                BigDecimal dailyPenalty = calculateDailyPenalty(invoice);
                BigDecimal newPenalty = invoice.getPenaltyAmount().add(dailyPenalty);
                invoice.setPenaltyAmount(newPenalty);
                invoice.setTotalAmount(invoice.getRentAmount().add(newPenalty));

                // Fire the overdue notification exactly once — the first
                // time we see this invoice in overdue state.
                if (invoice.getOverdueReminderSentOn() == null) {
                    notifyOverdue(invoice);
                    invoice.setOverdueReminderSentOn(today);
                }

                invoiceRepository.save(invoice);
            }
        }
    }

    private BigDecimal calculateDailyPenalty(Invoice invoice) {
        Tenant tenant = invoice.getLease().getTenant();
        BigDecimal rentInclusive = invoice.getRentAmount();
        LocalDate invoiceDate = invoice.getIssueDate();

        BigDecimal monthlyPenalty = taxCalculationService.calculatePenalty(tenant, rentInclusive, invoiceDate);
        return monthlyPenalty.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
    }

    /**
     * Sends the tenant an in-app notification + email + SMS when an invoice
     * first goes overdue. Caller is responsible for setting the idempotency
     * marker (overdueReminderSentOn) afterwards.
     */
    private void notifyOverdue(Invoice invoice) {
        log.info("notifyOverdue: starting for invoice {}", invoice.getInvoiceNumber());

        Tenant tenant = invoice.getLease().getTenant();
        if (tenant == null) {
            log.error("notifyOverdue: no tenant on invoice {}", invoice.getInvoiceNumber());
            return;
        }

        log.info("notifyOverdue: tenant id={} phone={} userId={}",
                tenant.getId(), tenant.getPhone(), tenant.getUserId());

        // In-app notification
        try {
            notificationService.createForUserOnce(
                    tenant.getUserId(),
                    "INVOICE_OVERDUE",
                    invoice.getId(),
                    "Invoice overdue",
                    "Invoice " + invoice.getInvoiceNumber() + " is overdue. Penalty is accumulating daily.",
                    "/payments"
            );
            log.info("notifyOverdue: in-app notification ok");
        } catch (Exception e) {
            log.error("notifyOverdue: in-app notification failed: {}", e.getMessage(), e);
        }

        // Email
        User user = userRepository.findById(tenant.getUserId()).orElse(null);
        if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendOverdueEmail(user, invoice);
                log.info("notifyOverdue: email sent to {}", user.getEmail());
            } catch (Exception e) {
                log.error("notifyOverdue: email failed: {}", e.getMessage(), e);
            }
        } else if (tenant.getEmail() != null && !tenant.getEmail().isBlank()) {
            // Fallback: send to tenant email directly if no user account or user has no email
            try {
                emailService.sendOverdueEmailToTenant(tenant, invoice);
                log.info("notifyOverdue: email sent to tenant {}", tenant.getEmail());
            } catch (Exception e) {
                log.error("notifyOverdue: email to tenant failed: {}", e.getMessage(), e);
            }
        } else {
            log.info("notifyOverdue: skipping email (no email available)");
        }

        // SMS
        String phone = tenant.getPhone();
        if (phone == null || phone.isBlank()) {
            log.warn("notifyOverdue: tenant has no phone, skipping SMS");
            return;
        }

        String normalized = SmsService.normalizePhone(phone);
        if (normalized == null) {
            log.warn("notifyOverdue: phone '{}' failed normalization (expected 09XXXXXXXX / 07XXXXXXXX)", phone);
            return;
        }

        String msg = "RMS: Invoice " + invoice.getInvoiceNumber()
                + " for ETB " + invoice.getTotalAmount()
                + " is OVERDUE (was due " + invoice.getDueDate()
                + "). Penalty is accumulating daily. Please pay now.";

        try {
            log.info("notifyOverdue: sending SMS to {} via AfroMessage", normalized);
            boolean ok = smsService.send(phone, msg);
            log.info("notifyOverdue: SMS result = {}", ok);
        } catch (Exception e) {
            log.error("notifyOverdue: SMS failed: {}", e.getMessage(), e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 4 — HELPER METHODS
    // ═════════════════════════════════════════════════════════════════════════

    private boolean isCoveredByPrepayment(Lease lease, LocalDate date) {
        return paymentScheduleService.isCoveredByPrepayment(lease, date);
    }

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

    private BigDecimal calculateProratedRent(Lease lease, LocalDate billingDate) {
        LocalDate startDate = lease.getStartDate();
        int daysInMonth = startDate.lengthOfMonth();
        int daysRemaining = daysInMonth - startDate.getDayOfMonth() + 1;

        BigDecimal dailyRate = lease.getMonthlyRent()
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, RoundingMode.HALF_UP);
    }

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
    // SECTION 5 — READ METHODS
    // ═════════════════════════════════════════════════════════════════════════

    public List<Invoice> getInvoicesByTenant(Long tenantId) {
        return invoiceRepository.findByLeaseTenantId(tenantId);
    }

    public List<Invoice> getInvoicesByTenantUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tenant found for user: " + username));
        return invoiceRepository.findByLeaseTenantId(tenant.getId());
    }

    public List<Invoice> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + id));
    }

    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SECTION 6 — MANUAL OPERATIONS
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public Invoice generateInvoice(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new IllegalArgumentException("Lease not found with id: " + leaseId));
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active. Cannot generate invoice.");
        }
        return generateInvoiceForLease(lease);
    }

    /** Public wrapper — used by InvoiceController.testMarkOverdue/forceOverdue. */
    public void notifyInvoiceOverdue(Invoice invoice) {
        notifyOverdue(invoice);
    }

    public BigDecimal calculateTotalPenalty(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return invoice.getPenaltyAmount();
        }
        long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), timeTravelService.getCurrentDate());
        if (daysOverdue <= 0) {
            return invoice.getPenaltyAmount();
        }
        BigDecimal dailyPenalty = calculateDailyPenalty(invoice);
        return invoice.getPenaltyAmount().add(dailyPenalty.multiply(BigDecimal.valueOf(daysOverdue)));
    }

    public boolean isOverdue(Invoice invoice) {
        return invoice.getStatus() == InvoiceStatus.OVERDUE ||
                (invoice.getStatus() == InvoiceStatus.PENDING &&
                        invoice.getDueDate().isBefore(timeTravelService.getCurrentDate()));
    }

    public long getDaysOverdue(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
            return 0;
        }
        return ChronoUnit.DAYS.between(invoice.getDueDate(), timeTravelService.getCurrentDate());
    }

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