// src/main/java/com/rms/controller/DocumentController.java
package com.rms.controller;

import com.rms.entity.DocumentType;
import com.rms.entity.TenantDocument;
import com.rms.service.CurrentUserService;
import com.rms.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:5173")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);


    private final DocumentService documentService;
    private final CurrentUserService currentUserService;

    public DocumentController(DocumentService documentService, CurrentUserService currentUserService) {
        this.documentService = documentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TenantDocument>> getDocumentsForTenant(@PathVariable Long tenantId) {
        try {
            List<TenantDocument> documents = documentService.getDocumentsForTenant(tenantId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting documents for tenant", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/my-documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
    public ResponseEntity<List<TenantDocument>> getMyDocuments(org.springframework.security.core.Authentication authentication) {
        try {
            String username = authentication.getName();
            List<TenantDocument> documents = documentService.getDocumentsForTenantByUsername(username);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error getting my documents", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/upload/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantDocument> uploadDocument(
            @PathVariable Long tenantId,
            @RequestParam DocumentType type,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            org.springframework.security.core.Authentication authentication) {
        try {
            String uploadedBy = authentication != null ? authentication.getName() : "admin";
            TenantDocument document = documentService.uploadDocument(tenantId, type, file, description, uploadedBy);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        log.debug("Download requested for document ID: {}", documentId);

        // Authorize BEFORE reading the file. Admin OR the owning tenant only.
        TenantDocument document = documentService.getDocumentByIdWithTenant(documentId);
        currentUserService.requireDocumentAccess(document);

        if (document.getFilePath() == null || document.getFilePath().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path filePath = Paths.get(document.getFilePath());

            if (!filePath.toFile().exists()) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            try {
                contentType = java.nio.file.Files.probeContentType(filePath);
            } catch (Exception ignored) { }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Download error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting document", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}