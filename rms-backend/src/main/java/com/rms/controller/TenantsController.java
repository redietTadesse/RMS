package com.rms.controller;

import com.rms.entity.Tenant;
import com.rms.service.TenantService;
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
    public List<Tenant> getAll(@RequestParam(required = false) String search) {
        return tenantService.getAll(search);
    }

    @GetMapping("/{id}")
    public Tenant getById(@PathVariable Long id) {
        return tenantService.getById(id);
    }

    @PostMapping
    public Tenant create(@RequestBody Tenant tenant) {
        return tenantService.create(tenant);
    }

    @PutMapping("/{id}")
    public Tenant update(@PathVariable Long id, @RequestBody Tenant tenant) {
        return tenantService.update(id, tenant);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        tenantService.delete(id);
    }
}
