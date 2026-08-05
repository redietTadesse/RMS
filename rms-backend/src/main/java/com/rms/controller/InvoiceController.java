package com.rms.controller;

import com.rms.entity.Invoice;
import com.rms.repository.InvoiceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only for now — invoice generation/payment recording comes in the
 * billing sprint. Newest first so the dashboard can show recent payments.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    public InvoiceController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public List<Invoice> getAll() {
        return invoiceRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }
}
