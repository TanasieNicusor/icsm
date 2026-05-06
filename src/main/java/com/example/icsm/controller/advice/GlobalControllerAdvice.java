package com.example.icsm.controller.advice;

import com.example.icsm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final NotificationService notificationService;

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        // Hardcoded userId for now since we don't have security yet
        Long userId = 1L;
        model.addAttribute("notifications", notificationService.getRecentNotifications(userId));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(userId));
    }
}
