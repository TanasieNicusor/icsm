package com.example.icsm.controller;

import com.example.icsm.model.Claim;
import com.example.icsm.service.ClaimService;
import com.example.icsm.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final PolicyService policyService;

    @GetMapping
    public String listClaims(Model model) {
        model.addAttribute("claims", claimService.getClaimsByCustomer(1L));
        return "claim/list";
    }

    @GetMapping("/file")
    public String showFileClaimForm(Model model) {
        model.addAttribute("policies", policyService.getPoliciesByCustomer(1L));
        model.addAttribute("claim", new Claim());
        return "claim/form";
    }

    @PostMapping("/file")
    public String fileClaim(Claim claim) {
        // Set hardcoded customer for demo
        // In real app, this comes from SecurityContext
        claim.setCustomer(policyService.getPolicyById(claim.getPolicy().getId()).get().getCustomer());
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
}
