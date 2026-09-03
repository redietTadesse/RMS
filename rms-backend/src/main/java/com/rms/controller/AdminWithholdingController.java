package com.rms.controller;

import com.rms.entity.Tenant;
import com.rms.entity.TenantDocument;
import com.rms.service.DocumentService;
import com.rms.service.TenantService;
import com.rms.service.TaxCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/withholding")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminWithholdingController {

    private final TenantService tenantService;
    private final DocumentService documentService;
    private final TaxCalculationService taxCalculationService;

    public AdminWithholdingController(TenantService tenantService,
                                      DocumentService documentService,
                                      TaxCalculationService taxCalculationService) {
        this.tenantService = tenantService;
        this.documentService = documentService;
        this.taxCalculationService = taxCalculationService;
    }

    @PostMapping("/{tenantId}/evidence")
    public ResponseEntity<Map<String, Object>> uploadWithholdingEvidence(
            @PathVariable Long tenantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String tinNumber,
            @RequestParam(required = false) String startDate,
            org.springframework.security.core.Authentication authentication) {
        
        Tenant tenant = tenantService.getById(tenantId);
        String uploadedBy = authentication != null ? authentication.getName() : "admin";

        TenantDocument document = documentService.uploadDocument(
                tenantId,
                com.rms.entity.DocumentType.TIN_CERTIFICATE,
                file,
                "Withholding Tax Evidence - TIN Certificate",
                uploadedBy
        );

        if (tinNumber != null && !tinNumber.isBlank()) {
            tenant.setTinNumber(tinNumber);
        }
        tenant.setWithholdingDocumentId(document.getId());
        tenantService.update(tenantId, tenant);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Withholding evidence uploaded successfully");
        response.put("documentId", document.getId());
        response.put("pendingApproval", true);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tenantId}/activate")
    public ResponseEntity<Map<String, Object>> activateWithholding(
            @PathVariable Long tenantId,
            @RequestParam(required = false) String startDate,
            org.springframework.security.core.Authentication authentication) {
        
        Tenant tenant = tenantService.getById(tenantId);
        String verifiedBy = authentication != null ? authentication.getName() : "admin";

        LocalDate effectiveDate = startDate != null ? LocalDate.parse(startDate) : LocalDate.now();
        
        tenant.setWithholdingTaxApplicable(true);
        tenant.setWithholdingStartDate(effectiveDate);
        tenant.setWithholdingEvidenceVerified(true);
        tenant.setWithholdingVerifiedBy(verifiedBy);
        tenant.setWithholdingVerifiedAt(LocalDateTime.now());

        tenantService.update(tenantId, tenant);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Withholding tax activated successfully");
        response.put("effectiveDate", effectiveDate);
        response.put("verifiedBy", verifiedBy);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tenantId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateWithholding(
            @PathVariable Long tenantId,
            org.springframework.security.core.Authentication authentication) {
        
        Tenant tenant = tenantService.getById(tenantId);
        
        tenant.setWithholdingTaxApplicable(false);
        tenant.setWithholdingEvidenceVerified(false);
        tenant.setWithholdingVerifiedBy(authentication != null ? authentication.getName() : "admin");
        tenant.setWithholdingVerifiedAt(LocalDateTime.now());

        tenantService.update(tenantId, tenant);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Withholding tax deactivated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tenantId}/preview")
    public ResponseEntity<Map<String, Object>> previewWithholdingImpact(
            @PathVariable Long tenantId,
            @RequestParam BigDecimal rentAmount,
            @RequestParam(required = false) String invoiceDate) {
        
        Tenant tenant = tenantService.getById(tenantId);
        LocalDate date = invoiceDate != null ? LocalDate.parse(invoiceDate) : LocalDate.now();

        TaxCalculationService.TaxBreakdown breakdown = 
            taxCalculationService.getTaxBreakdown(tenant, rentAmount, date);

        Map<String, Object> response = new HashMap<>();
        response.put("tenantName", tenant.getName());
        response.put("withholdingApplicable", breakdown.isWithholdingApplicable());
        response.put("rentInclusive", breakdown.getRentInclusive());
        response.put("untaxedRent", breakdown.getUntaxedRent());
        response.put("vatAmount", breakdown.getVatAmount());
        response.put("withholdingTax", breakdown.getWithholdingTax());
        response.put("penalty", breakdown.getPenalty());
        response.put("totalAmount", breakdown.getTotalAmount());
        response.put("effectiveDate", tenant.getWithholdingStartDate());
        response.put("tinNumber", tenant.getTinNumber());
        response.put("hasEvidence", tenant.getWithholdingDocumentId() != null);
        response.put("evidenceVerified", tenant.isWithholdingEvidenceVerified());
        
        return ResponseEntity.ok(response);
    }
}