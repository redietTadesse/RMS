package com.rms.repository;

import com.rms.entity.TenantDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TenantDocumentRepository extends JpaRepository<TenantDocument, Long> {

    List<TenantDocument> findByTenantId(Long tenantId);

    long countByTenantId(Long tenantId);

    void deleteByTenantId(Long tenantId);

    /** Eagerly fetch tenant so ownership checks can run in the controller layer. */
    @Query("SELECT d FROM TenantDocument d JOIN FETCH d.tenant WHERE d.id = :id")
    Optional<TenantDocument> findByIdWithTenant(@Param("id") Long id);
}