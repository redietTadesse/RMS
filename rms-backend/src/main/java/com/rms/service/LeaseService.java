package com.rms.service;

import com.rms.dto.LeaseRequest;
import com.rms.entity.*;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.LeaseRepository;
import com.rms.repository.SpaceRepository;
import com.rms.repository.TenantRepository;
import com.rms.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final SpaceRepository spaceRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public LeaseService(LeaseRepository leaseRepository,
                        SpaceRepository spaceRepository,
                        TenantRepository tenantRepository,
                        UserRepository userRepository,
                        NotificationService notificationService) {
        this.leaseRepository = leaseRepository;
        this.spaceRepository = spaceRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public List<Lease> getAll() {
        syncExpiredLeases();
        return leaseRepository.findAll();
    }

    @Transactional
    public Lease getById(Long id) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));
        return expireLeaseIfNeeded(lease) ? leaseRepository.save(lease) : lease;
    }

    @Transactional
    public List<Lease> getActiveLeases() {
        syncExpiredLeases();
        return leaseRepository.findByStatus(LeaseStatus.ACTIVE);
    }

    @Transactional
    public List<Lease> getByTenant(Long tenantId) {
        syncExpiredLeases();
        return leaseRepository.findByTenantId(tenantId);
    }

    // ✅ CORRECTED METHOD – fixes the .or() error
    @Transactional
    public Lease getLeaseByTenantUsername(String username) {
        syncExpiredLeases();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tenant found for user: " + username));

        // Convert List<Lease> to Optional<Lease> using stream().findFirst()
        return leaseRepository.findByTenantIdAndStatus(tenant.getId(), LeaseStatus.ACTIVE)
                .stream().findFirst()
                .or(() -> leaseRepository.findFirstByTenantIdOrderByEndDateDesc(tenant.getId()))
                .orElse(null);
    }

    @Transactional
    public Lease createLease(LeaseRequest request) {
        syncExpiredLeases();

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

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void autoMarkExpiredLeases() {
        int expiredCount = syncExpiredLeases();
        if (expiredCount > 0) {
            System.out.println(expiredCount + " leases marked as EXPIRED.");
        }
    }

    @Transactional
    public int syncExpiredLeases() {
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);
        int expiredCount = 0;

        for (Lease lease : activeLeases) {
            if (expireLeaseIfNeeded(lease)) {
                leaseRepository.save(lease);
                expiredCount++;
                System.out.println("Lease #" + lease.getId() + " expired. Space "
                        + lease.getSpace().getUnitNumber() + " is now VACANT.");
            }
        }

        return expiredCount;
    }

    public boolean isLeaseExpired(Long leaseId) {
        Lease lease = getById(leaseId);
        return lease.getEndDate().isBefore(LocalDate.now());
    }

    @Transactional
    public Lease forceExpire(Long leaseId) {
        Lease lease = getById(leaseId);

        if (lease.getStatus() == LeaseStatus.EXPIRED) {
            throw new IllegalArgumentException("Lease is already expired");
        }

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active leases can be expired");
        }

        lease.setStatus(LeaseStatus.EXPIRED);
        leaseRepository.save(lease);

        Space space = lease.getSpace();
        space.setStatus(SpaceStatus.VACANT);
        spaceRepository.save(space);
        notifyLeaseExpired(lease);

        return lease;
    }

    private void notifyLeaseExpired(Lease lease) {
        Tenant tenant = lease.getTenant();
        notificationService.createForUserOnce(
                tenant.getUserId(),
                "LEASE_EXPIRED",
                lease.getId(),
                "Lease expired",
                "Your lease for space " + lease.getSpace().getUnitNumber()
                        + " expired on " + lease.getEndDate() + ". Please contact admin to renew.",
                "/tenant-dashboard"
        );
    }

    private boolean expireLeaseIfNeeded(Lease lease) {
        if (lease.getStatus() != LeaseStatus.ACTIVE
                || !lease.getEndDate().isBefore(LocalDate.now())) {
            return false;
        }

        lease.setStatus(LeaseStatus.EXPIRED);

        Space space = lease.getSpace();
        space.setStatus(SpaceStatus.VACANT);
        spaceRepository.save(space);

        notifyLeaseExpired(lease);
        return true;
    }
}