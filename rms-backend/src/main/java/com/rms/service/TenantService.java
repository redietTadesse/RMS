package com.rms.service;

import com.rms.dto.*;
import com.rms.entity.Role;
import com.rms.entity.Tenant;
import com.rms.entity.User;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.TenantRepository;
import com.rms.repository.LeaseRepository;
import com.rms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;

    public TenantService(TenantRepository tenantRepository,
                         LeaseRepository leaseRepository,
                         UserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.leaseRepository = leaseRepository;
        this.userRepository = userRepository;
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

    @Transactional
    public TenantResponseDTO create(TenantCreateRequest request) {
        Long userId = request.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found with id " + userId));

        if (user.getRole() != Role.TENANT) {
            throw new IllegalArgumentException("Selected account must have the TENANT role");
        }

        boolean alreadyLinked = tenantRepository.findAll().stream()
                .anyMatch(t -> userId.equals(t.getUserId()));
        if (alreadyLinked) {
            throw new IllegalArgumentException("This user account is already linked to another tenant");
        }

        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            email = user.getEmail();
        }

        Tenant tenant = new Tenant();
        tenant.setUserId(userId);
        tenant.setName(request.getName());
        tenant.setType(request.getType());
        tenant.setContact(request.getContact());
        tenant.setPhone(request.getPhone());
        tenant.setEmail(email);
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