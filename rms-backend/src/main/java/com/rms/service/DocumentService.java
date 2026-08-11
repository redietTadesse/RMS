package com.rms.service;

import com.rms.entity.DocumentType;
import com.rms.entity.Tenant;
import com.rms.entity.TenantDocument;
import com.rms.entity.User;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.TenantDocumentRepository;
import com.rms.repository.TenantRepository;
import com.rms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    private final TenantDocumentRepository documentRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public DocumentService(TenantDocumentRepository documentRepository,
                           TenantRepository tenantRepository,
                           UserRepository userRepository,
                           FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Upload a document for a tenant
     */
    @Transactional
    public TenantDocument uploadDocument(Long tenantId, DocumentType type,
                                         MultipartFile file, String description,
                                         String uploadedBy) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id: " + tenantId));

        // Store file
        String filePath = fileStorageService.storeDocument(file, tenantId, type.name());

        // Create document record
        TenantDocument document = new TenantDocument();
        document.setTenant(tenant);
        document.setType(type);
        document.setFileName(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setDescription(description != null ? description : "");
        document.setUploadedBy(uploadedBy != null ? uploadedBy : "system");
        document.setUploadedAt(LocalDateTime.now());

        return documentRepository.save(document);
    }

    /**
     * Get all documents for a tenant
     */
    public List<TenantDocument> getDocumentsForTenant(Long tenantId) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        return documentRepository.findByTenantId(tenantId);
    }

    /**
     * Get documents for the logged-in tenant
     */
    public List<TenantDocument> getDocumentsForTenantByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tenant found for user: " + username));

        return documentRepository.findByTenantId(tenant.getId());
    }

    /**
     * Get document by ID
     */
    public TenantDocument getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
    }

    /**
     * Delete a document
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        TenantDocument document = getDocumentById(documentId);

        // Delete physical file
        boolean deleted = fileStorageService.deleteFile(document.getFilePath());
        if (!deleted) {
            System.err.println("Warning: Could not delete file: " + document.getFilePath());
        }

        // Delete database record
        documentRepository.delete(document);
    }

    /**
     * Get file path for download
     */
    public String getFilePath(Long documentId) {
        TenantDocument document = getDocumentById(documentId);
        return document.getFilePath();
    }

    /**
     * Count documents for a tenant
     */
    public long countDocumentsForTenant(Long tenantId) {
        return documentRepository.countByTenantId(tenantId);
    }
}