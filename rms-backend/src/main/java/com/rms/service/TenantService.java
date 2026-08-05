package com.rms.service;

import com.rms.entity.Tenant;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
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
        return tenantRepository.save(tenant);
    }

    public void delete(Long id) {
        Tenant tenant = getById(id);
        tenantRepository.delete(tenant);
    }
}
