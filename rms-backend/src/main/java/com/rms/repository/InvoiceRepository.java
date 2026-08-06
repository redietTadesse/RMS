package com.rms.repository;

import com.rms.entity.Invoice;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.Lease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    long countByStatus(InvoiceStatus status);

    List<Invoice> findByStatus(InvoiceStatus status);

    List<Invoice> findByLease(Lease lease);

    List<Invoice> findByLeaseIdAndStatus(Long leaseId, InvoiceStatus status);

    List<Invoice> findByLeaseTenantId(Long tenantId);

    boolean existsByLeaseAndIssueDateBetween(
            Lease lease,
            LocalDate startDate,
            LocalDate endDate
    );

    @Query("SELECT COALESCE(SUM(i.penaltyAmount), 0) FROM Invoice i")
    BigDecimal sumPenalties();

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") InvoiceStatus status);
}