package com.example.icsm.controller;

import com.example.icsm.model.User;
import com.example.icsm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String viewProfile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("user", user);
        return "auth/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("user") User user, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        // We skip @Valid here because we don't want to trigger password validation for profile updates
        // Manual validation could be added for phone/name if needed
        userService.updateUserProfile(userDetails.getUsername(), user);
        return "redirect:/profile?success";
    }
}
