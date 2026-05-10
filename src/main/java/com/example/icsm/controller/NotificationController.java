package com.example.icsm.controller;

import com.example.icsm.model.User;
import com.example.icsm.repository.UserRepository;
import com.example.icsm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public String listNotifications(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();

        model.addAttribute("notifications", notificationService.getNotificationsForUser(user.getId()));
        return "notification/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }

    @PostMapping("/{id}/delete")
    public String deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return "redirect:/notifications";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String replyMessage, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User me = userRepository.findByEmail(principal.getName()).orElseThrow();

        com.example.icsm.model.Notification original = notificationService.getNotificationsForUser(me.getId())
                .stream().filter(n -> n.getId().equals(id)).findFirst().orElseThrow();

        if (original.getSender() != null) {
            notificationService.createNotification(original.getSender(), me,
                    "Reply to: " + original.getTitle(),
                    replyMessage,
                    com.example.icsm.model.enums.NotificationType.system);
        }

        notificationService.markAsRead(id);
        return "redirect:/notifications?replied";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        notificationService.markAllAsRead(user.getId());
        return "redirect:/notifications";
    }
}
