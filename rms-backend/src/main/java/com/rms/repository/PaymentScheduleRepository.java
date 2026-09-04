package com.rms.repository;

import com.rms.entity.Lease;
import com.rms.entity.PaymentSchedule;
import com.rms.entity.PaymentScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    List<PaymentSchedule> findByLeaseAndStatusOrderByPaymentNumberAsc(Lease lease, PaymentScheduleStatus status);

    List<PaymentSchedule> findByLeaseOrderByPaymentNumberAsc(Lease lease);

    Optional<PaymentSchedule> findTopByLeaseOrderByPaymentNumberDesc(Lease lease);

    @Query("SELECT MAX(ps.paymentNumber) FROM PaymentSchedule ps WHERE ps.lease = :lease")
    Integer findMaxPaymentNumber(@Param("lease") Lease lease);

    @Query("SELECT ps FROM PaymentSchedule ps WHERE ps.lease = :lease AND ps.status = 'PENDING' AND ps.startDate <= :date")
    List<PaymentSchedule> findPendingSchedulesBeforeDate(@Param("lease") Lease lease, @Param("date") LocalDate date);

    long countByLeaseAndStatus(Lease lease, PaymentScheduleStatus status);

    @Query("SELECT SUM(ps.monthsCovered) FROM PaymentSchedule ps WHERE ps.lease = :lease AND ps.status IN :statuses")
    Integer sumMonthsCoveredByStatus(@Param("lease") Lease lease, @Param("statuses") List<PaymentScheduleStatus> statuses);
}