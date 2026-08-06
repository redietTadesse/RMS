package com.rms.controller;

import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:5173")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Get all invoices (newest first)
     */
    @GetMapping
    public List<Invoice> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

    /**
     * Get invoice by ID
     */
    @GetMapping("/{id}")
    public Invoice getInvoiceById(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }

    /**
     * Get invoices by status
     */
    @GetMapping("/status/{status}")
    public List<Invoice> getInvoicesByStatus(@PathVariable InvoiceStatus status) {
        return invoiceService.getInvoicesByStatus(status);
    }

    /**
     * Get invoices for a tenant
     */
    @GetMapping("/tenant/{tenantId}")
    public List<Invoice> getInvoicesByTenant(@PathVariable Long tenantId) {
        return invoiceService.getInvoicesByTenant(tenantId);
    }

    /**
     * Manually generate an invoice for a lease (admin only)
     */
    @PostMapping("/generate/{leaseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice generateInvoice(@PathVariable Long leaseId) {
        return invoiceService.generateInvoice(leaseId);
    }
}