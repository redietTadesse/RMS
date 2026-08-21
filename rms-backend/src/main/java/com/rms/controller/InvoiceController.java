package com.rms.controller;

import com.rms.dto.InvoiceResponse;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:5173")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceService.getAllInvoices().stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public InvoiceResponse getInvoiceById(@PathVariable Long id) {
        return InvoiceResponse.from(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/status/{status}")
    public List<InvoiceResponse> getInvoicesByStatus(@PathVariable InvoiceStatus status) {
        return invoiceService.getInvoicesByStatus(status).stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/tenant/{tenantId}")
    public List<InvoiceResponse> getInvoicesByTenant(@PathVariable Long tenantId) {
        return invoiceService.getInvoicesByTenant(tenantId).stream()
                .map(InvoiceResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/generate/{leaseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<InvoiceResponse> generateInvoice(@PathVariable Long leaseId) {
        Invoice invoice = invoiceService.generateInvoice(leaseId);
        if (invoice == null) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(InvoiceResponse.from(invoice));
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
    public InvoiceResponse forceOverdue(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        invoice.setDueDate(LocalDate.now().minusDays(5));
        invoice.setStatus(InvoiceStatus.OVERDUE);
        Invoice saved = invoiceService.saveInvoice(invoice);
        invoiceService.notifyInvoiceOverdue(saved);
        return InvoiceResponse.from(saved);
    }
}