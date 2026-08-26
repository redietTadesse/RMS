package com.rms.service;

import com.rms.entity.Tenant;
import com.rms.entity.User;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.TenantRepository;
import com.rms.repository.UserRepository;
import com.rms.repository.LeaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final LeaseRepository leaseRepository;

    public TenantService(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         LeaseRepository leaseRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.leaseRepository = leaseRepository;
    }

    public List<Tenant> getAll(String search) {
        return (search == null || search.isBlank())
                ? tenantRepository.findAll()
                : tenantRepository.search(search.trim());
    }

    public Tenant getById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id " + id));
    }

    public Tenant create(Tenant tenant) {
        tenant.setId(null);

        if (tenant.getUserId() != null) {
            User user = userRepository.findById(tenant.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + tenant.getUserId()));
            if (user.getRole() != com.rms.entity.Role.TENANT) {
                throw new IllegalArgumentException("User must have TENANT role");
            }

            if (tenantRepository.findByUserId(tenant.getUserId()).isPresent()) {
                throw new IllegalArgumentException("User already linked to another tenant");
            }
        }

        return tenantRepository.save(tenant);
    }

    public Tenant update(Long id, Tenant updated) {
        Tenant tenant = getById(id);
        tenant.setName(updated.getName());
        tenant.setType(updated.getType());
        tenant.setContact(updated.getContact());
        tenant.setPhone(updated.getPhone());
        tenant.setEmail(updated.getEmail());
        tenant.setTinNumber(updated.getTinNumber());
        tenant.setBusinessLicense(updated.getBusinessLicense());
        tenant.setHasIdCard(updated.isHasIdCard());
        tenant.setHasBusinessLicense(updated.isHasBusinessLicense());

        if (updated.getUserId() != null) {
            User user = userRepository.findById(updated.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + updated.getUserId()));
            if (user.getRole() != com.rms.entity.Role.TENANT) {
                throw new IllegalArgumentException("User must have TENANT role");
            }

            if (tenantRepository.findByUserId(updated.getUserId()).isPresent()) {
                Tenant existing = tenantRepository.findByUserId(updated.getUserId()).get();
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("User already linked to another tenant");
                }
            }
        }

        tenant.setUserId(updated.getUserId());

        return tenantRepository.save(tenant);
    }

    public void delete(Long id) {
        Tenant tenant = getById(id);

        List<com.rms.entity.Lease> activeLeases = leaseRepository.findByTenantIdAndStatus(id, com.rms.entity.LeaseStatus.ACTIVE);
        if (!activeLeases.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete tenant with active leases");
        }

        tenantRepository.delete(tenant);
    }
}