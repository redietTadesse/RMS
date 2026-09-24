package com.rms.service;

import com.rms.dto.*;
import com.rms.entity.*;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.LeaseRepository;
import com.rms.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import com.rms.service.TimeTravelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final SpaceRepository spaceRepository;
    private final LeaseRepository leaseRepository;
    private final TimeTravelService timeTravelService;

    public ReportService(InvoiceRepository invoiceRepository,
                         SpaceRepository spaceRepository,
                         LeaseRepository leaseRepository,
                         TimeTravelService timeTravelService) {
        this.invoiceRepository = invoiceRepository;
        this.spaceRepository = spaceRepository;
        this.leaseRepository = leaseRepository;
        this.timeTravelService = timeTravelService;
    }

    /** Revenue Report: PAID invoice totals grouped by month, for the last `months` months */
    public List<RevenueReportEntry> getRevenueReport(int months) {
        LocalDate today = timeTravelService.getCurrentDate();
        DateTimeFormatter keyFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM yyyy");

        // Build ordered month buckets first (oldest -> newest), each starting at 0
        LinkedHashMap<String, RevenueReportEntry> buckets = new LinkedHashMap<>();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate d = today.minusMonths(i).withDayOfMonth(1);
            String key = d.format(keyFmt);
            buckets.put(key, new RevenueReportEntry(key, d.format(labelFmt), BigDecimal.ZERO));
        }

        LocalDate rangeStart = today.minusMonths(months - 1).withDayOfMonth(1);
        LocalDate rangeEnd = today.withDayOfMonth(today.lengthOfMonth());
        List<Invoice> paid = invoiceRepository.findPaidBetween(rangeStart, rangeEnd);

        Map<String, BigDecimal> sums = new HashMap<>();
        for (Invoice inv : paid) {
            String key = inv.getIssueDate().format(keyFmt);
            sums.merge(key, inv.getTotalAmount(), BigDecimal::add);
        }

        List<RevenueReportEntry> result = new ArrayList<>();
        for (var entry : buckets.entrySet()) {
            BigDecimal revenue = sums.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            result.add(new RevenueReportEntry(entry.getKey(), entry.getValue().label(), revenue));
        }
        return result;
    }

    /** Outstanding Balance Report: tenants with PENDING or OVERDUE invoices */
    public List<OutstandingBalanceEntry> getOutstandingBalanceReport() {
        List<Invoice> unpaid = invoiceRepository.findByStatusIn(
                List.of(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE));

        Map<Long, List<Invoice>> byTenant = unpaid.stream()
                .collect(Collectors.groupingBy(i -> i.getLease().getTenant().getId()));

        List<OutstandingBalanceEntry> result = new ArrayList<>();
        for (var entry : byTenant.entrySet()) {
            Tenant tenant = entry.getValue().get(0).getLease().getTenant();
            BigDecimal total = entry.getValue().stream()
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new OutstandingBalanceEntry(
                    tenant.getId(), tenant.getName(), tenant.getPhone(),
                    entry.getValue().size(), total));
        }
        // Highest balance first
        result.sort((a, b) -> b.totalOwed().compareTo(a.totalOwed()));
        return result;
    }

    /** Occupancy Report: vacant vs occupied spaces */
    public OccupancyReport getOccupancyReport() {
        long total = spaceRepository.count();
        long vacant = spaceRepository.countByStatus(SpaceStatus.VACANT);
        long occupied = spaceRepository.countByStatus(SpaceStatus.OCCUPIED);
        double vacantRate = total == 0 ? 0 : (vacant * 100.0) / total;
        double occupiedRate = total == 0 ? 0 : (occupied * 100.0) / total;
        return new OccupancyReport(total, vacant, occupied, vacantRate, occupiedRate);
    }

    /** Lease Expiry Report: ACTIVE leases ending within `maxDays` days, bucketed into 30/60/90 */
    public List<leaseExpiryEntry> getLeaseExpiryReport(int maxDays) {
        LocalDate today = timeTravelService.getCurrentDate();
        LocalDate cutoff = today.plusDays(maxDays);

        List<Lease> expiring = leaseRepository.findByStatusAndEndDateBetween(
                LeaseStatus.ACTIVE, today, cutoff);

        List<leaseExpiryEntry> result = new ArrayList<>();
        for (Lease lease : expiring) {
            long daysRemaining = ChronoUnit.DAYS.between(today, lease.getEndDate());
            String bucket = daysRemaining <= 30 ? "30" : daysRemaining <= 60 ? "60" : "90";
            result.add(new leaseExpiryEntry(
                    lease.getId(),
                    lease.getTenant().getName(),
                    lease.getSpace().getUnitNumber(),
                    lease.getEndDate(),
                    daysRemaining,
                    bucket));
        }
        result.sort(Comparator.comparingLong(leaseExpiryEntry::daysRemaining));
        return result;
    }
}