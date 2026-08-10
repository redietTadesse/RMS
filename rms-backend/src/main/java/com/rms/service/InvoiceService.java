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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public InvoiceService(InvoiceRepository invoiceRepository, LeaseRepository leaseRepository,
                          TenantRepository tenantRepository, UserRepository userRepository,
                          NotificationService notificationService, EmailService emailService) {
        this.invoiceRepository = invoiceRepository;
        this.leaseRepository = leaseRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    
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
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        // CHECK FIRST: Does invoice already exist for this month?
        if (invoiceRepository.existsByLeaseAndIssueDateBetween(lease, monthStart, monthEnd)) {
            return null; // Already generated
        }

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber();
        LocalDate dueDate = today.withDayOfMonth(10);

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setLease(lease);
        invoice.setRentAmount(lease.getMonthlyRent());
        invoice.setPenaltyAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(lease.getMonthlyRent());
        invoice.setIssueDate(today);
        invoice.setDueDate(dueDate);
        invoice.setStatus(InvoiceStatus.PENDING);

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

        for (Invoice invoice : pendingInvoices) {
            if (invoice.getDueDate().isBefore(today)) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                invoiceRepository.save(invoice);
                notifyInvoiceOverdue(invoice);
            }
        }
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

    
    private String generateInvoiceNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        long count = invoiceRepository.count() + 1;
        String seq = String.format("%04d", count);
        return "INV-" + year + "-" + seq;
    }
}
