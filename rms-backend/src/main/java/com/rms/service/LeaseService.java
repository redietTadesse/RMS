package com.rms.service;

import com.rms.dto.LeaseRequest;
import com.rms.entity.*;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.LeaseRepository;
import com.rms.repository.SpaceRepository;
import com.rms.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final SpaceRepository spaceRepository;
    private final TenantRepository tenantRepository;

    public LeaseService(LeaseRepository leaseRepository,
                        SpaceRepository spaceRepository,
                        TenantRepository tenantRepository) {
        this.leaseRepository = leaseRepository;
        this.spaceRepository = spaceRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<Lease> getAll() {
        return leaseRepository.findAll();
    }

    public Lease getById(Long id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));
    }

    public List<Lease> getActiveLeases() {
        return leaseRepository.findByStatus(LeaseStatus.ACTIVE);
    }

    public List<Lease> getByTenant(Long tenantId) {
        return leaseRepository.findByTenantId(tenantId);
    }

    /**
     * Create a lease.
     * Business Rule 1: space must be Vacant (no other ACTIVE lease on it)
     * Business Rule 2: prepayment must be >= 3 months
     * Business Rule 3: space status -> OCCUPIED on creation
     */
    @Transactional
    public Lease createLease(LeaseRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id " + request.getTenantId()));

        Space space = spaceRepository.findById(request.getSpaceId())
                .orElseThrow(() -> new IllegalArgumentException("Space not found with id " + request.getSpaceId()));

        // Business Rule 1: space must be vacant
        if (space.getStatus() != SpaceStatus.VACANT
                || leaseRepository.existsBySpaceAndStatus(space, LeaseStatus.ACTIVE)) {
            throw new IllegalArgumentException("Space " + space.getUnitNumber() + " is not vacant");
        }

        // Business Rule 2: prepayment >= 3 months
        if (request.getPrepaymentMonths() == null || request.getPrepaymentMonths() < 3) {
            throw new IllegalArgumentException("Prepayment must be at least 3 months");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        Lease lease = new Lease();
        lease.setTenant(tenant);
        lease.setSpace(space);
        lease.setStartDate(request.getStartDate());
        lease.setEndDate(request.getEndDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setDeposit(request.getDeposit());
        lease.setPrepaymentMonths(request.getPrepaymentMonths());
        lease.setDueDay(request.getDueDay());
        lease.setStatus(LeaseStatus.ACTIVE);

        Lease saved = leaseRepository.save(lease);

        // Business Rule 3: space becomes Occupied
        space.setStatus(SpaceStatus.OCCUPIED);
        spaceRepository.save(space);

        return saved;
    }

    /**
     * Update a lease's editable fields. Does not change tenant/space assignment
     * or re-run the vacancy check — only terminate+recreate should reassign a space.
     */
    @Transactional
    public Lease updateLease(Long id, LeaseRequest request) {
        Lease lease = getById(id);

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active leases can be edited");
        }

        if (request.getPrepaymentMonths() == null || request.getPrepaymentMonths() < 3) {
            throw new IllegalArgumentException("Prepayment must be at least 3 months");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        lease.setStartDate(request.getStartDate());
        lease.setEndDate(request.getEndDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setDeposit(request.getDeposit());
        lease.setPrepaymentMonths(request.getPrepaymentMonths());
        lease.setDueDay(request.getDueDay());

        return leaseRepository.save(lease);
    }

    /**
     * Terminate a lease.
     * Business Rule 4: space status -> VACANT on termination
     */
    @Transactional
    public Lease terminateLease(Long id) {
        Lease lease = getById(id);

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active");
        }

        lease.setStatus(LeaseStatus.TERMINATED);
        leaseRepository.save(lease);

        Space space = lease.getSpace();
        space.setStatus(SpaceStatus.VACANT);
        spaceRepository.save(space);

        return lease;
    }
}