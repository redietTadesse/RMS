package com.rms.service;

import com.rms.entity.Notification;
import com.rms.entity.User;
import com.rms.repository.NotificationRepository;
import com.rms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<Notification> getForUsername(String username) {
        User user = getUser(username);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public long getUnreadCount(String username) {
        User user = getUser(username);
        return notificationRepository.countByUserIdAndReadFlagFalse(user.getId());
    }

    @Transactional
    public Notification createForUser(Long userId, String type, String title, String message, String link) {
        if (userId == null) {
            return null;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification createForUserOnce(Long userId, String type, Long referenceId,
                                          String title, String message, String link) {
        if (userId == null) {
            return null;
        }

        // Simple check without auto-flush issues
        if (referenceId != null) {
            try {
                boolean exists = notificationRepository.existsByUserIdAndTypeAndReferenceId(userId, type, referenceId);
                if (exists) {
                    return null;
                }
            } catch (Exception e) {
                // If exists check fails, continue with creation (optimistic approach)
                // This avoids transaction issues while still preventing most duplicates
            }
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markRead(String username, Long notificationId) {
        User user = getUser(username);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification does not belong to this user");
        }

        notification.setReadFlag(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(String username) {
        User user = getUser(username);
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        for (Notification notification : notifications) {
            if (!notification.isReadFlag()) {
                notification.setReadFlag(true);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}