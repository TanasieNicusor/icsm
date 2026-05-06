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
@RequestMapping("/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final PolicyService policyService;
    private final UserRepository userRepository;

    @GetMapping
    public String listClaims(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        if (user.getRole() == com.example.icsm.model.enums.UserRole.Agent) {
            model.addAttribute("claims", claimService.getAllClaims()); // Agents review all
            return "claim/review";
        } else {
            model.addAttribute("claims", claimService.getClaimsByCustomer(user.getId()));
            return "claim/list";
        }
    }

    @GetMapping("/file")
    public String showFileClaimForm(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("policies", policyService.getPoliciesByCustomer(user.getId()));
        model.addAttribute("claim", new Claim());
        return "claim/form";
    }

    @PostMapping("/file")
    public String fileClaim(Claim claim, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        claim.setCustomer(user);
        claim.setStatus(com.example.icsm.model.enums.ClaimStatus.Pending);
        claimService.saveClaim(claim);
        return "redirect:/claims";
    }

    @GetMapping("/{id}/edit")
    public String showEditClaimForm(@PathVariable Long id, Model model) {
        Claim claim = claimService.getClaimById(id).orElseThrow();
        model.addAttribute("claim", claim);
        model.addAttribute("policies", policyService.getPoliciesByCustomer(1L));
        return "claim/form";
    }

    @PostMapping("/{id}/edit")
    public String editClaim(@PathVariable Long id, Claim claim) {
        Claim existing = claimService.getClaimById(id).orElseThrow();
        claim.setCustomer(existing.getCustomer());
        claim.setStatus(existing.getStatus());
        claimService.saveClaim(claim);
        return "redirect:/claims";
    }

    @PostMapping("/{id}/delete")
    public String deleteClaim(@PathVariable Long id) {
        claimService.deleteClaim(id);
        return "redirect:/claims";
    }

    @PostMapping("/{id}/approve")
    public String approveClaim(@PathVariable Long id) {
        Claim claim = claimService.getClaimById(id).orElseThrow();
        claim.setStatus(com.example.icsm.model.enums.ClaimStatus.Settled);
        claimService.saveClaim(claim);
        return "redirect:/claims";
    }

    @PostMapping("/{id}/reject")
    public String rejectClaim(@PathVariable Long id) {
        Claim claim = claimService.getClaimById(id).orElseThrow();
        claim.setStatus(com.example.icsm.model.enums.ClaimStatus.Rejected);
        claimService.saveClaim(claim);
        return "redirect:/claims";
    }
}
