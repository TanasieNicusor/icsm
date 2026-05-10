package com.example.icsm.controller;

import com.example.icsm.model.Claim;
import com.example.icsm.service.ClaimService;
import com.example.icsm.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import com.example.icsm.model.User;
import com.example.icsm.repository.UserRepository;

@Controller
@RequestMapping("/claim")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final PolicyService policyService;
    private final UserRepository userRepository;
    private final com.example.icsm.service.NotificationService notificationService;

    @GetMapping
    public String listClaims(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();

        boolean isAgent = user.getRole() != null && user.getRole() == com.example.icsm.model.enums.UserRole.Agent;
        model.addAttribute("isAgent", isAgent);

        if (isAgent) {
            model.addAttribute("claims", claimService.getAllClaims());
            return "claim/review";
        } else {
            model.addAttribute("claims", claimService.getClaimsByCustomer(user.getId()));
            return "claim/list";
        }
    }

    @GetMapping("/file")
    public String showFileClaimForm(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("policies", policyService.getPoliciesByCustomer(user.getId()));
        model.addAttribute("claim", new Claim());
        return "claim/form";
    }

    @PostMapping("/file")
    public String fileClaim(Claim claim, Principal principal) {
        if (principal == null)
            return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();

        claim.setCustomer(user);
        claim.setStatus(com.example.icsm.model.enums.ClaimStatus.Pending);
        claimService.saveClaim(claim);

        // Notification for Customer (No specific sender for system confirmation)
        notificationService.createNotification(user, null, "Claim Filed", 
            "Your claim for policy " + (claim.getPolicy() != null ? claim.getPolicy().getName() : "Unknown") + " has been received.", 
            com.example.icsm.model.enums.NotificationType.system);

        return "redirect:/claim";
    }

    @GetMapping("/{id}/edit")
    public String showEditClaimForm(@PathVariable Long id, Model model) {
        Claim claim = claimService.getClaimById(id).orElseThrow();
        model.addAttribute("claim", claim);
        model.addAttribute("policies", policyService.getPoliciesByCustomer(claim.getCustomer().getId()));
        return "claim/form";
    }

    @PostMapping("/{id}/edit")
    public String editClaim(@PathVariable Long id, Claim claim) {
        Claim existing = claimService.getClaimById(id).orElseThrow();
        claim.setCustomer(existing.getCustomer());
        claim.setStatus(existing.getStatus());
        claimService.saveClaim(claim);
        return "redirect:/claim";
    }

    @PostMapping("/{id}/delete")
    public String deleteClaim(@PathVariable Long id) {
        claimService.deleteClaim(id);
        return "redirect:/claim";
    }

    @PostMapping("/{id}/approve")
    public String approveClaim(@PathVariable Long id) {
        Claim claim = claimService.getClaimById(id).orElseThrow();
        claim.setStatus(com.example.icsm.model.enums.ClaimStatus.Settled);
        claimService.saveClaim(claim);

        // Notification for Customer
        notificationService.createNotification(claim.getCustomer(), null, "Claim Approved", 
            "Good news! Your claim #" + id + " has been approved and settled.", 
            com.example.icsm.model.enums.NotificationType.system);

        return "redirect:/claim";
    }

    @PostMapping("/{id}/reject")
    public String rejectClaim(@PathVariable Long id) {
        Claim claim = claimService.getClaimById(id).orElseThrow();
        claim.setStatus(com.example.icsm.model.enums.ClaimStatus.Rejected);
        claimService.saveClaim(claim);

        // Notification for Customer
        notificationService.createNotification(claim.getCustomer(), null, "Claim Rejected", 
            "Your claim #" + id + " has been reviewed and rejected. Please contact support for details.", 
            com.example.icsm.model.enums.NotificationType.system);

        return "redirect:/claim";
    }
}
