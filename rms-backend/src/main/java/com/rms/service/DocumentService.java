package com.rms.service;

import com.rms.entity.DocumentType;
import com.rms.entity.Tenant;
import com.rms.entity.TenantDocument;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.TenantDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
public class DocumentService {

    private final TenantDocumentRepository documentRepository;
    private final TenantService tenantService;
    private final FileStorageService fileStorageService;

    public DocumentService(TenantDocumentRepository documentRepository,
                           TenantService tenantService,
                           FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.tenantService = tenantService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public TenantDocument upload(Long tenantId, DocumentType type, MultipartFile file) {
        Tenant tenant = tenantService.getById(tenantId);
        Path storedPath = fileStorageService.store(tenantId, file);

        TenantDocument doc = new TenantDocument();
        doc.setTenant(tenant);
        doc.setType(type);
        doc.setFileName(file.getOriginalFilename());
        doc.setFilePath(storedPath.toString());
        TenantDocument saved = documentRepository.save(doc);

        // Keep the tenant's document flags in sync (SRS: docs required before lease creation)
        if (type == DocumentType.ID_CARD) {
            tenant.setHasIdCard(true);
        } else if (type == DocumentType.BUSINESS_LICENSE) {
            tenant.setHasBusinessLicense(true);
        }
        return saved;
    }

    public List<TenantDocument> getByTenant(Long tenantId) {
        tenantService.getById(tenantId); // 404 if tenant doesn't exist
        return documentRepository.findByTenantId(tenantId);
    }

    public TenantDocument getById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id " + documentId));
    }
}
