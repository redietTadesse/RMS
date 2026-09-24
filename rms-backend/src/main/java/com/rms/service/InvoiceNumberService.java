package com.rms.service;

import com.rms.entity.InvoiceNumberSequence;
import com.rms.repository.InvoiceNumberSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class InvoiceNumberService {

    private final InvoiceNumberSequenceRepository sequenceRepository;

    public InvoiceNumberService(InvoiceNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String nextInvoiceNumber(LocalDate date) {
        int year = date.getYear();
        sequenceRepository.initialize(year);
        InvoiceNumberSequence sequence = sequenceRepository.findForUpdate(year)
                .orElseThrow(() -> new IllegalStateException("Invoice number sequence was not initialized"));
        long number = sequence.getNextNumber();
        sequence.setNextNumber(number + 1);
        sequenceRepository.save(sequence);
        return String.format("INV-%d-%04d", year, number);
    }
}