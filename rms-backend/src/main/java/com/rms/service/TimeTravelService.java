package com.rms.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TimeTravelService provides a centralized "current date" that scheduled job
 * methods can read instead of calling LocalDate.now() directly. This allows
 * the test time-travel feature to shift the effective date for invoice
 * generation, penalty calculation, lease expiry, etc. without touching the
 * system clock.
 *
 * @ThreadSafe
 */
@Component
public class TimeTravelService {

    private final AtomicReference<LocalDate> fixedDate = new AtomicReference<>(null);

    /** @return the virtual date when set, otherwise the real system date. */
    public LocalDate getCurrentDate() {
        LocalDate override = fixedDate.get();
        return override != null ? override : LocalDate.now();
    }

    /** Set a fixed virtual date. */
    public void setFixedDate(LocalDate date) {
        if (date == null) {
            resetTime();
        } else {
            fixedDate.set(date);
        }
    }

    /** Advance from the current effective date by the given number of days. */
    public LocalDate advanceDays(int days) {
        LocalDate next = getCurrentDate().plusDays(days);
        fixedDate.set(next);
        return next;
    }

    /** Advance from the current effective date by the given number of months. */
    public LocalDate advanceMonths(int months) {
        LocalDate next = getCurrentDate().plusMonths(months);
        fixedDate.set(next);
        return next;
    }

    /** Clear the override and return to real system time. */
    public void resetTime() {
        fixedDate.set(null);
    }

    /** @return true when a virtual date is currently active. */
    public boolean isActive() {
        return fixedDate.get() != null;
    }
}
