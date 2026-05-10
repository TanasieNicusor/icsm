package com.example.icsm.controller;

import com.example.icsm.model.User;
import com.example.icsm.model.enums.NotificationType;
import com.example.icsm.model.enums.UserRole;
import com.example.icsm.repository.UserRepository;
import com.example.icsm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SupportController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/support")
    public String support() {
        return "support/index";
    }

    @PostMapping("/support/message")
    public String sendMessage(@RequestParam String subject, 
                             @RequestParam String message, 
                             Principal principal, 
                             RedirectAttributes redirectAttributes) {
        
        User sender = (principal != null) ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
        String senderName = (sender != null) ? sender.getFullName() : "Anonymous";
        
        try {
            // Find Admins using the new dedicated repository method
            List<User> admins = userRepository.findByRole(UserRole.Admin);

            if (!admins.isEmpty()) {
                for (User admin : admins) {
                    notificationService.createNotification(admin, sender, 
                        "Support: " + subject, 
                        "From: " + senderName + "\n\n" + message, 
                        NotificationType.system);
                }
                redirectAttributes.addFlashAttribute("success", "Message sent successfully!");
            } else {
                // If no admin exists, we just log it and show success to user (or a warning)
                redirectAttributes.addFlashAttribute("success", "Message received (no admin available).");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/support";
    }
}
