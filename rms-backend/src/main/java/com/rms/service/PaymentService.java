package com.rms.service;

import com.rms.dto.PaymentRequest;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.User;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.UserRepository;
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
    private final SettingsService settingsService;

    public PaymentService(InvoiceRepository invoiceRepository,
                          FileStorageService fileStorageService,
                          UserRepository userRepository,
                          EmailService emailService,
                          SettingsService settingsService) {
        this.invoiceRepository = invoiceRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.settingsService = settingsService;
    }

    @Transactional
    public Invoice recordPayment(Long invoiceId, PaymentRequest request, MultipartFile receipt) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("Invoice already paid");
        }

        // Calculate penalty
        BigDecimal penalty = calculatePenalty(invoice, request.getPaymentDate());
        BigDecimal totalAmount = invoice.getRentAmount().add(penalty);

        // Store receipt
        String receiptPath = null;
        if (receipt != null && !receipt.isEmpty()) {
            receiptPath = fileStorageService.storeReceipt(receipt, invoiceId);
        }

        // Update invoice
        invoice.setPenaltyAmount(penalty);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setPaymentDate(request.getPaymentDate());
        invoice.setReceiptPath(receiptPath);

        // ✅ SAVE FIRST
        Invoice saved = invoiceRepository.save(invoice);

        // ✅ THEN send email (using 'saved')
        User user = userRepository.findById(invoice.getLease().getTenant().getUserId()).orElse(null);
        if (user != null && user.getEmail() != null) {
            emailService.sendPaymentConfirmationEmail(user, saved);
        }

        return saved;
    }
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
    private BigDecimal calculatePenalty(Invoice invoice, LocalDate paymentDate) {
        int paymentWindowEnd = settingsService.getIntSetting("payment_window_end", 10);
        if (paymentDate.getDayOfMonth() <= paymentWindowEnd) {
            return BigDecimal.ZERO;
        }

        int penaltyRate = settingsService.getIntSetting("penalty_rate", 20);
        BigDecimal penalty = invoice.getRentAmount()
                .multiply(BigDecimal.valueOf(penaltyRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return penalty;
    }

    public BigDecimal calculatePenaltyPreview(Long invoiceId, LocalDate paymentDate) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return calculatePenalty(invoice, paymentDate);
    }
}