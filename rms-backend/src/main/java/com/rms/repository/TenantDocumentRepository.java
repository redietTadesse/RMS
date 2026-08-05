package com.rms.repository;

import com.rms.entity.TenantDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TenantDocumentRepository extends JpaRepository<TenantDocument, Long> {
    List<TenantDocument> findByTenantId(Long tenantId);
}
