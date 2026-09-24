package com.rms.controller;

import com.rms.dto.*;
import com.rms.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@CrossOrigin(origins = "http://localhost:5173")
public class TenantsController {

    private final TenantService tenantService;

    public TenantsController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<TenantResponseDTO> getAll(@RequestParam(required = false) String search) {
        return tenantService.getAll(search);
    }

    @GetMapping("/{id}")
    public TenantResponseDTO getById(@PathVariable Long id) {
        return tenantService.getById(id);
    }

    @PostMapping
    public TenantResponseDTO create(@Valid @RequestBody TenantCreateRequest request) {
        return tenantService.create(request);
    }

    @PutMapping("/{id}")
    public TenantResponseDTO update(@PathVariable Long id, @Valid @RequestBody TenantUpdateRequest request) {
        return tenantService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        tenantService.delete(id);
    }
}
