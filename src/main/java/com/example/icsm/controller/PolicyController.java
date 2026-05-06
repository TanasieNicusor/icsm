package com.example.icsm.controller;

import com.example.icsm.model.Policy;
import com.example.icsm.service.DocumentService;
import com.example.icsm.service.PolicyService;
import com.example.icsm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final DocumentService documentService;
    private final UserRepository userRepository;

    @GetMapping
    public String listPolicies(Model model) {
        // Hardcoded userId 1L
        model.addAttribute("policies", policyService.getPoliciesByCustomer(1L));
        return "policy/list";
    }

    @GetMapping("/add")
    public String showAddPolicyForm(Model model) {
        model.addAttribute("policy", new Policy());
        model.addAttribute("policyTypes", policyService.getAllPolicyTypes());
        return "policy/form";
    }

    @PostMapping("/add")
    public String addPolicy(Policy policy) {
        policy.setCustomer(userRepository.findById(1L).orElseThrow());
        policy.setStatus(com.example.icsm.model.enums.PolicyStatus.Pending);
        policyService.savePolicy(policy);
        return "redirect:/policies";
    }

    @PostMapping("/{id}/edit")
    public String editPolicy(@PathVariable Long id, Policy policy) {
        Policy existing = policyService.getPolicyById(id).orElseThrow();
        policy.setCustomer(existing.getCustomer());
        policy.setStatus(existing.getStatus());
        policyService.savePolicy(policy);
        return "redirect:/policies";
    }

    @GetMapping("/{id}/edit")
    public String showEditPolicyForm(@PathVariable Long id, Model model) {
        Policy policy = policyService.getPolicyById(id).orElseThrow();
        model.addAttribute("policy", policy);
        return "policy/form";
    }

    @PostMapping("/{id}/delete")
    public String deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return "redirect:/policies";
    }

    @GetMapping("/{id}")
    public String viewPolicy(@PathVariable Long id, Model model) {
        Policy policy = policyService.getPolicyById(id).orElseThrow();
        model.addAttribute("policy", policy);
        model.addAttribute("documents", documentService.getDocumentsByPolicy(id));
        return "policy/details";
    }

    @GetMapping("/{id}/add-document")
    public String addDocument(@PathVariable Long id, Model model) {
        model.addAttribute("policyId", id);
        return "policy/add-document";
    }
}
