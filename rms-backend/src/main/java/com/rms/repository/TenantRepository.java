package com.rms.repository;

import com.rms.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    // Search by name, email, or TIN with a single query param
    @Query("""
           SELECT t FROM Tenant t
           WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(t.email) LIKE LOWER(CONCAT('%', :q, '%'))
              OR t.tinNumber LIKE CONCAT('%', :q, '%')
           """)
    List<Tenant> search(@Param("q") String q);
}
