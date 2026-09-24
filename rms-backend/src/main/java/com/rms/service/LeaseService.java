package com.rms.service;

import com.rms.dto.LeaseRequest;
import com.rms.entity.*;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.LeaseRepository;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.NotificationRepository;
import com.rms.repository.PaymentScheduleRepository;
import com.rms.repository.SpaceRepository;
import com.rms.repository.TenantRepository;
import com.rms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaseService {

    private static final Logger log = LoggerFactory.getLogger(LeaseService.class);

    private final LeaseRepository leaseRepository;
    private final SpaceRepository spaceRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TimeTravelService timeTravelService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final NotificationRepository notificationRepository;

    public LeaseService(LeaseRepository leaseRepository,
                        SpaceRepository spaceRepository,
                        TenantRepository tenantRepository,
                        UserRepository userRepository,
                        NotificationService notificationService,
                        TimeTravelService timeTravelService,
                        InvoiceRepository invoiceRepository,
                        PaymentScheduleRepository paymentScheduleRepository,
                        NotificationRepository notificationRepository) {
        this.leaseRepository = leaseRepository;
        this.spaceRepository = spaceRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.timeTravelService = timeTravelService;
        this.invoiceRepository = invoiceRepository;
        this.paymentScheduleRepository = paymentScheduleRepository;
        this.notificationRepository = notificationRepository;
    }

    // ─── GET METHODS ───

    @Transactional
    public List<Lease> getAll() {
        syncExpiredLeases();
        return leaseRepository.findAll();
    }

    @Transactional
    public Lease getById(Long id) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));
        expireLeaseIfNeeded(lease);
        return lease;
    }

    @Transactional
    public List<Lease> getActiveLeases() {
        // Don't auto-sync expired leases to avoid transaction issues
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

        // Try to find active lease first, then the most recent lease
        return leaseRepository.findByTenantIdAndStatus(tenant.getId(), LeaseStatus.ACTIVE)
                .stream().findFirst()
                .or(() -> leaseRepository.findFirstByTenantIdOrderByEndDateDesc(tenant.getId()))
                .orElse(null);
    }

    // ─── CREATE LEASE ───

    @Transactional
    public Lease createLease(LeaseRequest request) {
        syncExpiredLeases();

        // Validate Tenant
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id " + request.getTenantId()));

        // Validate Space
        Space space = spaceRepository.findById(request.getSpaceId())
                .orElseThrow(() -> new IllegalArgumentException("Space not found with id " + request.getSpaceId()));

        if (space.getStatus() != SpaceStatus.VACANT) {
            throw new IllegalArgumentException("Space " + space.getUnitNumber() + " is not vacant");
        }

        if (leaseRepository.existsBySpaceAndStatus(space, LeaseStatus.ACTIVE)) {
            throw new IllegalArgumentException("Space " + space.getUnitNumber() + " already has an active lease");
        }

        // Validate Initial Payment
        if (request.getInitialPaymentMonths() == null || request.getInitialPaymentMonths() < 1) {
            throw new IllegalArgumentException("Initial payment must be at least 1 month");
        }

        if (request.getInitialPaymentMonths() > 24) {
            throw new IllegalArgumentException("Initial payment cannot exceed 24 months");
        }

        // Validate Dates
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        // Uncomment for production
        // if (request.getStartDate().isBefore(LocalDate.now())) {
        //     throw new IllegalArgumentException("Start date cannot be in the past");
        // }

        // Validate Monthly Rent
        if (request.getMonthlyRent() == null || request.getMonthlyRent().compareTo(new java.math.BigDecimal("1.00")) < 0) {
            throw new IllegalArgumentException("Monthly rent must be at least 1.00");
        }

        // Validate Due Day
        if (request.getDueDay() == null || request.getDueDay() < 1 || request.getDueDay() > 31) {
            throw new IllegalArgumentException("Due day must be between 1 and 31");
        }

        // Build Lease
        Lease lease = new Lease();
        lease.setTenant(tenant);
        lease.setSpace(space);
        lease.setStartDate(request.getStartDate());
        lease.setEndDate(request.getEndDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setDeposit(request.getDeposit() != null ? request.getDeposit() : java.math.BigDecimal.ZERO);
        lease.setInitialPaymentMonths(request.getInitialPaymentMonths());
        lease.setDueDay(request.getDueDay());
        lease.setStatus(LeaseStatus.ACTIVE);
        lease.setBillingFrequency(request.getBillingFrequency() != null ? 
                request.getBillingFrequency() : BillingFrequency.MONTHLY);
        lease.setNextBillingDate(request.getStartDate().plusMonths(request.getInitialPaymentMonths()));
        lease.setFirstInvoiceProrated(request.getStartDate().getDayOfMonth() > 15);

        Lease saved = leaseRepository.save(lease);

        // Update Space Status
        space.setStatus(SpaceStatus.OCCUPIED);
        spaceRepository.save(space);

        return saved;
    }

    // ─── UPDATE LEASE ───

    @Transactional
    public Lease updateLease(Long id, LeaseRequest request) {
        Lease lease = getById(id);

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active leases can be edited");
        }

        // Validate Initial Payment
        if (request.getInitialPaymentMonths() == null || request.getInitialPaymentMonths() < 1) {
            throw new IllegalArgumentException("Initial payment must be at least 1 month");
        }

        if (request.getInitialPaymentMonths() > 24) {
            throw new IllegalArgumentException("Initial payment cannot exceed 24 months");
        }

        // Validate Dates
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        LocalDate today = LocalDate.now();
        if (request.getStartDate().isBefore(today) && !request.getStartDate().equals(lease.getStartDate())) {
            throw new IllegalArgumentException("Cannot change start date to a past date");
        }

        // Validate Monthly Rent
        if (request.getMonthlyRent() == null || request.getMonthlyRent().compareTo(new java.math.BigDecimal("1.00")) < 0) {
            throw new IllegalArgumentException("Monthly rent must be at least 1.00");
        }

        // Validate Due Day
        if (request.getDueDay() == null || request.getDueDay() < 1 || request.getDueDay() > 31) {
            throw new IllegalArgumentException("Due day must be between 1 and 31");
        }

        try {
            lease.setStartDate(request.getStartDate());
            lease.setEndDate(request.getEndDate());
            lease.setMonthlyRent(request.getMonthlyRent());
            lease.setDeposit(request.getDeposit() != null ? request.getDeposit() : java.math.BigDecimal.ZERO);
            lease.setInitialPaymentMonths(request.getInitialPaymentMonths());
            lease.setDueDay(request.getDueDay());
            lease.setUpdatedAt(today);

            if (request.getBillingFrequency() != null) {
                lease.setBillingFrequency(request.getBillingFrequency());
            }

            // Update next billing date if prepayment period changes
            LocalDate newPrepaymentEnd = request.getStartDate().plusMonths(request.getInitialPaymentMonths());
            if (lease.getNextBillingDate() != null && today.isBefore(lease.getNextBillingDate())) {
                lease.setNextBillingDate(newPrepaymentEnd);
            }

            return leaseRepository.save(lease);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalArgumentException("Lease was updated by another user. Please refresh and try again.");
        }
    }

    // ─── TERMINATE LEASE ───

    @Transactional
    public Lease terminateLease(Long id) {
        // Fetch lease directly WITHOUT calling getById(), because getById() has a side effect
        // (expireLeaseIfNeeded) that auto-expires ACTIVE leases whose endDate has passed.
        // That would make a lease un-terminable after its natural end date.
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));

        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active (current status: " + lease.getStatus() + ")");
        }

        if (!isValidLeaseStatusTransition(lease.getStatus(), LeaseStatus.TERMINATED)) {
            throw new IllegalArgumentException("Invalid status transition from " + lease.getStatus() + 
                    " to " + LeaseStatus.TERMINATED);
        }

        try {
            lease.setStatus(LeaseStatus.TERMINATED);
            lease.setUpdatedAt(LocalDate.now());
            leaseRepository.save(lease);

            Space space = lease.getSpace();
            if (space != null) {
                space.setStatus(SpaceStatus.VACANT);
                spaceRepository.save(space);
            }

            return lease;
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalArgumentException("Lease was updated by another user. Please refresh and try again.");
        }
    }

    // ─── DELETE LEASE ───

    @Transactional
    public void deleteLease(Long id) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));

        // Delete dependent records first to satisfy FK constraints.
        List<com.rms.entity.PaymentSchedule> schedules =
                paymentScheduleRepository.findByLeaseOrderByPaymentNumberAsc(lease);
        if (!schedules.isEmpty()) {
            paymentScheduleRepository.deleteAll(schedules);
        }

        List<com.rms.entity.Invoice> invoices = invoiceRepository.findByLease(lease);
        if (!invoices.isEmpty()) {
            invoiceRepository.deleteAll(invoices);
        }

        // Clean up notifications that reference this lease directly.
        notificationRepository.deleteByReferenceId(lease.getId());

        // Only free the space when deleting an ACTIVE lease; if the lease is
        // already TERMINATED/EXPIRED the space may have been re-occupied.
        if (lease.getStatus() == LeaseStatus.ACTIVE) {
            Space space = lease.getSpace();
            if (space != null) {
                space.setStatus(SpaceStatus.VACANT);
                spaceRepository.save(space);
            }
        }

        leaseRepository.delete(lease);
        log.info("Lease {} deleted", id);
    }

    // ─── EXPIRATION MANAGEMENT ───

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void autoMarkExpiredLeases() {
        int expiredCount = syncExpiredLeases();
        if (expiredCount > 0) {
            log.info("{} leases marked as EXPIRED", expiredCount);
        }
    }

    @Transactional
    public int syncExpiredLeases() {
        List<Lease> activeLeases = leaseRepository.findByStatus(LeaseStatus.ACTIVE);
        int expiredCount = 0;

        for (Lease lease : activeLeases) {
            if (expireLeaseIfNeeded(lease)) {
                expiredCount++;
            }
        }

        return expiredCount;
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

        if (!isValidLeaseStatusTransition(lease.getStatus(), LeaseStatus.EXPIRED)) {
            throw new IllegalArgumentException("Invalid status transition from " + lease.getStatus() + 
                    " to " + LeaseStatus.EXPIRED);
        }

        try {
            lease.setStatus(LeaseStatus.EXPIRED);
            lease.setUpdatedAt(LocalDate.now());
            leaseRepository.save(lease);

            Space space = lease.getSpace();
            space.setStatus(SpaceStatus.VACANT);
            spaceRepository.save(space);

            notifyLeaseExpired(lease);
            return lease;
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalArgumentException("Lease was updated by another user. Please refresh and try again.");
        }
    }

    // ─── UTILITY METHODS ───

    public LocalDate getNextDueDate(Lease lease) {
        if (lease == null || lease.getStatus() != LeaseStatus.ACTIVE) {
            return null;
        }

        LocalDate today = LocalDate.now();
        int dueDay = lease.getDueDay() != null ? lease.getDueDay() : 1;

        if (lease.getNextPaymentDueDate() != null) {
            return lease.getNextPaymentDueDate();
        }

        LocalDate dueDate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));

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
        return lease.isPaymentScheduleActive() && lease.getNextPaymentDueDate() != null &&
               LocalDate.now().isBefore(lease.getNextPaymentDueDate());
    }

    public boolean hasActiveLease(Long tenantId) {
        return !leaseRepository.findByTenantIdAndStatus(tenantId, LeaseStatus.ACTIVE).isEmpty();
    }

    // ─── PRIVATE HELPERS ───

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
        if (lease.getStatus() != LeaseStatus.ACTIVE || !lease.getEndDate().isBefore(timeTravelService.getCurrentDate())) {
            return false;
        }

        if (!isValidLeaseStatusTransition(lease.getStatus(), LeaseStatus.EXPIRED)) {
            return false;
        }

        try {
            lease.setStatus(LeaseStatus.EXPIRED);
            lease.setUpdatedAt(LocalDate.now());
            leaseRepository.save(lease);

            Space space = lease.getSpace();
            space.setStatus(SpaceStatus.VACANT);
            spaceRepository.save(space);

            notifyLeaseExpired(lease);
            return true;
        } catch (OptimisticLockingFailureException e) {
            return false;
        }
    }

    private boolean isValidLeaseStatusTransition(LeaseStatus from, LeaseStatus to) {
        return from == LeaseStatus.ACTIVE && (to == LeaseStatus.TERMINATED || to == LeaseStatus.EXPIRED);
    }

    // ─── VALIDATION HELPERS (for use by controllers) ───

    public void validateLeaseRequest(LeaseRequest request) {
        if (request.getTenantId() == null || request.getTenantId() <= 0) {
            throw new IllegalArgumentException("Valid Tenant ID is required");
        }

        if (request.getSpaceId() == null || request.getSpaceId() <= 0) {
            throw new IllegalArgumentException("Valid Space ID is required");
        }

        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }

        if (request.getEndDate() == null) {
            throw new IllegalArgumentException("End date is required");
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        if (request.getMonthlyRent() == null || 
            request.getMonthlyRent().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly rent must be a positive amount");
        }

        if (request.getInitialPaymentMonths() == null || request.getInitialPaymentMonths() < 1) {
            throw new IllegalArgumentException("Initial payment must be at least 1 month");
        }

        if (request.getInitialPaymentMonths() > 24) {
            throw new IllegalArgumentException("Initial payment cannot exceed 24 months");
        }

        if (request.getDueDay() == null || request.getDueDay() < 1 || request.getDueDay() > 31) {
            throw new IllegalArgumentException("Due day must be between 1 and 31");
        }
    }
}
