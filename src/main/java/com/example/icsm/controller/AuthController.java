package com.example.icsm.controller;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.icsm.model.User;
import com.example.icsm.model.enums.UserRole;
import com.example.icsm.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
//a
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", UserRole.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            return "auth/register";
        }

        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email already exists");
            model.addAttribute("roles", UserRole.values());
            return "auth/register";
        }
        userService.saveUser(user);
        return "redirect:/login?success";
    }
}
