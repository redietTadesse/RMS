package com.rms.service;

import com.rms.dto.PaymentRequest;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.User;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.UserRepository;
<<<<<<< HEAD
=======
import org.springframework.beans.factory.annotation.Value;
>>>>>>> abc52cb (Resolve merge conflicts)
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class PaymentService {

    private final InvoiceRepository invoiceRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
<<<<<<< HEAD
    private final EmailService emailService;
    private final SettingsService settingsService;
    private final NotificationService notificationService;

    public PaymentService(InvoiceRepository invoiceRepository,
                          FileStorageService fileStorageService,
                          UserRepository userRepository,
                          EmailService emailService,
                          SettingsService settingsService,
                          NotificationService notificationService) {
=======
    private final EmailService emailService;  // ← ADD final + inject

    @Value("${app.penalty-rate:20}")
    private int penaltyRate;

    @Value("${app.payment-window-end:10}")
    private int paymentWindowEnd;

    public PaymentService(InvoiceRepository invoiceRepository,
                          FileStorageService fileStorageService,
                          UserRepository userRepository,
                          EmailService emailService) {  // ← ADD emailService to constructor
>>>>>>> abc52cb (Resolve merge conflicts)
        this.invoiceRepository = invoiceRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.emailService = emailService;
<<<<<<< HEAD
        this.settingsService = settingsService;
        this.notificationService = notificationService;
=======
>>>>>>> abc52cb (Resolve merge conflicts)
    }

    @Transactional
    public Invoice recordPayment(Long invoiceId, PaymentRequest request, MultipartFile receipt) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("Invoice already paid");
        }

<<<<<<< HEAD
        // Validate status transition
        if (!isValidInvoiceStatusTransition(invoice.getStatus(), InvoiceStatus.SUBMITTED)) {
            throw new IllegalArgumentException("Invalid status transition from " + 
                invoice.getStatus() + " to SUBMITTED");
        }

=======
        // Calculate penalty
>>>>>>> abc52cb (Resolve merge conflicts)
        BigDecimal penalty = calculatePenalty(invoice, request.getPaymentDate());
        BigDecimal expectedTotal = invoice.getRentAmount().add(penalty);

        // Validate payment amount if provided (for manual payments)
        // Chapa payments don't include amount in request, so we skip validation for those
        if (request.getAmount() != null && !"CHAPA".equalsIgnoreCase(request.getPaymentMethod())) {
            BigDecimal paymentAmount = request.getAmount();
            BigDecimal tolerance = expectedTotal.multiply(BigDecimal.valueOf(0.01)); // 1% tolerance
            
            if (paymentAmount.compareTo(expectedTotal.subtract(tolerance)) < 0) {
                throw new IllegalArgumentException("Payment amount " + paymentAmount + 
                    " is less than expected total " + expectedTotal + 
                    " (minimum allowed: " + expectedTotal.subtract(tolerance) + ")");
            }
            
            if (paymentAmount.compareTo(expectedTotal.add(tolerance)) > 0) {
                throw new IllegalArgumentException("Payment amount " + paymentAmount + 
                    " exceeds expected total " + expectedTotal + 
                    " (maximum allowed: " + expectedTotal.add(tolerance) + ")");
            }
        }

<<<<<<< HEAD
=======
        // Store receipt
>>>>>>> abc52cb (Resolve merge conflicts)
        String receiptPath = null;
        if (receipt != null && !receipt.isEmpty()) {
            receiptPath = fileStorageService.storeReceipt(receipt, invoiceId);
        }

        invoice.setPenaltyAmount(penalty);
        invoice.setTotalAmount(expectedTotal);
        invoice.setStatus(InvoiceStatus.SUBMITTED);
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setPaymentDate(request.getPaymentDate());
        invoice.setReceiptPath(receiptPath);

<<<<<<< HEAD
        Invoice saved = invoiceRepository.save(invoice);

        notifyAdminOfSubmittedPayment(saved);

=======
        // ✅ SAVE FIRST
        Invoice saved = invoiceRepository.save(invoice);

        // ✅ THEN send email (using 'saved')
>>>>>>> abc52cb (Resolve merge conflicts)
        User user = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
        if (user != null && user.getEmail() != null) {
            emailService.sendPaymentConfirmationEmail(user, saved);
        }

        return saved;
    }
<<<<<<< HEAD

    @Transactional
    public Invoice approvePayment(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.SUBMITTED) {
            throw new IllegalArgumentException("Only submitted payments can be approved");
        }

        // Validate status transition
        if (!isValidInvoiceStatusTransition(invoice.getStatus(), InvoiceStatus.PAID)) {
            throw new IllegalArgumentException("Invalid status transition from " + 
                invoice.getStatus() + " to PAID");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        Invoice saved = invoiceRepository.save(invoice);

        User tenant = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
        if (tenant != null) {
            if (tenant.getEmail() != null) {
                emailService.sendPaymentApprovedEmail(tenant, saved);
            }
            notificationService.createForUser(
                    tenant.getId(),
                    "PAYMENT_APPROVED",
                    "Payment Approved",
                    "Your payment for invoice " + saved.getInvoiceNumber() + " has been approved.",
                    "/payments"
            );
        }

        return saved;
    }

    private void notifyAdminOfSubmittedPayment(Invoice invoice) {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin != null) {
            notificationService.createForUser(
                    admin.getId(),
                    "PAYMENT_SUBMITTED",
                    "Payment Submitted",
                    "Tenant " + invoice.getLease().getTenant().getName() +
                            " has submitted payment for invoice " + invoice.getInvoiceNumber() +
                            ". Please review and approve.",
                    "/payments"
            );
        }
    }

=======
    @Transactional
    public Invoice submitReceipt(Long invoiceId, LocalDate paymentDate,
                                 String paymentMethod, MultipartFile receipt) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("Invoice already paid");
        }
        if (receipt == null || receipt.isEmpty()) {
            throw new IllegalArgumentException("A receipt file is required");
        }

        BigDecimal penalty = calculatePenalty(invoice, paymentDate);
        invoice.setPenaltyAmount(penalty);
        invoice.setTotalAmount(invoice.getRentAmount().add(penalty));
        invoice.setStatus(InvoiceStatus.SUBMITTED);
        invoice.setPaymentMethod(paymentMethod);
        invoice.setPaymentDate(paymentDate);
        invoice.setReceiptPath(fileStorageService.storeReceipt(receipt, invoiceId));

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice approvePayment(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.getStatus() != InvoiceStatus.SUBMITTED) {
            throw new IllegalArgumentException("Only submitted payments can be approved");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        Invoice saved = invoiceRepository.save(invoice);

        User user = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
        if (user != null && user.getEmail() != null) {
            emailService.sendPaymentConfirmationEmail(user, saved);
        }
        return saved;
    }
>>>>>>> abc52cb (Resolve merge conflicts)
    private BigDecimal calculatePenalty(Invoice invoice, LocalDate paymentDate) {
        int paymentWindowEnd = settingsService.getIntSetting("payment_window_end", 10);
        if (paymentDate.getDayOfMonth() <= paymentWindowEnd) {
            return BigDecimal.ZERO;
        }

<<<<<<< HEAD
        int penaltyRate = settingsService.getIntSetting("penalty_rate", 20);
        return invoice.getRentAmount()
=======
        BigDecimal penalty = invoice.getRentAmount()
>>>>>>> abc52cb (Resolve merge conflicts)
                .multiply(BigDecimal.valueOf(penaltyRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePenaltyPreview(Long invoiceId, LocalDate paymentDate) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return calculatePenalty(invoice, paymentDate);
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