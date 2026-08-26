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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
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
    private final SettingsService settingsService;

    public InvoiceService(InvoiceRepository invoiceRepository, LeaseRepository leaseRepository,
                          TenantRepository tenantRepository, UserRepository userRepository,
                          NotificationService notificationService, EmailService emailService,
                          SettingsService settingsService) {
        this.invoiceRepository = invoiceRepository;
        this.leaseRepository = leaseRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.settingsService = settingsService;
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void generateMonthlyInvoices() {
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);
        LocalDate today = LocalDate.now();

        for (Lease lease : activeLeases) {
            LocalDate nextBilling = lease.getNextBillingDate();

            if (nextBilling == null) {
                initializeNextBillingDate(lease);
                nextBilling = lease.getNextBillingDate();
            }

            if (nextBilling != null && (nextBilling.isEqual(today) || nextBilling.isBefore(today))) {
                try {
                    generateInvoiceForLease(lease);
                } catch (OptimisticLockingFailureException e) {
                    System.err.println("Failed to generate invoice for lease " + lease.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    @Transactional
    public Invoice generateInvoiceForLease(Lease lease) {
        LocalDate today = LocalDate.now();

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

        String invoiceNumber = generateInvoiceNumber();

        BigDecimal rentAmount = lease.getMonthlyRent();
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

        try {
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
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalArgumentException("Invoice generation failed due to concurrent update. Please try again.");
        }
    }

    @Transactional
    public Invoice generateInvoice(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new IllegalArgumentException("Lease not found"));

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active");
        }

        return generateInvoiceForLease(lease);
    }

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void markOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> pendingInvoices = invoiceRepository.findByStatus(InvoiceStatus.PENDING);
        int penaltyRate = settingsService.getIntSetting("penalty_rate", 20);

        for (Invoice invoice : pendingInvoices) {
            if (invoice.getDueDate().isBefore(today)) {
                if (!isValidInvoiceStatusTransition(invoice.getStatus(), InvoiceStatus.OVERDUE)) {
                    continue;
                }

                try {
                    invoice.setStatus(InvoiceStatus.OVERDUE);

                    if (invoice.getPenaltyAmount().compareTo(BigDecimal.ZERO) == 0) {
                        BigDecimal penalty = invoice.getRentAmount()
                                .multiply(BigDecimal.valueOf(penaltyRate))
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        invoice.setPenaltyAmount(penalty);
                        invoice.setTotalAmount(invoice.getRentAmount().add(penalty));
                    }

                    invoiceRepository.save(invoice);
                    notifyInvoiceOverdue(invoice);

                    User user = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
                    if (user != null && user.getEmail() != null) {
                        emailService.sendOverdueEmail(user, invoice);
                    }
                } catch (OptimisticLockingFailureException e) {
                    System.err.println("Failed to mark invoice " + invoice.getId() + " as overdue: " + e.getMessage());
                }
            }
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendRentDueReminders() {
        LocalDate today = LocalDate.now();
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);

        for (Lease lease : activeLeases) {
            if (!isCoveredByPrepayment(lease, today)) {
                LocalDate nextBilling = lease.getNextBillingDate();
                if (nextBilling == null) {
                    initializeNextBillingDate(lease);
                    nextBilling = lease.getNextBillingDate();
                }

                if (nextBilling != null && today.equals(nextBilling)) {
                    User user = userRepository.findById(lease.getTenant().getUserId()).orElse(null);
                    if (user != null && user.getEmail() != null) {
                        emailService.sendRentDueEmail(user, lease);
                        notificationService.createForUserOnce(
                                user.getId(),
                                "RENT_DUE_TODAY",
                                lease.getId(),
                                "Rent Due Today",
                                "Rent for " + lease.getSpace().getUnitNumber() +
                                        " (ETB " + lease.getMonthlyRent() + ") is due today. Please pay to avoid penalties.",
                                "/payments"
                        );
                    }
                }
            }
        }
    }

    private boolean isCoveredByPrepayment(Lease lease, LocalDate date) {
        LocalDate start = lease.getStartDate();
        LocalDate prepaymentEnd = start.plusMonths(lease.getPrepaymentMonths());
        return date.isBefore(prepaymentEnd) || date.isEqual(prepaymentEnd);
    }

    public void notifyInvoiceOverdue(Invoice invoice) {
        Tenant tenant = invoice.getLease().getTenant();
        notificationService.createForUserOnce(
                tenant.getUserId(),
                "INVOICE_OVERDUE",
                invoice.getId(),
                "Invoice overdue",
                "Invoice " + invoice.getInvoiceNumber() + " is overdue. Please pay as soon as possible.",
                "/payments"
        );
    }

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
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
    }

    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    public long getDaysOverdue(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
            return 0;
        }
        return ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
    }

    public boolean isOverdue(Invoice invoice) {
        return invoice.getStatus() == InvoiceStatus.OVERDUE ||
                (invoice.getStatus() == InvoiceStatus.PENDING && invoice.getDueDate().isBefore(LocalDate.now()));
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(Year.now().getValue());
        long count = invoiceRepository.count() + 1;
        String seq = String.format("%04d", count);
        return "INV-" + year + "-" + seq;
    }

    private void initializeNextBillingDate(Lease lease) {
        LocalDate start = lease.getStartDate();
        LocalDate prepaymentEnd = start.plusMonths(lease.getPrepaymentMonths());
        lease.setNextBillingDate(prepaymentEnd);
        leaseRepository.save(lease);
    }

    private void updateNextBillingDate(Lease lease) {
        LocalDate currentNext = lease.getNextBillingDate();
        if (currentNext == null) {
            initializeNextBillingDate(lease);
            return;
        }

        int monthsToAdd = lease.getBillingFrequency().getMonths();
        LocalDate nextBilling = currentNext.plusMonths(monthsToAdd);

        lease.setNextBillingDate(nextBilling);
        leaseRepository.save(lease);
    }

    private boolean hasFirstInvoiceBeenGenerated(Lease lease) {
        return invoiceRepository.countByLease(lease) > 0;
    }

    private boolean shouldProrateFirstInvoice(Lease lease) {
        LocalDate startDate = lease.getStartDate();
        return startDate.getDayOfMonth() > 15;
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