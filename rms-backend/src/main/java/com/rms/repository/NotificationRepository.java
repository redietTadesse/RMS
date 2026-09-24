package com.rms.repository;

import com.rms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);

    boolean existsByUserIdAndTypeAndReferenceId(Long userId, String type, Long referenceId);

    void deleteByReferenceId(Long referenceId);
}
