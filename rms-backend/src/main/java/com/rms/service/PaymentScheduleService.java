package com.rms.service;

import com.rms.entity.*;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.LeaseRepository;
import com.rms.repository.PaymentScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentScheduleService {

    private final PaymentScheduleRepository scheduleRepository;
    private final LeaseRepository leaseRepository;
    private final InvoiceRepository invoiceRepository;
    private final TaxCalculationService taxCalculationService;

    public PaymentScheduleService(PaymentScheduleRepository scheduleRepository,
                                  LeaseRepository leaseRepository,
                                  InvoiceRepository invoiceRepository,
                                  TaxCalculationService taxCalculationService) {
        this.scheduleRepository = scheduleRepository;
        this.leaseRepository = leaseRepository;
        this.invoiceRepository = invoiceRepository;
        this.taxCalculationService = taxCalculationService;
    }

    @Transactional
    public PaymentSchedule createPaymentSchedule(Lease lease, Integer monthsCovered,
                                                  BigDecimal amountPaid, LocalDate paymentDate) {
        Integer lastPaymentNumber = scheduleRepository.findMaxPaymentNumber(lease);
        int newPaymentNumber = (lastPaymentNumber == null) ? 1 : lastPaymentNumber + 1;

        LocalDate startDate = calculatePaymentStartDate(lease);
        LocalDate endDate = startDate.plusMonths(monthsCovered).minusDays(1);

        PaymentSchedule schedule = new PaymentSchedule();
        schedule.setLease(lease);
        schedule.setPaymentNumber(newPaymentNumber);
        schedule.setMonthsCovered(monthsCovered);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
        schedule.setAmountPaid(amountPaid);
        schedule.setPaymentDate(paymentDate);
        schedule.setStatus(PaymentScheduleStatus.PAID);

        PaymentSchedule saved = scheduleRepository.save(schedule);

        List<Long> invoiceIds = generateInvoicesForPeriod(lease, startDate, monthsCovered, paymentDate);
        saved.setInvoiceIds(invoiceIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));

        scheduleRepository.save(saved);
        updateNextPaymentDueDate(lease);

        return saved;
    }

    private LocalDate calculatePaymentStartDate(Lease lease) {
        PaymentSchedule lastSchedule = scheduleRepository
                .findTopByLeaseOrderByPaymentNumberDesc(lease)
                .orElse(null);

        if (lastSchedule == null) {
            return lease.getStartDate();
        } else {
            return lastSchedule.getEndDate().plusDays(1);
        }
    }

    private List<Long> generateInvoicesForPeriod(Lease lease, LocalDate startDate,
                                                  Integer monthsCovered, LocalDate paymentDate) {
        List<Long> invoiceIds = new ArrayList<>();
        Tenant tenant = lease.getTenant();

        for (int i = 0; i < monthsCovered; i++) {
            LocalDate invoiceDate = startDate.plusMonths(i);
            LocalDate monthStart = invoiceDate.withDayOfMonth(1);
            LocalDate monthEnd = invoiceDate.withDayOfMonth(invoiceDate.lengthOfMonth());

            if (invoiceRepository.existsByLeaseAndIssueDateBetween(lease, monthStart, monthEnd)) {
                continue;
            }

            int dueDay = lease.getDueDay() != null ? lease.getDueDay() : 10;
            LocalDate dueDate = invoiceDate.withDayOfMonth(Math.min(dueDay, invoiceDate.lengthOfMonth()));

            BigDecimal rentInclusive = lease.getMonthlyRent();

            if (i == 0 && startDate.getDayOfMonth() > 15 &&
                scheduleRepository.findTopByLeaseOrderByPaymentNumberDesc(lease).isEmpty()) {
                rentInclusive = calculateProratedRent(lease, startDate);
            }

            BigDecimal penalty = taxCalculationService.calculatePenalty(tenant, rentInclusive, invoiceDate);
            BigDecimal totalAmount = rentInclusive.add(penalty);

            String invoiceNumber = generateInvoiceNumber();

            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber(invoiceNumber);
            invoice.setLease(lease);
            invoice.setRentAmount(rentInclusive);
            invoice.setPenaltyAmount(penalty);
            invoice.setTotalAmount(totalAmount);
            invoice.setIssueDate(invoiceDate);
            invoice.setDueDate(dueDate);
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaymentMethod("PREPAID");
            invoice.setPaymentDate(paymentDate);
            invoice.setCreatedAt(invoiceDate);

            Invoice saved = invoiceRepository.save(invoice);
            invoiceIds.add(saved.getId());
        }

        return invoiceIds;
    }

    private BigDecimal calculateProratedRent(Lease lease, LocalDate startDate) {
        int daysInMonth = startDate.lengthOfMonth();
        int daysRemaining = daysInMonth - startDate.getDayOfMonth() + 1;

        BigDecimal dailyRate = lease.getMonthlyRent()
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        long count = invoiceRepository.count() + 1;
        String seq = String.format("%04d", count);
        return "INV-" + year + "-" + seq;
    }

    private void updateNextPaymentDueDate(Lease lease) {
        PaymentSchedule lastSchedule = scheduleRepository
                .findTopByLeaseOrderByPaymentNumberDesc(lease)
                .orElse(null);

        if (lastSchedule != null) {
            LocalDate nextDue = lastSchedule.getEndDate().plusDays(1);
            if (nextDue.isBefore(LocalDate.now())) {
                nextDue = LocalDate.now();
            }
            lease.setNextPaymentDueDate(nextDue);
            lease.setPaymentScheduleActive(true);
            leaseRepository.save(lease);
        }
    }

    public int getTotalMonthsPaid(Lease lease) {
        Integer total = scheduleRepository.sumMonthsCoveredByStatus(
                lease,
                List.of(PaymentScheduleStatus.PAID)
        );
        return total == null ? 0 : total;
    }

    public LocalDate getNextPaymentDueDate(Lease lease) {
        return lease.getNextPaymentDueDate();
    }

    public boolean isCoveredByPrepayment(Lease lease, LocalDate date) {
        List<PaymentSchedule> paidSchedules = scheduleRepository
                .findByLeaseAndStatusOrderByPaymentNumberAsc(lease, PaymentScheduleStatus.PAID);

        for (PaymentSchedule schedule : paidSchedules) {
            if (!date.isBefore(schedule.getStartDate()) && !date.isAfter(schedule.getEndDate())) {
                return true;
            }
        }
        return false;
    }

    public List<PaymentSchedule> getSchedulesForLease(Lease lease) {
        return scheduleRepository.findByLeaseOrderByPaymentNumberAsc(lease);
    }

    public PaymentSchedule getCurrentSchedule(Lease lease) {
        LocalDate today = LocalDate.now();
        List<PaymentSchedule> schedules = scheduleRepository
                .findByLeaseOrderByPaymentNumberAsc(lease);

        for (PaymentSchedule schedule : schedules) {
            if (schedule.getStatus() == PaymentScheduleStatus.PAID &&
                !today.isBefore(schedule.getStartDate()) &&
                !today.isAfter(schedule.getEndDate())) {
                return schedule;
            }
        }
        return null;
    }
}