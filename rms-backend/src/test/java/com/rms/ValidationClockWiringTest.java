package com.rms;

import com.rms.dto.LeaseRequest;
import com.rms.service.TimeTravelService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ValidationClockWiringTest {

    @Autowired
    private TimeTravelService timeTravelService;

    @Autowired
    private Validator validator;

    @Test
    void productionValidatorHonorsVirtualTime() {
        timeTravelService.setFixedDate(LocalDate.of(2026, 1, 1));
        try {
            LeaseRequest lease = new LeaseRequest();
            lease.setStartDate(LocalDate.of(2026, 1, 15));

            assertTrue(validator.validateProperty(lease, "startDate").isEmpty());
        } finally {
            timeTravelService.resetTime();
        }
    }
}
