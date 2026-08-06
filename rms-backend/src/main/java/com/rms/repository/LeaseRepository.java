package com.rms.repository;

import com.rms.entity.Lease;
import com.rms.entity.LeaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaseRepository extends JpaRepository<Lease, Long> {

    long countByStatus(LeaseStatus status);

    List<Lease> findByStatus(LeaseStatus status);
}