package com.rms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice_number_sequences")
public class InvoiceNumberSequence {

    @Id
    @Column(name = "sequence_year")
    private Integer year;

    @Column(name = "next_number", nullable = false)
    private Long nextNumber = 1L;

    public InvoiceNumberSequence() {
    }

    public InvoiceNumberSequence(Integer year) {
        this.year = year;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Long getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(Long nextNumber) {
        this.nextNumber = nextNumber;
    }
}
