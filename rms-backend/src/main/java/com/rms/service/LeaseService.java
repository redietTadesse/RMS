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
import java.time.temporal.ChronoUnit;
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

    @Transactional
    public Lease getLeaseByTenantUsername(String username) {
        syncExpiredLeases();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tenant found for user: " + username));

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
        lease.setCreatedAt(LocalDate.now());
        
        // Initialize nextBillingDate based on prepayment period
        LocalDate start = request.getStartDate();
        LocalDate prepaymentEnd = start.plusMonths(request.getPrepaymentMonths());
        lease.setNextBillingDate(prepaymentEnd);
        
        // Set billing frequency from request
        lease.setBillingFrequency(request.getBillingFrequency());
        
        // Check if lease starts after 15th - only then prorate first invoice
        // If start date is 1st-15th, treat as full month payment
        if (start.getDayOfMonth() > 15) {
            lease.setFirstInvoiceProrated(true);
        } else {
            lease.setFirstInvoiceProrated(false);
        }

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

        // Store original values for comparison
        LocalDate originalStartDate = lease.getStartDate();
        Integer originalPrepaymentMonths = lease.getPrepaymentMonths();
        LocalDate originalNextBillingDate = lease.getNextBillingDate();

        lease.setStartDate(request.getStartDate());
        lease.setEndDate(request.getEndDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setDeposit(request.getDeposit());
        lease.setPrepaymentMonths(request.getPrepaymentMonths());
        lease.setDueDay(request.getDueDay());
        lease.setUpdatedAt(LocalDate.now());
        
        // Update billing frequency if changed
        if (request.getBillingFrequency() != null) {
            lease.setBillingFrequency(request.getBillingFrequency());
        }
        
        // Recalculate nextBillingDate based on current state, not just start date
        if (!originalPrepaymentMonths.equals(request.getPrepaymentMonths()) || 
            !originalStartDate.equals(request.getStartDate())) {
            
            LocalDate today = LocalDate.now();
            LocalDate newStart = request.getStartDate();
            LocalDate newPrepaymentEnd = newStart.plusMonths(request.getPrepaymentMonths());
            
            // If we're still in the original prepayment period, adjust from today
            if (originalNextBillingDate != null && today.isBefore(originalNextBillingDate)) {
                // We're in prepayment, so calculate new prepayment end from today
                LocalDate monthsRemaining = newPrepaymentEnd.minusMonths(
                    ChronoUnit.MONTHS.between(newStart, today)
                );
                lease.setNextBillingDate(monthsRemaining.isAfter(today) ? monthsRemaining : newPrepaymentEnd);
            } else if (originalNextBillingDate != null && !today.isBefore(originalNextBillingDate)) {
                // We're past original prepayment, keep current billing cycle but adjust for new prepayment
                lease.setNextBillingDate(newPrepaymentEnd.isAfter(today) ? newPrepaymentEnd : originalNextBillingDate);
            } else {
                // No original billing date, initialize from new prepayment
                lease.setNextBillingDate(newPrepaymentEnd);
            }
        }

        return leaseRepository.save(lease);
    }

    @Transactional
    public Lease terminateLease(Long id) {
        Lease lease = getById(id);

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active");
        }

        // Validate status transition
        if (!isValidLeaseStatusTransition(lease.getStatus(), LeaseStatus.TERMINATED)) {
            throw new IllegalArgumentException("Invalid status transition from " + 
                lease.getStatus() + " to " + LeaseStatus.TERMINATED);
        }

        lease.setStatus(LeaseStatus.TERMINATED);
        lease.setUpdatedAt(LocalDate.now());
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
        lease.setUpdatedAt(LocalDate.now());
        leaseRepository.save(lease);

        Space space = lease.getSpace();
        space.setStatus(SpaceStatus.VACANT);
        spaceRepository.save(space);
        notifyLeaseExpired(lease);

        return lease;
    }


    public LocalDate getNextDueDate(Lease lease) {
        if (lease == null || lease.getStatus() != LeaseStatus.ACTIVE) {
            return null;
        }

        // Prepayment covers `prepaymentMonths` months from start date
        LocalDate prepaymentEnd = lease.getStartDate().plusMonths(lease.getPrepaymentMonths());
        LocalDate today = LocalDate.now();
        int dueDay = lease.getDueDay() != null ? lease.getDueDay() : 1;

        // First billing starts after prepayment ends
        LocalDate firstBilling = prepaymentEnd;
        LocalDate dueDate = firstBilling.withDayOfMonth(Math.min(dueDay, firstBilling.lengthOfMonth()));

        // If due date is before today, move to next month
        while (dueDate.isBefore(today)) {
            dueDate = dueDate.plusMonths(1);
            dueDate = dueDate.withDayOfMonth(Math.min(dueDay, dueDate.lengthOfMonth()));
        }

        return dueDate;
    }

    public long getDaysUntilDue(Lease lease) {
        LocalDate nextDue = getNextDueDate(lease);
        if (nextDue == null) return -1;
        return ChronoUnit.DAYS.between(LocalDate.now(), nextDue);
    }

    public boolean isPrepaymentActive(Lease lease) {
        if (lease == null) return false;
        LocalDate today = LocalDate.now();
        LocalDate prepaymentEnd = lease.getStartDate().plusMonths(lease.getPrepaymentMonths());
        return today.isBefore(prepaymentEnd) || today.isEqual(prepaymentEnd);
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

        // Validate status transition
        if (!isValidLeaseStatusTransition(lease.getStatus(), LeaseStatus.EXPIRED)) {
            System.err.println("Invalid status transition from " + lease.getStatus() + " to EXPIRED");
            return false;
        }

        lease.setStatus(LeaseStatus.EXPIRED);
        lease.setUpdatedAt(LocalDate.now());

        Space space = lease.getSpace();
        space.setStatus(SpaceStatus.VACANT);
        spaceRepository.save(space);

        notifyLeaseExpired(lease);
        return true;
    }

    private boolean isValidLeaseStatusTransition(LeaseStatus from, LeaseStatus to) {
        // Valid transitions:
        // ACTIVE -> TERMINATED (manual termination)
        // ACTIVE -> EXPIRED (automatic expiration)
        // No other transitions are valid
        return (from == LeaseStatus.ACTIVE && (to == LeaseStatus.TERMINATED || to == LeaseStatus.EXPIRED));
    }
}