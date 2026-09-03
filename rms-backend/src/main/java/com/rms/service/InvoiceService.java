package com.rms.service;

import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.Lease;
import com.rms.entity.LeaseStatus;
import com.rms.entity.Tenant;
import com.rms.entity.User;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.LeaseRepository;
import com.rms.repository.TenantRepository;
import com.rms.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final SettingsService settingsService;
    private final TaxCalculationService taxCalculationService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          LeaseRepository leaseRepository,
                          TenantRepository tenantRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          EmailService emailService,
                          SmsService smsService,
                          SettingsService settingsService,
                          TaxCalculationService taxCalculationService) {
        this.invoiceRepository = invoiceRepository;
        this.leaseRepository = leaseRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.settingsService = settingsService;
        this.taxCalculationService = taxCalculationService;
    }

    // ─── GENERATE INVOICES ON 1ST ───
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void generateMonthlyInvoices() {
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);
        for (Lease lease : activeLeases) {
            generateInvoiceForLease(lease);
        }
    }

    @Transactional
    public Invoice generateInvoiceForLease(Lease lease) {
        LocalDate today = LocalDate.now();

        // Skip if still in prepayment period
        if (isCoveredByPrepayment(lease, today)) {
            return null;
        }

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        // Check if invoice already exists for this month
        if (invoiceRepository.existsByLeaseAndIssueDateBetween(lease, monthStart, monthEnd)) {
            return null;
        }

        int dueDay = lease.getDueDay() != null ? lease.getDueDay() : 10;
        LocalDate dueDate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));

        String invoiceNumber = generateInvoiceNumber();

        BigDecimal rentAmount = lease.getMonthlyRent();
        
        // Handle prorated first invoice
        if (Boolean.TRUE.equals(lease.getFirstInvoiceProrated()) && !hasFirstInvoiceBeenGenerated(lease)) {
            if (shouldProrateFirstInvoice(lease)) {
                rentAmount = calculateProratedRent(lease, today);
            }
            lease.setFirstInvoiceProrated(false);
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

        // Send notifications
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

    // ─── SEND SMS ON 25TH ───
    @Scheduled(cron = "0 0 8 25 * ?")
    @Transactional
    public void sendRentDueRemindersSms() {
        LocalDate today = LocalDate.now();
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);

        for (Lease lease : activeLeases) {
            if (isCoveredByPrepayment(lease, today)) {
                continue;
            }

            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
            
            Invoice currentInvoice = invoiceRepository.findByLease(lease).stream()
                    .filter(i -> i.getIssueDate().isAfter(monthStart.minusDays(1)) &&
                            i.getIssueDate().isBefore(monthEnd.plusDays(1)))
                    .filter(i -> i.getStatus() == InvoiceStatus.PENDING)
                    .findFirst()
                    .orElse(null);

            if (currentInvoice == null) {
                continue;
            }

            Tenant tenant = lease.getTenant();
            if (tenant.getPhone() != null && !tenant.getPhone().isEmpty()) {
                int daysUntilDue = (int) ChronoUnit.DAYS.between(today, currentInvoice.getDueDate());
                if (daysUntilDue > 0) {
                    smsService.sendRentReminderSms(tenant, currentInvoice, daysUntilDue);
                    System.out.println("📱 SMS reminder sent to " + tenant.getPhone() + 
                            " for " + currentInvoice.getInvoiceNumber());
                }
            }
        }
    }

    // ─── SEND RENT DUE EMAIL ON DUE DAY ───
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendRentDueEmails() {
        LocalDate today = LocalDate.now();
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);

        for (Lease lease : activeLeases) {
            if (isCoveredByPrepayment(lease, today)) {
                continue;
            }

            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            Invoice currentInvoice = invoiceRepository.findByLease(lease).stream()
                    .filter(i -> i.getIssueDate().isAfter(monthStart.minusDays(1)) &&
                            i.getIssueDate().isBefore(monthEnd.plusDays(1)))
                    .filter(i -> i.getStatus() == InvoiceStatus.PENDING)
                    .findFirst()
                    .orElse(null);

            if (currentInvoice == null) {
                continue;
            }

            // Send email only on the due date
            if (!currentInvoice.getDueDate().equals(today)) {
                continue;
            }

            User user = userRepository.findById(lease.getTenant().getUserId()).orElse(null);
            if (user != null && user.getEmail() != null) {
                emailService.sendRentDueEmail(user, lease);
                System.out.println("📧 Rent due email sent to " + user.getEmail() +
                        " for " + currentInvoice.getInvoiceNumber());
            }
        }
    }

    // ─── DAILY PENALTY CALCULATION ───
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void calculateDailyPenalties() {
        LocalDate today = LocalDate.now();
        List<Invoice> pendingInvoices = invoiceRepository.findByStatus(InvoiceStatus.PENDING);

        for (Invoice invoice : pendingInvoices) {
            if (invoice.getDueDate().isBefore(today)) {
                // Mark as OVERDUE
                invoice.setStatus(InvoiceStatus.OVERDUE);

                // Calculate daily penalty
                BigDecimal dailyPenalty = calculateDailyPenalty(invoice);
                BigDecimal newPenalty = invoice.getPenaltyAmount().add(dailyPenalty);
                invoice.setPenaltyAmount(newPenalty);
                invoice.setTotalAmount(invoice.getRentAmount().add(newPenalty));

                invoiceRepository.save(invoice);

                // Send notification on first overdue day
                long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
                if (daysOverdue == 1) {
                    notifyOverdue(invoice);
                }
            }
        }
    }

    // ─── CALCULATE DAILY PENALTY ───
    private BigDecimal calculateDailyPenalty(Invoice invoice) {
        Tenant tenant = invoice.getLease().getTenant();
        BigDecimal rentInclusive = invoice.getRentAmount();
        LocalDate invoiceDate = invoice.getIssueDate();

        // Calculate monthly penalty using tax service
        BigDecimal monthlyPenalty = taxCalculationService.calculatePenalty(tenant, rentInclusive, invoiceDate);

        // Daily penalty = monthly penalty / 30
        return monthlyPenalty.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
    }

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

        if (tenant.getPhone() != null && !tenant.getPhone().isEmpty()) {
            BigDecimal dailyPenalty = calculateDailyPenalty(invoice);
            smsService.sendOverdueSms(tenant, invoice, 1, dailyPenalty.doubleValue());
        }
    }

    // ─── UPCOMING PAYMENT REMINDERS ───
    @Scheduled(cron = "0 30 8 * * ?")
    @Transactional
    public void sendUpcomingPaymentReminders() {
        LocalDate today = LocalDate.now();
        int reminderDays = settingsService.getIntSetting("reminder_days_before", 3);
        LocalDate targetDueDate = today.plusDays(reminderDays);
        List<Invoice> pendingInvoices = invoiceRepository.findByStatus(InvoiceStatus.PENDING);

        for (Invoice invoice : pendingInvoices) {
            Lease lease = invoice.getLease();
            if (lease == null || !invoice.getDueDate().equals(targetDueDate)) {
                continue;
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

    // ─── HELPER METHODS ───
    private boolean isCoveredByPrepayment(Lease lease, LocalDate date) {
        LocalDate start = lease.getStartDate();
        LocalDate prepaymentEnd = start.plusMonths(lease.getPrepaymentMonths());
        return date.isBefore(prepaymentEnd) || date.isEqual(prepaymentEnd);
    }

    private boolean hasFirstInvoiceBeenGenerated(Lease lease) {
        return invoiceRepository.countByLease(lease) > 0;
    }

    private boolean shouldProrateFirstInvoice(Lease lease) {
        return lease.getStartDate().getDayOfMonth() > 15;
    }

    private BigDecimal calculateProratedRent(Lease lease, LocalDate billingDate) {
        LocalDate startDate = lease.getStartDate();
        int daysInMonth = billingDate.lengthOfMonth();
        int daysRemaining = daysInMonth - startDate.getDayOfMonth() + 1;

        BigDecimal dailyRate = lease.getMonthlyRent()
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        long count = invoiceRepository.count() + 1;
        String seq = String.format("%04d", count);
        return "INV-" + year + "-" + seq;
    }

    private void updateNextBillingDate(Lease lease) {
        LocalDate currentNext = lease.getNextBillingDate();
        if (currentNext == null) {
            lease.setNextBillingDate(lease.getStartDate().plusMonths(lease.getPrepaymentMonths()));
        } else {
            int monthsToAdd = lease.getBillingFrequency().getMonths();
            lease.setNextBillingDate(currentNext.plusMonths(monthsToAdd));
        }
        leaseRepository.save(lease);
    }

    // ─── PUBLIC METHODS ───

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

    @Transactional
    public Invoice generateInvoice(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new IllegalArgumentException("Lease not found with id: " + leaseId));
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active. Cannot generate invoice.");
        }
        return generateInvoiceForLease(lease);
    }

    public void notifyInvoiceOverdue(Invoice invoice) {
        notifyOverdue(invoice);
    }

    public BigDecimal calculateTotalPenalty(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return invoice.getPenaltyAmount();
        }
        long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
        if (daysOverdue <= 0) {
            return invoice.getPenaltyAmount();
        }
        BigDecimal dailyPenalty = calculateDailyPenalty(invoice);
        return invoice.getPenaltyAmount().add(dailyPenalty.multiply(BigDecimal.valueOf(daysOverdue)));
    }

    public boolean isOverdue(Invoice invoice) {
        return invoice.getStatus() == InvoiceStatus.OVERDUE ||
                (invoice.getStatus() == InvoiceStatus.PENDING && 
                 invoice.getDueDate().isBefore(LocalDate.now()));
    }

    public long getDaysOverdue(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
            return 0;
        }
        return ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
    }

    public String getInvoiceStatusDisplay(Invoice invoice) {
        LocalDate today = LocalDate.now();
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

    private boolean isValidInvoiceStatusTransition(InvoiceStatus from, InvoiceStatus to) {
        switch (from) {
            case PENDING:
                return to == InvoiceStatus.SUBMITTED || to == InvoiceStatus.OVERDUE;
            case SUBMITTED:
                return to == InvoiceStatus.PAID;
            case OVERDUE:
                return to == InvoiceStatus.SUBMITTED || to == InvoiceStatus.PAID;
            case PAID:
                return false;
            default:
                return false;
        }
    }
}