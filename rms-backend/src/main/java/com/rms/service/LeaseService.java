package com.rms.service;

import com.rms.dto.LeaseRequest;
import com.rms.entity.*;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.LeaseRepository;
import com.rms.repository.SpaceRepository;
import com.rms.repository.TenantRepository;
import com.rms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final SpaceRepository spaceRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public LeaseService(LeaseRepository leaseRepository,
                        SpaceRepository spaceRepository,
                        TenantRepository tenantRepository,
                        UserRepository userRepository) {
        this.leaseRepository = leaseRepository;
        this.spaceRepository = spaceRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
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

    
    public Lease getLeaseByTenantUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tenant found for user: " + username));

        return leaseRepository.findByTenantIdAndStatus(tenant.getId(), LeaseStatus.ACTIVE)
                .orElse(null);
    }

    
    @Transactional
    public Lease createLease(LeaseRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id " + request.getTenantId()));

        Space space = spaceRepository.findById(request.getSpaceId())
                .orElseThrow(() -> new IllegalArgumentException("Space not found with id " + request.getSpaceId()));

        
        if (space.getStatus() != SpaceStatus.VACANT
                || leaseRepository.existsBySpaceAndStatus(space, LeaseStatus.ACTIVE)) {
            throw new IllegalArgumentException("Space " + space.getUnitNumber() + " is not vacant");
        }

        
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

        
        space.setStatus(SpaceStatus.OCCUPIED);
        spaceRepository.save(space);

        return saved;
    }

    
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