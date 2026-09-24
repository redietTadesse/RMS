package com.rms.repository;

import com.rms.entity.InvoiceNumberSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface InvoiceNumberSequenceRepository extends JpaRepository<InvoiceNumberSequence, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceNumberSequence s WHERE s.year = :year")
    java.util.Optional<InvoiceNumberSequence> findForUpdate(@Param("year") Integer year);

    @Modifying
        @Query(value = "INSERT IGNORE INTO invoice_number_sequences (sequence_year, next_number) "
            + "SELECT :year, COALESCE(MAX(CAST(SUBSTRING_INDEX(invoice_number, '-', -1) AS UNSIGNED)), 0) + 1 "
            + "FROM invoices WHERE invoice_number LIKE CONCAT('INV-', :year, '-%')", nativeQuery = true)
    void initialize(@Param("year") Integer year);

    @Modifying
    @Query("UPDATE InvoiceNumberSequence s SET s.nextNumber = s.nextNumber + 1 WHERE s.year = :year")
    int increment(@Param("year") Integer year);
}
