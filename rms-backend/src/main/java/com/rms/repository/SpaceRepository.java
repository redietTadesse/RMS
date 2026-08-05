package com.rms.repository;

import com.rms.entity.Space;
import com.rms.entity.SpaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpaceRepository extends JpaRepository<Space, Long> {
    List<Space> findByStatus(SpaceStatus status);
    long countByStatus(SpaceStatus status);
    boolean existsByUnitNumber(String unitNumber);
}
