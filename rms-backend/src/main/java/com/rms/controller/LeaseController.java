package com.rms.controller;

import com.rms.entity.Lease;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.LeaseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only for now — full lease management comes in the leases sprint.
 * Newest first so the dashboard can show recent leases directly.
 */
@RestController
@RequestMapping("/api/leases")
@CrossOrigin(origins = "http://localhost:5173")
public class LeaseController {

    private final LeaseRepository leaseRepository;

    public LeaseController(LeaseRepository leaseRepository) {
        this.leaseRepository = leaseRepository;
    }

    @GetMapping
    public List<Lease> getAll() {
        return leaseRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @GetMapping("/{id}")
    public Lease getById(@PathVariable Long id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));
    }
}
