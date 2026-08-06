package com.rms.service;

import com.rms.dto.PaymentRequest;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.penalty-rate:20}")
    private int penaltyRate;

    @Value("${app.payment-window-end:10}")
    private int paymentWindowEnd;

    public PaymentService(InvoiceRepository invoiceRepository, FileStorageService fileStorageService) {
        this.invoiceRepository = invoiceRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Record a payment for an invoice
     */
    @Transactional
    public Invoice recordPayment(Long invoiceId, PaymentRequest request, MultipartFile receipt) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalArgumentException("Invoice already paid");
        }

        // Calculate penalty if payment is late
        BigDecimal penalty = calculatePenalty(invoice, request.getPaymentDate());
        BigDecimal totalAmount = invoice.getRentAmount().add(penalty);

        // Save receipt if provided
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

        return invoiceRepository.save(invoice);
    }

    /**
     * Calculate penalty based on payment date
     */
    private BigDecimal calculatePenalty(Invoice invoice, LocalDate paymentDate) {
        if (paymentDate.getDayOfMonth() <= paymentWindowEnd) {
            return BigDecimal.ZERO;
        }

        // Apply penalty rate (default 20%)
        BigDecimal penalty = invoice.getRentAmount()
                .multiply(BigDecimal.valueOf(penaltyRate))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return penalty;
    }

    /**
     * Get penalty for an invoice (without saving)
     */
    public BigDecimal calculatePenaltyPreview(Long invoiceId, LocalDate paymentDate) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return calculatePenalty(invoice, paymentDate);
    }
}