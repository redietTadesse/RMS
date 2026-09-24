package com.rms.service;

import com.rms.dto.PaymentRequest;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.Lease;
import com.rms.entity.LeaseStatus;
import com.rms.entity.Tenant;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private SettingsService settingsService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaxCalculationService taxCalculationService;

    @Mock
    private TimeTravelService timeTravelService;

    @InjectMocks
    private PaymentService paymentService;

    private Invoice testInvoice;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        Tenant testTenant = new Tenant();
        testTenant.setUserId(1L);

        Lease testLease = new Lease();
        testLease.setStatus(LeaseStatus.ACTIVE);
        testLease.setTenant(testTenant);

        testInvoice = new Invoice();
        testInvoice.setId(1L);
        testInvoice.setLease(testLease);
        testInvoice.setRentAmount(new BigDecimal("60000"));
        testInvoice.setPenaltyAmount(BigDecimal.ZERO);
        testInvoice.setTotalAmount(new BigDecimal("60000"));
        testInvoice.setStatus(InvoiceStatus.PENDING);
        testInvoice.setIssueDate(LocalDate.of(2026, 8, 1));

        paymentRequest = new PaymentRequest();
        paymentRequest.setPaymentDate(LocalDate.of(2026, 8, 5));
        paymentRequest.setPaymentMethod("receipt");

        lenient().when(timeTravelService.getCurrentDate()).thenReturn(LocalDate.now());
    }

    // ✅ TEST 1: No penalty when paid within window
    @Test
    void calculatePenalty_ShouldReturnZero_WhenPaidWithinWindow() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(settingsService.getIntSetting("payment_window_end", 10)).thenReturn(10);
        LocalDate paymentDate = LocalDate.of(2026, 8, 8);

        BigDecimal penalty = paymentService.calculatePenaltyPreview(1L, paymentDate);

        assertEquals(BigDecimal.ZERO, penalty);
    }

    // ✅ TEST 2: Penalty applied when paid after window
    @Test
    void calculatePenalty_ShouldApplyPenalty_WhenPaidAfterWindow() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(settingsService.getIntSetting("payment_window_end", 10)).thenReturn(10);
        when(taxCalculationService.calculatePenalty(any(), any(), any()))
                .thenReturn(new BigDecimal("12000"));
        LocalDate paymentDate = LocalDate.of(2026, 8, 15);

        BigDecimal penalty = paymentService.calculatePenaltyPreview(1L, paymentDate);

        assertEquals(new BigDecimal("12000"), penalty);
    }

    // ✅ TEST 3: Record payment with receipt
    @Test
    void recordPayment_ShouldMarkInvoiceAsSubmitted_WhenReceiptUploaded() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(settingsService.getIntSetting("payment_window_end", 10)).thenReturn(10);
        when(fileStorageService.storeReceipt(any(), any())).thenReturn("receipts/test.pdf");
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

        MultipartFile receipt = mock(MultipartFile.class);
        when(receipt.getContentType()).thenReturn("application/pdf");

        Invoice result = paymentService.recordPayment(1L, paymentRequest, receipt);

        assertEquals(InvoiceStatus.SUBMITTED, result.getStatus());
        assertEquals("receipts/test.pdf", result.getReceiptPath());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // ✅ TEST 4: Cannot pay already paid invoice
    @Test
    void recordPayment_ShouldThrowException_WhenInvoiceAlreadyPaid() {
        testInvoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        MultipartFile receipt = mock(MultipartFile.class);
        when(receipt.isEmpty()).thenReturn(false);
        when(receipt.getContentType()).thenReturn("application/pdf");
        when(receipt.getSize()).thenReturn(1000L);

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.recordPayment(1L, paymentRequest, receipt));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    // ✅ TEST 5: Approve submitted payment
    @Test
    void approvePayment_ShouldMarkAsPaid_WhenInvoiceIsSubmitted() {
        testInvoice.setStatus(InvoiceStatus.SUBMITTED);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);

        Invoice result = paymentService.approvePayment(1L);

        assertEquals(InvoiceStatus.PAID, result.getStatus());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // ✅ TEST 6: Cannot approve non-submitted invoice
    @Test
    void approvePayment_ShouldThrowException_WhenInvoiceNotSubmitted() {
        testInvoice.setStatus(InvoiceStatus.PENDING);
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.approvePayment(1L));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }
}