package com.rms.repository;

import com.rms.entity.Lease;
import com.rms.entity.LeaseStatus;
import com.rms.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;   // add this import
<<<<<<< HEAD
@Repository
=======

>>>>>>> abc52cb (Resolve merge conflicts)
public interface LeaseRepository extends JpaRepository<Lease, Long> {

    long countByStatus(LeaseStatus status);

    List<Lease> findByStatus(LeaseStatus status);

    List<Lease> findByTenantId(Long tenantId);

    boolean existsBySpaceAndStatus(Space space, LeaseStatus status);

    List<Lease> findByStatusAndEndDateBetween(LeaseStatus status, LocalDate start, LocalDate end);

    // These were missing – add them back
    List<Lease> findByTenantIdAndStatus(Long tenantId, LeaseStatus status);

    Optional<Lease> findFirstByTenantIdOrderByEndDateDesc(Long tenantId);
}