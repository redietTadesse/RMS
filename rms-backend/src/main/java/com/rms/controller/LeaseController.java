package com.rms.controller;
import com.rms.dto.LeaseRequest;
import com.rms.dto.LeaseResponseDTO;
import com.rms.service.LeaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leases")
@CrossOrigin(origins = "http://localhost:5173")
@PreAuthorize("hasRole('ADMIN')")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }


    @GetMapping
    public List<LeaseResponseDTO> getAll() {
        return leaseService.getAll();
    }

    @GetMapping("/{id}")
    public LeaseResponseDTO getById(@PathVariable Long id) {
        return leaseService.getById(id);
    }

    @GetMapping("/active")
    public List<LeaseResponseDTO> getActive() {
        return leaseService.getActiveLeases();
    }

    @GetMapping("/tenant/{tenantId}")
    public List<LeaseResponseDTO> getByTenant(@PathVariable Long tenantId) {
        return leaseService.getByTenant(tenantId);
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaseResponseDTO create(@Valid @RequestBody LeaseRequest request) {
        return leaseService.createLease(request);
    }

    @PutMapping("/{id}")
    public LeaseResponseDTO update(@PathVariable Long id,
                                   @Valid @RequestBody LeaseRequest request) {
        return leaseService.updateLease(id, request);
    }

    @PutMapping("/{id}/terminate")
    public LeaseResponseDTO terminate(@PathVariable Long id) {
        return leaseService.terminateLease(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        leaseService.deleteLease(id);
    }


    @PostMapping("/test/mark-expired")
    @ResponseStatus(HttpStatus.OK)
    public String testMarkExpired() {
        int count = leaseService.syncExpiredLeases();
        return count + " leases marked as EXPIRED";
    }

    @PostMapping("/test/{id}/force-expire")
    @ResponseStatus(HttpStatus.OK)
    public LeaseResponseDTO forceExpire(@PathVariable Long id) {
        return leaseService.forceExpire(id);
    }
}
