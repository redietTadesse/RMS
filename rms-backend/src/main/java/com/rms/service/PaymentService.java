package com.rms.service;

import com.rms.dto.PaymentRequest;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.LeaseStatus;
import com.rms.entity.User;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.UserRepository;
import org.springframework.dao.OptimisticLockingFailureException;
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
    private final EmailService emailService;
    private final SmsService smsService;
    private final SettingsService settingsService;
    private final NotificationService notificationService;

    public PaymentService(InvoiceRepository invoiceRepository,
                          FileStorageService fileStorageService,
                          UserRepository userRepository,
                          EmailService emailService,
                          SmsService smsService,
                          SettingsService settingsService,
                          NotificationService notificationService) {
        this.invoiceRepository = invoiceRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.settingsService = settingsService;
        this.notificationService = notificationService;
    }

    // ─── RECORD PAYMENT (Tenant submits receipt) ───

    @Transactional
    public Invoice recordPayment(Long invoiceId, PaymentRequest request, MultipartFile receipt) {
        // Validate invoice exists
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

        // Validate invoice is not already paid
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("Invoice already paid");
        }

        // Validate lease is active
        if (invoice.getLease().getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is no longer active. Cannot process payment.");
        }

        // Validate status transition
        if (!isValidInvoiceStatusTransition(invoice.getStatus(), InvoiceStatus.SUBMITTED)) {
            throw new IllegalArgumentException("Invalid status transition from " +
                    invoice.getStatus() + " to SUBMITTED");
        }

        // Calculate penalty based on payment date
        BigDecimal penalty = calculatePenalty(invoice, request.getPaymentDate());
        BigDecimal expectedTotal = invoice.getRentAmount().add(penalty);

        // Validate payment amount (if provided)
        if (request.getAmount() != null && !"CHAPA".equalsIgnoreCase(request.getPaymentMethod())) {
            BigDecimal tolerance = expectedTotal.multiply(BigDecimal.valueOf(0.01)); // 1% tolerance

            if (request.getAmount().compareTo(expectedTotal.subtract(tolerance)) < 0) {
                throw new IllegalArgumentException("Payment amount " + request.getAmount() +
                        " is less than expected total " + expectedTotal);
            }

            if (request.getAmount().compareTo(expectedTotal.add(tolerance)) > 0) {
                throw new IllegalArgumentException("Payment amount " + request.getAmount() +
                        " exceeds expected total " + expectedTotal);
            }
        }

        // Store receipt file
        String receiptPath = null;
        if (receipt != null && !receipt.isEmpty()) {
            receiptPath = fileStorageService.storeReceipt(receipt, invoiceId);
        }

        try {
            // Update invoice
            invoice.setPenaltyAmount(penalty);
            invoice.setTotalAmount(expectedTotal);
            invoice.setStatus(InvoiceStatus.SUBMITTED);
            invoice.setPaymentMethod(request.getPaymentMethod());
            invoice.setPaymentDate(request.getPaymentDate());
            invoice.setReceiptPath(receiptPath);

            Invoice saved = invoiceRepository.save(invoice);

            // Notify admin
            notifyAdminOfSubmittedPayment(saved);

            // Send confirmation email to tenant
            User user = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
            if (user != null && user.getEmail() != null) {
                emailService.sendPaymentConfirmationEmail(user, saved);
            }

            return saved;
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalArgumentException("Invoice was updated by another process. Please try again.");
        }
    }

    // ─── SUBMIT RECEIPT (Simplified version) ───

    @Transactional
    public Invoice submitReceipt(Long invoiceId, LocalDate paymentDate,
                                 String paymentMethod, MultipartFile receipt) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("Invoice already paid");
        }

        if (invoice.getLease().getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is no longer active. Cannot process payment.");
        }

        if (receipt == null || receipt.isEmpty()) {
            throw new IllegalArgumentException("A receipt file is required");
        }

        BigDecimal penalty = calculatePenalty(invoice, paymentDate);

        try {
            invoice.setPenaltyAmount(penalty);
            invoice.setTotalAmount(invoice.getRentAmount().add(penalty));
            invoice.setStatus(InvoiceStatus.SUBMITTED);
            invoice.setPaymentMethod(paymentMethod);
            invoice.setPaymentDate(paymentDate);
            invoice.setReceiptPath(fileStorageService.storeReceipt(receipt, invoiceId));

            Invoice saved = invoiceRepository.save(invoice);

            notifyAdminOfSubmittedPayment(saved);

            User user = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
            if (user != null && user.getEmail() != null) {
                emailService.sendPaymentConfirmationEmail(user, saved);
            }

            return saved;
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalArgumentException("Invoice was updated by another process. Please try again.");
        }
    }

    // ─── APPROVE PAYMENT (Admin) ───

    @Transactional
    public Invoice approvePayment(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.SUBMITTED) {
            throw new IllegalArgumentException("Only submitted payments can be approved. Current status: " + invoice.getStatus());
        }

        if (invoice.getLease().getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is no longer active. Cannot approve payment.");
        }

        if (!isValidInvoiceStatusTransition(invoice.getStatus(), InvoiceStatus.PAID)) {
            throw new IllegalArgumentException("Invalid status transition from " +
                    invoice.getStatus() + " to PAID");
        }

        try {
            invoice.setStatus(InvoiceStatus.PAID);
            Invoice saved = invoiceRepository.save(invoice);

            // Notify tenant
            User tenant = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
            if (tenant != null) {
                if (tenant.getEmail() != null) {
                    emailService.sendPaymentConfirmedEmail(tenant, saved);
                }
                notificationService.createForUserOnce(
                        tenant.getId(),
                        "PAYMENT_APPROVED",
                        saved.getId(),
                        "Payment Approved",
                        "Your payment for invoice " + saved.getInvoiceNumber() + " has been approved.",
                        "/payments"
                );
            }

            // Send SMS confirmation
            if (invoice.getLease().getTenant().getPhone() != null) {
                smsService.sendPaymentConfirmedSms(invoice.getLease().getTenant(), saved);
            }

            return saved;
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalArgumentException("Invoice was updated by another process. Please try again.");
        }
    }

    // ─── CALCULATE PENALTY ───

    private BigDecimal calculatePenalty(Invoice invoice, LocalDate paymentDate) {
        int paymentWindowEnd = settingsService.getIntSetting("payment_window_end", 10);
        
        // No penalty if paid within the grace period (first X days of month)
        if (paymentDate.getDayOfMonth() <= paymentWindowEnd) {
            return BigDecimal.ZERO;
        }

        int penaltyRate = settingsService.getIntSetting("penalty_rate", 20);
        return invoice.getRentAmount()
                .multiply(BigDecimal.valueOf(penaltyRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePenaltyPreview(Long invoiceId, LocalDate paymentDate) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with id: " + invoiceId));
        return calculatePenalty(invoice, paymentDate);
    }

    // ─── NOTIFICATIONS ───

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

    // ─── VALIDATION HELPERS ───

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

    // ─── VALIDATION METHODS (For use by controllers) ───

    public void validatePaymentRequest(PaymentRequest request) {
        if (request.getPaymentDate() == null) {
            throw new IllegalArgumentException("Payment date is required");
        }

        if (request.getPaymentDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future");
        }

        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException("Payment method is required");
        }

        String method = request.getPaymentMethod().toUpperCase();
        if (!"CASH".equals(method) && !"BANK_TRANSFER".equals(method) && 
            !"CHAPA".equals(method) && !"RECEIPT".equals(method)) {
            throw new IllegalArgumentException("Payment method must be CASH, BANK_TRANSFER, CHAPA, or RECEIPT");
        }

        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative");
        }

        if (request.getAmount() != null && request.getAmount().compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed 99,999,999.99");
        }
    }

    public void validateReceipt(MultipartFile receipt) {
        if (receipt == null || receipt.isEmpty()) {
            throw new IllegalArgumentException("Receipt file is required");
        }

        String contentType = receipt.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Could not determine file type");
        }

        boolean allowed = false;
        String[] allowedTypes = {
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };
        for (String allowedType : allowedTypes) {
            if (allowedType.equals(contentType)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            throw new IllegalArgumentException("File type not allowed. Please upload JPG, PNG, GIF, WEBP, PDF, or DOC files.");
        }

        long maxSize = 10 * 1024 * 1024; // 10MB
        if (receipt.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
    }
}