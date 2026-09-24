package com.rms.service;

import com.rms.dto.*;
import com.rms.entity.Tenant;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.TenantRepository;
import com.rms.repository.LeaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final LeaseRepository leaseRepository;

    public TenantService(TenantRepository tenantRepository,
                         LeaseRepository leaseRepository) {
        this.tenantRepository = tenantRepository;
        this.leaseRepository = leaseRepository;
    }

    public List<TenantResponseDTO> getAll(String search) {
        List<Tenant> tenants = (search == null || search.isBlank())
                ? tenantRepository.findAll()
                : tenantRepository.search(search.trim());

        return tenants.stream()
                .map(TenantResponseDTO::from)
                .collect(Collectors.toList());
    }

    public TenantResponseDTO getById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id " + id));
        return TenantResponseDTO.from(tenant);
    }

    public Tenant getEntityById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id " + id));
    }

    public TenantResponseDTO saveEntity(Tenant tenant) {
        return TenantResponseDTO.from(tenantRepository.save(tenant));
    }

    public TenantResponseDTO create(TenantCreateRequest request) {
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setType(request.getType());
        tenant.setContact(request.getContact());
        tenant.setPhone(request.getPhone());
        tenant.setEmail(request.getEmail());
        tenant.setTinNumber(request.getTinNumber());
        tenant.setBusinessLicense(request.getBusinessLicense());

        return TenantResponseDTO.from(tenantRepository.save(tenant));
    }

    public TenantResponseDTO update(Long id, TenantUpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id " + id));

        tenant.setName(request.getName());
        tenant.setType(request.getType());
        tenant.setContact(request.getContact());
        tenant.setPhone(request.getPhone());
        tenant.setEmail(request.getEmail());
        tenant.setTinNumber(request.getTinNumber());
        tenant.setBusinessLicense(request.getBusinessLicense());

        return TenantResponseDTO.from(tenantRepository.save(tenant));
    }

    public void delete(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id " + id));

        List<com.rms.entity.Lease> activeLeases = leaseRepository.findByTenantIdAndStatus(id, com.rms.entity.LeaseStatus.ACTIVE);
        if (!activeLeases.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete tenant with active leases");
        }

        tenantRepository.delete(tenant);
    }
}
