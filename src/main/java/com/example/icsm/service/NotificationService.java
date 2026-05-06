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

    public List<Notification> getRecentNotifications(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId).size();
    }
}
