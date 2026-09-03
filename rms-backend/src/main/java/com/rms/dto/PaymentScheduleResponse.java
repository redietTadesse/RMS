package com.rms.dto;

import com.rms.entity.PaymentSchedule;
import com.rms.entity.PaymentScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentScheduleResponse {
    private Long id;
    private Integer paymentNumber;
    private Integer monthsCovered;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amountPaid;
    private LocalDate paymentDate;
    private PaymentScheduleStatus status;
    private String invoiceIds;

    public static PaymentScheduleResponse from(PaymentSchedule schedule) {
        PaymentScheduleResponse response = new PaymentScheduleResponse();
        response.setId(schedule.getId());
        response.setPaymentNumber(schedule.getPaymentNumber());
        response.setMonthsCovered(schedule.getMonthsCovered());
        response.setStartDate(schedule.getStartDate());
        response.setEndDate(schedule.getEndDate());
        response.setAmountPaid(schedule.getAmountPaid());
        response.setPaymentDate(schedule.getPaymentDate());
        response.setStatus(schedule.getStatus());
        response.setInvoiceIds(schedule.getInvoiceIds());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
}