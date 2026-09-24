package com.rms.service;

import com.rms.dto.*;
import com.rms.entity.*;
import com.rms.exception.ResourceNotFoundException;
import com.rms.repository.LeaseRepository;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.NotificationRepository;
import com.rms.repository.PaymentScheduleRepository;
import com.rms.repository.SpaceRepository;
import com.rms.repository.TenantRepository;
import com.rms.repository.UserRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaseService {

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

    @Transactional
    public List<LeaseResponseDTO> getAll() {
        syncExpiredLeases();
        return leaseRepository.findAll().stream()
                .map(LeaseResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeaseResponseDTO getById(Long id) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));
        expireLeaseIfNeeded(lease);
        return LeaseResponseDTO.from(lease);
    }

    @Transactional
    public List<LeaseResponseDTO> getActiveLeases() {
        return leaseRepository.findByStatus(LeaseStatus.ACTIVE).stream()
                .map(LeaseResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<LeaseResponseDTO> getByTenant(Long tenantId) {
        syncExpiredLeases();
        return leaseRepository.findByTenantId(tenantId).stream()
                .map(LeaseResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeaseResponseDTO getLeaseByTenantUsername(String username) {
        Lease lease = getLeaseEntityByTenantUsername(username);
        return LeaseResponseDTO.from(lease);
    }

    public Lease getLeaseEntityByTenantUsername(String username) {
        syncExpiredLeases();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Tenant tenant = tenantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No tenant found for user: " + username));

        Lease lease = leaseRepository.findByTenantIdAndStatus(tenant.getId(), LeaseStatus.ACTIVE)
                .stream().findFirst()
                .or(() -> leaseRepository.findFirstByTenantIdOrderByEndDateDesc(tenant.getId()))
                .orElse(null);

        if (lease != null) {
            expireLeaseIfNeeded(lease);
        }
        return lease;
    }

    @Transactional
    public LeaseResponseDTO createLease(LeaseRequest request) {
        validateLeaseRequest(request);
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found with id " + request.getTenantId()));
        Space space = spaceRepository.findById(request.getSpaceId())
                .orElseThrow(() -> new IllegalArgumentException("Space not found with id " + request.getSpaceId()));

        if (space.getStatus() != SpaceStatus.VACANT) {
            throw new IllegalArgumentException("Space " + space.getUnitNumber() + " is not vacant");
        }
        if (leaseRepository.existsBySpaceAndStatus(space, LeaseStatus.ACTIVE)) {
            throw new IllegalArgumentException("Space " + space.getUnitNumber() + " already has an active lease");
        }

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
        space.setStatus(SpaceStatus.OCCUPIED);
        spaceRepository.save(space);

        return LeaseResponseDTO.from(saved);
    }

    @Transactional
    public LeaseResponseDTO updateLease(Long id, LeaseRequest request) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active leases can be edited");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        lease.setStartDate(request.getStartDate());
        lease.setEndDate(request.getEndDate());
        lease.setMonthlyRent(request.getMonthlyRent());
        lease.setDeposit(request.getDeposit() != null ? request.getDeposit() : java.math.BigDecimal.ZERO);
        lease.setInitialPaymentMonths(request.getInitialPaymentMonths());
        lease.setDueDay(request.getDueDay());
        if (request.getBillingFrequency() != null) {
            lease.setBillingFrequency(request.getBillingFrequency());
        }
        lease.setUpdatedAt(LocalDate.now());

        return LeaseResponseDTO.from(leaseRepository.save(lease));
    }

    @Transactional
    public LeaseResponseDTO terminateLease(Long id) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalArgumentException("Lease is not active");
        }
        lease.setStatus(LeaseStatus.TERMINATED);
        lease.setUpdatedAt(LocalDate.now());
        leaseRepository.save(lease);
        Space space = lease.getSpace();
        if (space != null) {
            space.setStatus(SpaceStatus.VACANT);
            spaceRepository.save(space);
        }
        return LeaseResponseDTO.from(lease);
    }

    @Transactional
    public void deleteLease(Long id) {
        Lease lease = leaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + id));
        List<com.rms.entity.PaymentSchedule> schedules = paymentScheduleRepository.findByLeaseOrderByPaymentNumberAsc(lease);
        if (!schedules.isEmpty()) {
            paymentScheduleRepository.deleteAll(schedules);
        }
        List<com.rms.entity.Invoice> invoices = invoiceRepository.findByLease(lease);
        if (!invoices.isEmpty()) {
            invoiceRepository.deleteAll(invoices);
        }
        notificationRepository.deleteByReferenceId(lease.getId());
        if (lease.getStatus() == LeaseStatus.ACTIVE) {
            Space space = lease.getSpace();
            if (space != null) {
                space.setStatus(SpaceStatus.VACANT);
                spaceRepository.save(space);
            }
        }
        leaseRepository.delete(lease);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void autoMarkExpiredLeases() {
        syncExpiredLeases();
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
    public LeaseResponseDTO forceExpire(Long leaseId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found with id " + leaseId));
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
        return LeaseResponseDTO.from(lease);
    }

    public LocalDate getNextDueDate(Lease lease) {
        if (lease == null || lease.getStatus() != LeaseStatus.ACTIVE) return null;
        LocalDate today = timeTravelService.getCurrentDate();
        int dueDay = lease.getDueDay() != null ? lease.getDueDay() : 1;
        if (lease.getNextPaymentDueDate() != null) return lease.getNextPaymentDueDate();
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
        return ChronoUnit.DAYS.between(timeTravelService.getCurrentDate(), nextDue);
    }

    public boolean isPrepaymentActive(Lease lease) {
        if (lease == null) return false;
        return lease.isPaymentScheduleActive() && lease.getNextPaymentDueDate() != null &&
             timeTravelService.getCurrentDate().isBefore(lease.getNextPaymentDueDate());
    }

    public boolean hasActiveLease(Long tenantId) {
        return !leaseRepository.findByTenantIdAndStatus(tenantId, LeaseStatus.ACTIVE).isEmpty();
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
        if (lease.getStatus() != LeaseStatus.ACTIVE || !lease.getEndDate().isBefore(timeTravelService.getCurrentDate())) {
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

    public void validateLeaseRequest(LeaseRequest request) {
        if (request.getTenantId() == null || request.getTenantId() <= 0) {
            throw new IllegalArgumentException("Valid Tenant ID is required");
        }
        if (request.getSpaceId() == null || request.getSpaceId() <= 0) {
            throw new IllegalArgumentException("Valid Space ID is required");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Dates are required");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (request.getMonthlyRent() == null || request.getMonthlyRent().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rent must be positive");
        }
        if (request.getInitialPaymentMonths() == null || request.getInitialPaymentMonths() < 1) {
            throw new IllegalArgumentException("Initial payment must be at least 1 month");
        }
        if (request.getDueDay() == null || request.getDueDay() < 1 || request.getDueDay() > 31) {
            throw new IllegalArgumentException("Due day must be between 1 and 31");
        }
    }
}
