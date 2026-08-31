package com.rms.service;

import com.rms.entity.Invoice;
import com.rms.entity.Lease;
import com.rms.entity.Space;
import com.rms.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @InjectMocks
    private SmsService smsService;

    private Tenant testTenant;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setName("Test Tenant");
        testTenant.setPhone("+251911000000");

        Space space = new Space();
        space.setId(1L);
        space.setUnitNumber("G-01");

        Lease lease = new Lease();
        lease.setId(1L);
        lease.setSpace(space);

        testInvoice = new Invoice();
        testInvoice.setId(1L);
        testInvoice.setInvoiceNumber("INV-2026-0001");
        testInvoice.setLease(lease);
        testInvoice.setRentAmount(new BigDecimal("60000"));
        testInvoice.setTotalAmount(new BigDecimal("60000"));
        testInvoice.setDueDate(LocalDate.of(2026, 8, 10));

        // Set mock mode via reflection
        ReflectionTestUtils.setField(smsService, "smsMode", "mock");
        ReflectionTestUtils.setField(smsService, "senderId", "RMS");
        ReflectionTestUtils.setField(smsService, "baseUrl", "http://localhost:5173");
    }

    @Test
    void sendSms_ShouldLogMockSms_WhenInMockMode() {
        // This should not throw any exception
        assertDoesNotThrow(() -> smsService.sendSms(testTenant, "Test message"));
    }

    @Test
    void isMockMode_ShouldReturnTrue_WhenSetToMock() {
        assertTrue(smsService.isMockMode());
    }

    @Test
    void sendRentReminderSms_ShouldWork_WhenInMockMode() {
        assertDoesNotThrow(() -> smsService.sendRentReminderSms(testTenant, testInvoice, 5));
    }

    @Test
    void sendOverdueSms_ShouldWork_WhenInMockMode() {
        assertDoesNotThrow(() -> smsService.sendOverdueSms(testTenant, testInvoice, 2, 400.0));
    }
}