package com.rms.controller;

import com.rms.dto.LeaseRequest;
import com.rms.entity.Lease;
import com.rms.service.LeaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leases")
@CrossOrigin(origins = "http://localhost:5173")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @GetMapping
    public List<Lease> getAll() {
        return leaseService.getAll();
    }

    @GetMapping("/{id}")
    public Lease getById(@PathVariable Long id) {
        return leaseService.getById(id);
    }

    @GetMapping("/active")
    public List<Lease> getActive() {
        return leaseService.getActiveLeases();
    }

    @GetMapping("/tenant/{tenantId}")
    public List<Lease> getByTenant(@PathVariable Long tenantId) {
        return leaseService.getByTenant(tenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Lease create(@Valid @RequestBody LeaseRequest request) {
        return leaseService.createLease(request);
    }

    @PutMapping("/{id}")
    public Lease update(@PathVariable Long id, @Valid @RequestBody LeaseRequest request) {
        return leaseService.updateLease(id, request);
    }

    @PutMapping("/{id}/terminate")
    public Lease terminate(@PathVariable Long id) {
        return leaseService.terminateLease(id);
    }

    @PostMapping("/test/mark-expired")
    @ResponseStatus(HttpStatus.OK)
    public String testMarkExpired() {
        int count = leaseService.syncExpiredLeases();
        return count + " leases marked as EXPIRED";
    }

    @PostMapping("/test/{id}/force-expire")
    @ResponseStatus(HttpStatus.OK)
    public Lease forceExpire(@PathVariable Long id) {
        return leaseService.forceExpire(id);
    }
}
