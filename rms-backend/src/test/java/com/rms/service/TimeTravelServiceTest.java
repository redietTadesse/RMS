package com.rms.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeTravelServiceTest {

    @Test
    void fixedDateCanAdvanceAndReset() {
        TimeTravelService service = new TimeTravelService();
        LocalDate fixedDate = LocalDate.of(2026, 9, 24);

        service.setFixedDate(fixedDate);
        assertEquals(fixedDate, service.getCurrentDate());
        assertTrue(service.isActive());

        assertEquals(fixedDate.plusDays(3), service.advanceDays(3));
        assertEquals(fixedDate.plusDays(3).plusMonths(2), service.advanceMonths(2));

        service.resetTime();
        assertFalse(service.isActive());
        assertEquals(LocalDate.now(), service.getCurrentDate());
    }

    @Test
    void advanceMonthsClampsAtMonthEnd() {
        TimeTravelService service = new TimeTravelService();

        service.setFixedDate(LocalDate.of(2026, 1, 31));

        assertEquals(LocalDate.of(2026, 2, 28), service.advanceMonths(1));
        assertEquals(LocalDate.of(2026, 3, 28), service.advanceMonths(1));
    }
}
