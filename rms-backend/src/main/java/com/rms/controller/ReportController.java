package com.rms.controller;

import com.rms.dto.*;
import com.rms.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:5173")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** GET /api/reports/revenue?months=6 */
    @GetMapping("/revenue")
    public List<RevenueReportEntry> getRevenue(@RequestParam(defaultValue = "6") int months) {
        return reportService.getRevenueReport(months);
    }

    /** GET /api/reports/outstanding-balance */
    @GetMapping("/outstanding-balance")
    public List<OutstandingBalanceEntry> getOutstandingBalance() {
        return reportService.getOutstandingBalanceReport();
    }

    /** GET /api/reports/occupancy */
    @GetMapping("/occupancy")
    public OccupancyReport getOccupancy() {
        return reportService.getOccupancyReport();
    }

    /** GET /api/reports/lease-expiry?days=90 */
    @GetMapping("/lease-expiry")
    public List<leaseExpiryEntry> getLeaseExpiry(@RequestParam(defaultValue = "90") int days) {
        return reportService.getLeaseExpiryReport(days);
    }
}