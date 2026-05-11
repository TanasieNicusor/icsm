package com.example.icsm.service;

import com.example.icsm.model.Notification;
import com.example.icsm.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId).size();
    }

    public void createNotification(com.example.icsm.model.User user, com.example.icsm.model.User sender, String title, String message, com.example.icsm.model.enums.NotificationType type) {
        createNotification(user, sender, title, message, type, null);
    }

    public void createNotification(com.example.icsm.model.User user, com.example.icsm.model.User sender, String title, String message, com.example.icsm.model.enums.NotificationType type, Long relatedId) {
        try {
            Notification notification = Notification.builder()
                    .user(user)
                    .sender(sender)
                    .title(title)
                    .message(message)
                    .type(type)
                    .relatedId(relatedId)
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            // Log error but don't crash the whole policy purchase
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
