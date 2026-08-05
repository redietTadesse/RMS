package com.rms.service;

import com.rms.dto.DashboardStats;
import com.rms.entity.InvoiceStatus;
import com.rms.entity.LeaseStatus;
import com.rms.entity.SpaceStatus;
import com.rms.repository.InvoiceRepository;
import com.rms.repository.LeaseRepository;
import com.rms.repository.SpaceRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final SpaceRepository spaceRepository;
    private final LeaseRepository leaseRepository;
    private final InvoiceRepository invoiceRepository;

    public DashboardService(SpaceRepository spaceRepository,
                            LeaseRepository leaseRepository,
                            InvoiceRepository invoiceRepository) {
        this.spaceRepository = spaceRepository;
        this.leaseRepository = leaseRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public DashboardStats getStats() {
        return new DashboardStats(
                spaceRepository.count(),
                spaceRepository.countByStatus(SpaceStatus.VACANT),
                spaceRepository.countByStatus(SpaceStatus.OCCUPIED),
                leaseRepository.countByStatus(LeaseStatus.ACTIVE),
                invoiceRepository.countByStatus(InvoiceStatus.PENDING),
                invoiceRepository.countByStatus(InvoiceStatus.OVERDUE),
                invoiceRepository.sumPenalties()
        );
    }
}
