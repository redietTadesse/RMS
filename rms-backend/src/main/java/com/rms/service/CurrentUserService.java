package com.rms.service;

import com.rms.entity.Invoice;
import com.rms.entity.Tenant;
import com.rms.entity.TenantDocument;
import com.rms.entity.User;
import com.rms.repository.TenantRepository;
import com.rms.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    public CurrentUserService(UserRepository userRepository, TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
    }

    /** The logged-in User, resolved from the JWT. */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /** The Tenant record linked to the logged-in User. */
    public Tenant getCurrentTenant() {
        User user = getCurrentUser();
        return tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No tenant record linked to this account"));
    }

    /** Throws 403 unless the invoice belongs to the logged-in tenant. */
    public Tenant requireOwnership(Invoice invoice) {
        Tenant tenant = getCurrentTenant();
        Long ownerId = invoice.getLease() == null || invoice.getLease().getTenant() == null
                ? null : invoice.getLease().getTenant().getId();
        if (ownerId == null || !ownerId.equals(tenant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This invoice is not yours");
        }
        return tenant;
    }

    /** True when the authenticated principal carries the ADMIN role. */
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Authorizes invoice access for the current caller: admins may access any
     * invoice; tenants may only access invoices belonging to their own tenant.
     */
    public void requireInvoiceAccess(Invoice invoice) {
        if (isAdmin()) return;
        requireOwnership(invoice);   // existing tenant-scoped check
    }

    /**
     * Authorizes document access. The document must be loaded with its tenant
     * initialized (JOIN FETCH) so this check does not rely on lazy loading.
     */
    public void requireDocumentAccess(TenantDocument document) {
        if (isAdmin()) return;

        Tenant current = getCurrentTenant();
        Long ownerId = (document != null && document.getTenant() != null)
                ? document.getTenant().getId() : null;

        if (ownerId == null || !ownerId.equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This document is not yours");
        }
    }
}
