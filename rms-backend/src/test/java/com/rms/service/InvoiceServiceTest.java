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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SettingsService settingsService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private TimeTravelService timeTravelService;

    @Mock
    private TaxCalculationService taxCalculationService;

    @Mock
    private PaymentScheduleService paymentScheduleService;

    @Mock
    private InvoiceNumberService invoiceNumberService;

    @InjectMocks
    private InvoiceService invoiceService;

    private Lease testLease;
    private Invoice testInvoice;
    private Tenant testTenant;
    private User testUser;

    @BeforeEach
    void setUp() {
        testLease = new Lease();
        testLease.setId(1L);
        testLease.setMonthlyRent(new BigDecimal("60000"));
        testLease.setDueDay(10);
        testLease.setStatus(LeaseStatus.ACTIVE);
        testLease.setStartDate(LocalDate.now().minusMonths(1));

        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("Test Tenant");
        testTenant.setPhone("+251911000000");
        testTenant.setUserId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        testInvoice = new Invoice();
        testInvoice.setId(1L);
        testInvoice.setInvoiceNumber("INV-2026-0001");
        testInvoice.setLease(testLease);
        testInvoice.setRentAmount(new BigDecimal("60000"));
        testInvoice.setTotalAmount(new BigDecimal("60000"));
        testInvoice.setStatus(InvoiceStatus.PENDING);
        testInvoice.setDueDate(LocalDate.of(2026, 8, 10));
        testLease.setTenant(testTenant);

        lenient().when(timeTravelService.getCurrentDate()).thenReturn(LocalDate.now());
        lenient().when(invoiceNumberService.nextInvoiceNumber(any(LocalDate.class)))
            .thenReturn("INV-2026-0001");
    }

    // ✅ TEST 1: Generate invoice success
    @Test
    void generateInvoice_ShouldCreateInvoice_WhenLeaseIsActive() {
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(testLease));
        when(invoiceRepository.existsByLeaseAndIssueDateBetween(any(), any(), any())).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(testInvoice);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Invoice result = invoiceService.generateInvoice(1L);

        assertNotNull(result);
        assertEquals("INV-2026-0001", result.getInvoiceNumber());
        assertEquals(new BigDecimal("60000"), result.getRentAmount());
        assertEquals(InvoiceStatus.PENDING, result.getStatus());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // ✅ TEST 2: Skip duplicate invoice
    @Test
    void generateInvoice_ShouldReturnNull_WhenInvoiceAlreadyExists() {
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(testLease));
        when(invoiceRepository.existsByLeaseAndIssueDateBetween(any(), any(), any())).thenReturn(true);

        Invoice result = invoiceService.generateInvoice(1L);

        assertNull(result);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    // ✅ TEST 3: Calculate daily penalties (mark overdue invoices)
    @Test
    void calculateDailyPenalties_ShouldMarkOverdue_WhenDueDatePassed() {
        List<Invoice> pendingInvoices = new ArrayList<>();
        Invoice pastDueInvoice = new Invoice();
        pastDueInvoice.setId(1L);
        pastDueInvoice.setLease(testLease);
        pastDueInvoice.setDueDate(LocalDate.now().minusDays(5));
        pastDueInvoice.setStatus(InvoiceStatus.PENDING);
        pastDueInvoice.setRentAmount(new BigDecimal("60000"));
        pastDueInvoice.setPenaltyAmount(BigDecimal.ZERO);
        pendingInvoices.add(pastDueInvoice);

        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(pendingInvoices);
        when(taxCalculationService.calculatePenalty(any(), any(), any()))
                .thenReturn(new BigDecimal("300"));

        invoiceService.calculateDailyPenalties();

        assertEquals(InvoiceStatus.OVERDUE, pastDueInvoice.getStatus());
        assertTrue(pastDueInvoice.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0);
        verify(invoiceRepository).save(pastDueInvoice);
    }

    // ✅ TEST 4: Don't calculate penalties for non-overdue invoices
    @Test
    void calculateDailyPenalties_ShouldNotChange_WhenDueDateNotPassed() {
        List<Invoice> pendingInvoices = new ArrayList<>();
        Invoice futureInvoice = new Invoice();
        futureInvoice.setId(1L);
        futureInvoice.setDueDate(LocalDate.now().plusDays(5));
        futureInvoice.setStatus(InvoiceStatus.PENDING);
        pendingInvoices.add(futureInvoice);

        when(invoiceRepository.findByStatus(InvoiceStatus.PENDING)).thenReturn(pendingInvoices);

        invoiceService.calculateDailyPenalties();

        assertEquals(InvoiceStatus.PENDING, futureInvoice.getStatus());
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    // ✅ TEST 5: Get invoice by ID
    @Test
    void getInvoiceById_ShouldReturnInvoice_WhenFound() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(testInvoice));

        Invoice result = invoiceService.getInvoiceById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("INV-2026-0001", result.getInvoiceNumber());
    }

    // ✅ TEST 6: Get invoice by ID - not found
    @Test
    void getInvoiceById_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.getInvoiceById(99L));
    }

    // ✅ TEST 7: Notify overdue sends notification and email
    @Test
    void notifyOverdue_ShouldSendNotificationAndEmail_WhenInvoiceOverdue() {
        testInvoice.setDueDate(LocalDate.now().minusDays(1));
        testInvoice.setStatus(InvoiceStatus.OVERDUE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        invoiceService.notifyInvoiceOverdue(testInvoice);

        verify(emailService).sendOverdueEmail(testUser, testInvoice);
    }
}