package com.rms.controller;

import com.rms.dto.PrepaymentRequest;
import com.rms.dto.PaymentScheduleResponse;
import com.rms.entity.Lease;
import com.rms.entity.PaymentSchedule;
import com.rms.repository.LeaseRepository;
import com.rms.service.InvoiceService;
import com.rms.service.PaymentScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prepayments")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:5173")
public class PrepaymentController {

    private final InvoiceService invoiceService;
    private final PaymentScheduleService paymentScheduleService;
    private final LeaseRepository leaseRepository;

    public PrepaymentController(InvoiceService invoiceService,
                                PaymentScheduleService paymentScheduleService,
                                LeaseRepository leaseRepository) {
        this.invoiceService = invoiceService;
        this.paymentScheduleService = paymentScheduleService;
        this.leaseRepository = leaseRepository;
    }

    @PostMapping
    public ResponseEntity<PaymentScheduleResponse> recordPrepayment(
            @Valid @RequestBody PrepaymentRequest request) {
        
        PaymentSchedule schedule = invoiceService.processPrepayment(
                request.getLeaseId(),
                request.getMonthsToPay(),
                request.getAmountPaid(),
                request.getPaymentDate()
        );
        
        return ResponseEntity.ok(PaymentScheduleResponse.from(schedule));
    }

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<List<PaymentScheduleResponse>> getSchedulesForLease(
            @PathVariable Long leaseId) {
        
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new IllegalArgumentException("Lease not found"));
        
        List<PaymentSchedule> schedules = paymentScheduleService.getSchedulesForLease(lease);
        return ResponseEntity.ok(schedules.stream()
                .map(PaymentScheduleResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/lease/{leaseId}/coverage")
    public ResponseEntity<CoverageResponse> checkCoverage(@PathVariable Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new IllegalArgumentException("Lease not found"));
        
        LocalDate today = LocalDate.now();
        boolean covered = paymentScheduleService.isCoveredByPrepayment(lease, today);
        LocalDate nextDue = paymentScheduleService.getNextPaymentDueDate(lease);
        int totalMonthsPaid = paymentScheduleService.getTotalMonthsPaid(lease);

        CoverageResponse response = new CoverageResponse(
                covered,
                nextDue,
                totalMonthsPaid,
                paymentScheduleService.getCurrentSchedule(lease)
        );
        
        return ResponseEntity.ok(response);
    }

    public static class CoverageResponse {
        private final boolean covered;
        private final LocalDate nextPaymentDue;
        private final int totalMonthsPaid;
        private final PaymentSchedule currentSchedule;

        public CoverageResponse(boolean covered, LocalDate nextPaymentDue,
                               int totalMonthsPaid, PaymentSchedule currentSchedule) {
            this.covered = covered;
            this.nextPaymentDue = nextPaymentDue;
            this.totalMonthsPaid = totalMonthsPaid;
            this.currentSchedule = currentSchedule;
        }

        public boolean isCovered() { return covered; }
        public LocalDate getNextPaymentDue() { return nextPaymentDue; }
        public int getTotalMonthsPaid() { return totalMonthsPaid; }
        public PaymentSchedule getCurrentSchedule() { return currentSchedule; }
    }
}