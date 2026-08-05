package com.rms.repository;

import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    long countByStatus(InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.penaltyAmount), 0) FROM Invoice i")
    BigDecimal sumPenalties();
}
