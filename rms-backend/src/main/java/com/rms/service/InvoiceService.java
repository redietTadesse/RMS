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
<<<<<<< HEAD
import java.time.temporal.ChronoUnit;
=======
>>>>>>> abc52cb (Resolve merge conflicts)
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
<<<<<<< HEAD
    private final SettingsService settingsService;

    public InvoiceService(InvoiceRepository invoiceRepository, LeaseRepository leaseRepository,
                          TenantRepository tenantRepository, UserRepository userRepository,
                          NotificationService notificationService, EmailService emailService,
                          SettingsService settingsService) {
=======

    public InvoiceService(InvoiceRepository invoiceRepository, LeaseRepository leaseRepository,
                          TenantRepository tenantRepository, UserRepository userRepository,
                          NotificationService notificationService, EmailService emailService) {
>>>>>>> abc52cb (Resolve merge conflicts)
        this.invoiceRepository = invoiceRepository;
        this.leaseRepository = leaseRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
<<<<<<< HEAD
        this.settingsService = settingsService;
    }

=======
    }

    
>>>>>>> abc52cb (Resolve merge conflicts)
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void generateMonthlyInvoices() {
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);
        for (Lease lease : activeLeases) {
            // Only generate invoice if nextBillingDate is today or null (initialize)
            LocalDate today = LocalDate.now();
            LocalDate nextBilling = lease.getNextBillingDate();
            
            if (nextBilling == null) {
                // Initialize nextBillingDate based on lease start date and billing frequency
                initializeNextBillingDate(lease);
                nextBilling = lease.getNextBillingDate();
            }
            
            if (nextBilling != null && (nextBilling.isEqual(today) || nextBilling.isBefore(today))) {
                generateInvoiceForLease(lease);
            }
        }
    }

<<<<<<< HEAD
=======

>>>>>>> abc52cb (Resolve merge conflicts)
    @Transactional
    public Invoice generateInvoiceForLease(Lease lease) {
        LocalDate today = LocalDate.now();
        
        // Check if covered by prepayment period
        if (isCoveredByPrepayment(lease, today)) {
            System.out.println("Lease " + lease.getId() + " is in prepayment period, skipping invoice generation");
            return null;
        }

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

<<<<<<< HEAD
=======
        // CHECK FIRST: Does invoice already exist for this month?
>>>>>>> abc52cb (Resolve merge conflicts)
        if (invoiceRepository.existsByLeaseAndIssueDateBetween(lease, monthStart, monthEnd)) {
            System.out.println("Invoice already exists for lease " + lease.getId() + " this month");
            return null;
        }

<<<<<<< HEAD
        int dueDay = lease.getDueDay() != null ? lease.getDueDay() : 10;
        LocalDate dueDate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));

=======
        // Generate invoice number
>>>>>>> abc52cb (Resolve merge conflicts)
        String invoiceNumber = generateInvoiceNumber();

        // Calculate rent amount - prorate if this is first invoice and lease started after 15th
        BigDecimal rentAmount = lease.getMonthlyRent();
        if (Boolean.TRUE.equals(lease.getFirstInvoiceProrated()) && !hasFirstInvoiceBeenGenerated(lease)) {
            // Check if lease started after 15th - if so, prorate; otherwise charge full month
            if (shouldProrateFirstInvoice(lease)) {
                rentAmount = calculateProratedRent(lease, today);
            }
            // Mark that first invoice (prorated or full) has been generated
            lease.setFirstInvoiceProrated(false);
            leaseRepository.save(lease);
        }

        // Create invoice
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

<<<<<<< HEAD
        Invoice saved = invoiceRepository.save(invoice);

        // Update nextBillingDate based on billing frequency
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

=======
        // Save
        Invoice saved = invoiceRepository.save(invoice);

        // Send notifications
        User user = userRepository.findById(lease.getTenant().getUserId()).orElse(null);
        if (user != null) {
            // In-app notification
            notificationService.createForUserOnce(
                    user.getId(),
                    "INVOICE_GENERATED",
                    saved.getId(),
                    "New Invoice Generated",
                    "Invoice " + saved.getInvoiceNumber() + " for ETB " + saved.getTotalAmount() + " is due on " + saved.getDueDate(),
                    "/payments"
            );
            // Email notification
            emailService.sendInvoiceGeneratedEmail(user, saved, lease);
        }

        return saved;
    }
    
>>>>>>> abc52cb (Resolve merge conflicts)
    @Transactional
    public Invoice generateInvoice(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new IllegalArgumentException("Lease not found"));

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active");
        }

        return generateInvoiceForLease(lease);
    }

<<<<<<< HEAD
=======
    
>>>>>>> abc52cb (Resolve merge conflicts)
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void markOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> pendingInvoices = invoiceRepository.findByStatus(InvoiceStatus.PENDING);

        int penaltyRate = settingsService.getIntSetting("penalty_rate", 20);

        for (Invoice invoice : pendingInvoices) {
            if (invoice.getDueDate().isBefore(today)) {
                // Validate status transition
                if (!isValidInvoiceStatusTransition(invoice.getStatus(), InvoiceStatus.OVERDUE)) {
                    System.err.println("Invalid status transition from " + invoice.getStatus() + " to OVERDUE for invoice " + invoice.getInvoiceNumber());
                    continue;
                }

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
<<<<<<< HEAD

                User user = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
                if (user != null && user.getEmail() != null) {
                    emailService.sendOverdueEmail(user, invoice);
                }
=======
>>>>>>> abc52cb (Resolve merge conflicts)
            }
        }
    }

