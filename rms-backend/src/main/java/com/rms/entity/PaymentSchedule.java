package com.rms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payment_schedules")
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lease_id")
    private Lease lease;

    @Column(name = "payment_number", nullable = false)
    private Integer paymentNumber;

    @Column(name = "months_covered", nullable = false)
    private Integer monthsCovered;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentScheduleStatus status = PaymentScheduleStatus.PENDING;

    @Column(name = "invoice_ids")
    private String invoiceIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt = LocalDate.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lease getLease() { return lease; }
    public void setLease(Lease lease) { this.lease = lease; }

    public Integer getPaymentNumber() { return paymentNumber; }
    public void setPaymentNumber(Integer paymentNumber) { this.paymentNumber = paymentNumber; }

    public Integer getMonthsCovered() { return monthsCovered; }
    public void setMonthsCovered(Integer monthsCovered) { this.monthsCovered = monthsCovered; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public PaymentScheduleStatus getStatus() { return status; }
    public void setStatus(PaymentScheduleStatus status) { this.status = status; }

    public String getInvoiceIds() { return invoiceIds; }
    public void setInvoiceIds(String invoiceIds) { this.invoiceIds = invoiceIds; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}