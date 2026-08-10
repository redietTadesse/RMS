package com.rms.repository;

import com.rms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);

    boolean existsByUserIdAndTypeAndReferenceId(Long userId, String type, Long referenceId);
}
