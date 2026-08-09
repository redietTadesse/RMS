package com.rms.controller;

import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:5173")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    
    @GetMapping
    public List<Invoice> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

    
    @GetMapping("/{id}")
    public Invoice getInvoiceById(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }

    
    @GetMapping("/status/{status}")
    public List<Invoice> getInvoicesByStatus(@PathVariable InvoiceStatus status) {
        return invoiceService.getInvoicesByStatus(status);
    }

    
    @GetMapping("/tenant/{tenantId}")
    public List<Invoice> getInvoicesByTenant(@PathVariable Long tenantId) {
        return invoiceService.getInvoicesByTenant(tenantId);
    }

    
    @PostMapping("/generate/{leaseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice generateInvoice(@PathVariable Long leaseId) {
        return invoiceService.generateInvoice(leaseId);
    }

    
    @PostMapping("/test/mark-overdue")
    @ResponseStatus(HttpStatus.OK)
    public String testMarkOverdue() {
        LocalDate today = LocalDate.now();
        List<Invoice> pendingInvoices = invoiceService.getInvoicesByStatus(InvoiceStatus.PENDING);
        int count = 0;

        for (Invoice invoice : pendingInvoices) {
            if (invoice.getDueDate().isBefore(today)) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                invoiceService.saveInvoice(invoice);
                invoiceService.notifyInvoiceOverdue(invoice);
                count++;
            }
        }

        return count + " invoices marked as OVERDUE";
    }

    
    @PostMapping("/test/{id}/force-overdue")
    @ResponseStatus(HttpStatus.OK)
    public Invoice forceOverdue(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        invoice.setDueDate(LocalDate.now().minusDays(5));
        invoice.setStatus(InvoiceStatus.OVERDUE);
        Invoice saved = invoiceService.saveInvoice(invoice);
        invoiceService.notifyInvoiceOverdue(saved);
        return saved;
    }
}
