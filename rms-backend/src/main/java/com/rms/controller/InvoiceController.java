package com.rms.controller;

import com.rms.dto.InvoiceResponse;
import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.service.CurrentUserService;
import com.rms.service.InvoicePdfService;
import com.rms.service.InvoiceService;
import com.rms.service.TimeTravelService;
import com.rms.service.TaxCalculationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices")
@CrossOrigin(origins = "http://localhost:5173")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final TaxCalculationService taxCalculationService;
    private final TimeTravelService timeTravelService;
    private final CurrentUserService currentUserService;

    public InvoiceController(InvoiceService invoiceService,
                             InvoicePdfService invoicePdfService,
                             TaxCalculationService taxCalculationService,
                             TimeTravelService timeTravelService,
                             CurrentUserService currentUserService) {
        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
        this.taxCalculationService = taxCalculationService;
        this.timeTravelService = timeTravelService;
        this.currentUserService = currentUserService;
        InvoiceResponse.setTimeTravelService(timeTravelService);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceService.getAllInvoices().stream()
                .map(inv -> InvoiceResponse.from(inv, taxCalculationService))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
    public InvoiceResponse getInvoiceById(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        currentUserService.requireInvoiceAccess(invoice);
        return InvoiceResponse.from(invoice, taxCalculationService);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceResponse> getInvoicesByStatus(@PathVariable InvoiceStatus status) {
        return invoiceService.getInvoicesByStatus(status).stream()
                .map(inv -> InvoiceResponse.from(inv, taxCalculationService))
                .collect(Collectors.toList());
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceResponse> getInvoicesByTenant(@PathVariable Long tenantId) {
        return invoiceService.getInvoicesByTenant(tenantId).stream()
                .map(inv -> InvoiceResponse.from(inv, taxCalculationService))
                .collect(Collectors.toList());
    }

    @PostMapping("/generate/{leaseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<InvoiceResponse> generateInvoice(@PathVariable Long leaseId) {
        Invoice invoice = invoiceService.generateInvoice(leaseId);
        if (invoice == null) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(InvoiceResponse.from(invoice, taxCalculationService));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        // Authorize OUTSIDE the try/catch so the 403 propagates to the client.
        Invoice invoice = invoiceService.getInvoiceById(id);
        currentUserService.requireInvoiceAccess(invoice);

        try {
            byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoice);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "invoice-" + invoice.getInvoiceNumber() + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/test/mark-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public String testMarkOverdue() {
        LocalDate today = timeTravelService.getCurrentDate();
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
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public InvoiceResponse forceOverdue(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        invoice.setDueDate(timeTravelService.getCurrentDate().minusDays(5));
        invoice.setStatus(InvoiceStatus.OVERDUE);
        Invoice saved = invoiceService.saveInvoice(invoice);
        invoiceService.notifyInvoiceOverdue(saved);
        return InvoiceResponse.from(saved, taxCalculationService);
    }
}