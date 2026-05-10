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
    private final com.example.icsm.repository.UserRepository userRepository;

    @ModelAttribute
    public void addGlobalAttributes(org.springframework.ui.Model model, java.security.Principal principal) {
        if (principal != null) {
            userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                model.addAttribute("unreadCount", notificationService.getUnreadCount(user.getId()));
            });
        } else {
            model.addAttribute("unreadCount", 0);
        }
    }
}
