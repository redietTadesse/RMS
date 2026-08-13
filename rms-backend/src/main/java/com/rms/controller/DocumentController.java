// src/main/java/com/rms/controller/DocumentController.java
package com.rms.controller;

import com.rms.entity.DocumentType;
import com.rms.entity.TenantDocument;
import com.rms.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "http://localhost:5173")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TenantDocument>> getDocumentsForTenant(@PathVariable Long tenantId) {
        try {
            List<TenantDocument> documents = documentService.getDocumentsForTenant(tenantId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            System.err.println("Error getting documents: " + e.getMessage());
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
            System.err.println("Error getting my documents: " + e.getMessage());
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
            System.err.println("Error uploading document: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        try {
            System.out.println("📥 Download requested for document ID: " + documentId);

            TenantDocument document = documentService.getDocumentById(documentId);

            if (document.getFilePath() == null || document.getFilePath().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(document.getFilePath());

            if (!filePath.toFile().exists()) {
                System.err.println("File not found: " + filePath.toString());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            try {
                contentType = java.nio.file.Files.probeContentType(filePath);
            } catch (Exception e) {
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            System.err.println("❌ Download error: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Error deleting document: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}