package com.rms.repository;

import com.rms.entity.Space;
import com.rms.entity.SpaceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, Long> {
    List<Space> findByStatus(SpaceStatus status);
    long countByStatus(SpaceStatus status);
    boolean existsByUnitNumber(String unitNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Space s WHERE s.id = :id")
    Optional<Space> findByIdForUpdate(@Param("id") Long id);
}
