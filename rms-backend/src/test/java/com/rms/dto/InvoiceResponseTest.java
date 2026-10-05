package com.rms.dto;

import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.Lease;
import com.rms.entity.Space;
import com.rms.entity.Tenant;
import com.rms.service.TaxCalculationService;
import com.rms.service.TimeTravelService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the admin-facing "How this was calculated" strings (Fix 3) — the
 * plain-English derivation of rent, due date and penalty on InvoiceResponse.
 */
class InvoiceResponseTest {

    @AfterEach
    void tearDown() {
        // The DTO holds its TimeTravelService statically; don't leak the fixed
        // clock into other tests.
        InvoiceResponse.setTimeTravelService(null);
    }

    private static Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Rediet PLC");
        tenant.setPhone("0911223344");
        return tenant;
    }

    private static Space space() {
        Space space = new Space();
        space.setId(1L);
        space.setUnitNumber("G-01");
        space.setCategory("Shop");
        return space;
    }

    private static Lease lease(LocalDate startDate, Integer dueDay) {
        Lease lease = new Lease();
        lease.setId(1L);
        lease.setTenant(tenant());
        lease.setSpace(space());
        lease.setStartDate(startDate);
        lease.setEndDate(startDate.plusMonths(12));
        lease.setMonthlyRent(new BigDecimal("60000"));
        lease.setDueDay(dueDay);
        return lease;
    }

    private static Invoice invoice(Lease lease, String rent, LocalDate issueDate, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("INV-2026-0001");
        invoice.setLease(lease);
        invoice.setRentAmount(new BigDecimal(rent));
        invoice.setPenaltyAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal(rent));
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(dueDate);
        invoice.setStatus(InvoiceStatus.PENDING);
        return invoice;
    }

    @Test
    void from_explainsProratedRent_withDayCountAndDailyRate() {
        // Sep 24 start in a 30-day month → 7 billable days (24th..30th).
        Invoice invoice = invoice(
                lease(LocalDate.of(2026, 9, 24), 10),
                "14000.00",
                LocalDate.of(2026, 9, 24),
                LocalDate.of(2026, 10, 10));

        InvoiceResponse response = InvoiceResponse.from(invoice);

        assertTrue(response.getRentExplanation().startsWith("Prorated first month"));
        assertTrue(response.getRentExplanation().contains("7 of 30 days"));
        // Daily rate = 60,000 ÷ 30 = 2,000.00 (formatting matches the default locale).
        assertTrue(response.getRentExplanation()
                .contains(String.format("%,.2f", new BigDecimal("2000.00"))));
    }

    @Test
    void from_explainsFullMonthRent_whenNothingWasProrated() {
        Invoice invoice = invoice(
                lease(LocalDate.of(2026, 9, 28), 10),
                "60000",
                LocalDate.of(2026, 12, 28),
                LocalDate.of(2027, 1, 10));

        InvoiceResponse response = InvoiceResponse.from(invoice);

        assertEquals("Full month rent — no proration", response.getRentExplanation());
        assertEquals("Due on day 10 of the month", response.getDueDateExplanation());
        assertEquals("No penalty — invoice is not overdue", response.getPenaltyExplanation());
    }

    @Test
    void from_explainsPenalty_withDaysOverdueAndDailyRate() {
        TimeTravelService timeTravel = new TimeTravelService();
        timeTravel.setFixedDate(LocalDate.of(2026, 1, 6));   // 5 days after the due date
        InvoiceResponse.setTimeTravelService(timeTravel);

        TaxCalculationService taxService = mock(TaxCalculationService.class);
        when(taxService.calculatePenalty(any(), any(), any())).thenReturn(new BigDecimal("3478.26"));
        when(taxService.getPenaltyRate()).thenReturn(new BigDecimal("0.20"));
        when(taxService.getTaxBreakdown(any(), any(), any())).thenReturn(
                new TaxCalculationService.TaxBreakdown(
                        new BigDecimal("60000"), new BigDecimal("52173.91"),
                        new BigDecimal("7826.09"), BigDecimal.ZERO, false,
                        new BigDecimal("3478.26"), new BigDecimal("67826.26")));

        Invoice invoice = invoice(
                lease(LocalDate.of(2025, 9, 28), 10),
                "60000",
                LocalDate.of(2025, 12, 28),
                LocalDate.of(2026, 1, 1));
        invoice.setStatus(InvoiceStatus.OVERDUE);
        invoice.setPenaltyAmount(new BigDecimal("115.94"));
        invoice.setTotalAmount(new BigDecimal("60115.94"));

        InvoiceResponse response = InvoiceResponse.from(invoice, taxService);

        assertEquals(
                String.format("5 days overdue × ETB %,.2f/day (monthly ETB %,.2f ÷ 30)",
                        new BigDecimal("115.94"), new BigDecimal("3478.26")),
                response.getPenaltyExplanation());
        assertEquals("Full month rent — no proration", response.getRentExplanation());
        assertEquals(5L, response.getDaysOverdue());
    }
}
