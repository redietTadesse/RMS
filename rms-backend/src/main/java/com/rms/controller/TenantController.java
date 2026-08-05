package com.rms.controller;

import com.rms.entity.DocumentType;
import com.rms.entity.Tenant;
import com.rms.entity.TenantDocument;
import com.rms.service.DocumentService;
import com.rms.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final DocumentService documentService;

    public TenantController(TenantService tenantService, DocumentService documentService) {
        this.tenantService = tenantService;
        this.documentService = documentService;
    }

    /** GET /api/tenants  or  GET /api/tenants?search=abebe (matches name, email, or TIN) */
    @GetMapping
    public List<Tenant> getAll(@RequestParam(required = false) String search) {
        return tenantService.getAll(search);
    }

    @GetMapping("/{id}")
    public Tenant getById(@PathVariable Long id) {
        return tenantService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tenant create(@Valid @RequestBody Tenant tenant) {
        return tenantService.create(tenant);
    }

    @PutMapping("/{id}")
    public Tenant update(@PathVariable Long id, @Valid @RequestBody Tenant tenant) {
        return tenantService.update(id, tenant);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tenantService.delete(id);
    }

    // ===== Documents =====

    /**
     * POST /api/tenants/{id}/documents
     * multipart/form-data with fields:
     *   - file: the uploaded file
     *   - type: TIN_CERTIFICATE | ID_CARD | BUSINESS_LICENSE
     */
    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantDocument uploadDocument(@PathVariable Long id,
                                         @RequestParam("type") DocumentType type,
                                         @RequestParam("file") MultipartFile file) {
        return documentService.upload(id, type, file);
    }

    /** GET /api/tenants/{id}/documents */
    @GetMapping("/{id}/documents")
    public List<TenantDocument> getDocuments(@PathVariable Long id) {
        return documentService.getByTenant(id);
    }

    /** GET /api/tenants/{tenantId}/documents/{documentId}/download */
    @GetMapping("/{tenantId}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long tenantId,
                                                     @PathVariable Long documentId) {
        TenantDocument doc = documentService.getById(documentId);
        FileSystemResource resource = new FileSystemResource(doc.getFilePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }
}