<<<<<<< HEAD
    @Scheduled(cron = "0 0 8 * * ?")  // Runs at 8:00 AM every day
    @Transactional
    public void sendRentDueReminders() {
        LocalDate today = LocalDate.now();
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);

        for (Lease lease : activeLeases) {
            // Check if this lease is NOT in prepayment period
            if (!isCoveredByPrepayment(lease, today)) {
                // Check if rent is due today based on nextBillingDate
                LocalDate nextBilling = lease.getNextBillingDate();
                if (nextBilling == null) {
                    initializeNextBillingDate(lease);
                    nextBilling = lease.getNextBillingDate();
                }
                
                // Only send if today is the billing date
                if (nextBilling != null && today.equals(nextBilling)) {
                    User user = userRepository.findById(lease.getTenant().getUserId()).orElse(null);
                    if (user != null && user.getEmail() != null) {
                        emailService.sendRentDueEmail(user, lease);

                        // Also send in-app notification
                        notificationService.createForUserOnce(
                                user.getId(),
                                "RENT_DUE_TODAY",
                                lease.getId(),
                                "🚨 Rent Due Today",
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

=======
    
>>>>>>> abc52cb (Resolve merge conflicts)
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

<<<<<<< HEAD
=======
    
>>>>>>> abc52cb (Resolve merge conflicts)
    public List<Invoice> getInvoicesByTenant(Long tenantId) {
        return invoiceRepository.findByLeaseTenantId(tenantId);
    }

<<<<<<< HEAD
=======
    
>>>>>>> abc52cb (Resolve merge conflicts)
    public List<Invoice> getInvoicesByTenantUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tenant found for user: " + username));

        return invoiceRepository.findByLeaseTenantId(tenant.getId());
    }

<<<<<<< HEAD
=======
    
>>>>>>> abc52cb (Resolve merge conflicts)
    public List<Invoice> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

<<<<<<< HEAD
=======
    
>>>>>>> abc52cb (Resolve merge conflicts)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

<<<<<<< HEAD
=======
    
>>>>>>> abc52cb (Resolve merge conflicts)
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
    }

<<<<<<< HEAD
=======
    
>>>>>>> abc52cb (Resolve merge conflicts)
    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

<<<<<<< HEAD
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

=======
    
>>>>>>> abc52cb (Resolve merge conflicts)
    private String generateInvoiceNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        long count = invoiceRepository.count() + 1;
        String seq = String.format("%04d", count);
        return "INV-" + year + "-" + seq;
    }
<<<<<<< HEAD

    private void initializeNextBillingDate(Lease lease) {
        LocalDate start = lease.getStartDate();
        LocalDate prepaymentEnd = start.plusMonths(lease.getPrepaymentMonths());
        LocalDate nextBilling = prepaymentEnd;
        
        lease.setNextBillingDate(nextBilling);
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
        // Check if any invoice exists for this lease
        return invoiceRepository.countByLease(lease) > 0;
    }

    private boolean shouldProrateFirstInvoice(Lease lease) {
        LocalDate startDate = lease.getStartDate();
        // If lease starts on or before the 15th, treat as full month (no proration)
        // If lease starts after the 15th, prorate for remaining days
        return startDate.getDayOfMonth() > 15;
    }

    private BigDecimal calculateProratedRent(Lease lease, LocalDate billingDate) {
        LocalDate startDate = lease.getStartDate();
        int daysInMonth = billingDate.lengthOfMonth();
        int daysRemaining = daysInMonth - startDate.getDayOfMonth() + 1;
        
        // Calculate daily rate based on actual days in the month
        BigDecimal dailyRate = lease.getMonthlyRent()
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        
        // Calculate prorated amount for remaining days
        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isValidInvoiceStatusTransition(InvoiceStatus from, InvoiceStatus to) {
        // Valid transitions:
        // PENDING -> SUBMITTED (tenant submits payment)
        // PENDING -> OVERDUE (scheduled task marks overdue)
        // SUBMITTED -> PAID (admin approves payment)
        // OVERDUE -> SUBMITTED (tenant submits late payment)
        // OVERDUE -> PAID (admin approves late payment)
        // No other transitions are valid
        switch (from) {
            case PENDING:
                return to == InvoiceStatus.SUBMITTED || to == InvoiceStatus.OVERDUE;
            case SUBMITTED:
                return to == InvoiceStatus.PAID;
            case OVERDUE:
                return to == InvoiceStatus.SUBMITTED || to == InvoiceStatus.PAID;
            case PAID:
                return false; // Already paid, no further transitions
            default:
                return false;
        }
    }
}
=======
}
>>>>>>> abc52cb (Resolve merge conflicts)
