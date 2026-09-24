package com.rms;

import com.rms.dto.LeaseRequest;
import com.rms.dto.PaymentRequest;
import com.rms.dto.PrepaymentRequest;
import com.rms.service.TimeTravelService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationClockTest {

    @Test
    void dateConstraintsUseVirtualDate() {
        TimeTravelService timeTravelService = new TimeTravelService();
        timeTravelService.setFixedDate(LocalDate.of(2026, 1, 1));
        Validator validator = Validation.byDefaultProvider()
                .configure()
                .clockProvider(() -> java.time.Clock.fixed(
                        timeTravelService.getCurrentDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),
                        java.time.ZoneId.systemDefault()))
                .buildValidatorFactory()
                .getValidator();

        LeaseRequest lease = new LeaseRequest();
        lease.setStartDate(LocalDate.of(2026, 1, 15));
        lease.setEndDate(LocalDate.of(2026, 2, 15));
        assertTrue(validator.validateProperty(lease, "startDate").isEmpty());

        PaymentRequest payment = new PaymentRequest();
        payment.setPaymentDate(LocalDate.of(2026, 1, 1));
        assertTrue(validator.validateProperty(payment, "paymentDate").isEmpty());

        PrepaymentRequest prepayment = new PrepaymentRequest();
        prepayment.setPaymentDate(LocalDate.of(2026, 1, 1));
        assertTrue(validator.validateProperty(prepayment, "paymentDate").isEmpty());
    }
}
