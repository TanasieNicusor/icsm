package com.example.icsm.controller;

import com.example.icsm.model.Policy;
import com.example.icsm.service.DocumentService;
import com.example.icsm.service.PolicyService;
import com.example.icsm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.example.icsm.model.User;

@Controller
@RequestMapping("/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final DocumentService documentService;
    private final UserRepository userRepository;

    @GetMapping
    public String listPolicies(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        
        // One-time cleanup for ID 2 as requested
        policyService.getPolicyById(2L).ifPresent(p -> policyService.deletePolicy(2L));
        
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        if (user.getRole() == com.example.icsm.model.enums.UserRole.Agent) {
            // Only show "Generic Plans" (templates) for the agent
            List<Policy> myTemplates = policyService.getPoliciesByAgent(user.getId()).stream()
                    .filter(p -> p.getCustomer() != null && p.getCustomer().getId().equals(user.getId()))
                    .collect(Collectors.toList());
            model.addAttribute("policies", myTemplates);
            model.addAttribute("isAgent", true);
        } else {
            model.addAttribute("policies", policyService.getPoliciesByCustomer(user.getId()));
            model.addAttribute("isAgent", false);
        }
        return "policy/list";
    }

    @GetMapping("/browse")
    public String browsePolicies(Model model) {
        // Only show "Generic Plans" where customer == agent (our template logic)
        List<Policy> genericPlans = policyService.getAllPolicies().stream()
                .filter(p -> p.getCustomer() != null && p.getAgent() != null && 
                            p.getCustomer().getId().equals(p.getAgent().getId()))
                .collect(java.util.stream.Collectors.toList());
                
        model.addAttribute("policies", genericPlans);
        return "policy/browse";
    }

    @GetMapping("/{id}/enroll")
    public String showEnrollmentForm(@PathVariable Long id, Model model) {
        Policy template = policyService.getPolicyById(id).orElseThrow();
        model.addAttribute("policy", template);
        return "policy/enroll";
    }

    @PostMapping("/{id}/purchase")
    public String purchasePolicy(@PathVariable Long id, 
                               @RequestParam LocalDate startDate, 
                               @RequestParam LocalDate endDate, 
                               Principal principal) {
        if (principal == null) return "redirect:/login";
        User customer = userRepository.findByEmail(principal.getName()).orElseThrow();
        Policy template = policyService.getPolicyById(id).orElseThrow();
        
        // Create a new personalized policy for the customer
        Policy personalPolicy = Policy.builder()
                .name(template.getName())
                .agent(template.getAgent())
                .customer(customer)
                .policyType(template.getPolicyType())
                .coverageAmount(template.getCoverageAmount())
                .premiumAmount(template.getPremiumAmount())
                .paymentFrequency(template.getPaymentFrequency())
                .status(com.example.icsm.model.enums.PolicyStatus.Active)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        policyService.savePolicy(personalPolicy);
        return "redirect:/policies";
    }

    @GetMapping("/add")
    public String showAddPolicyForm(Model model) {
        model.addAttribute("policy", new Policy());
        model.addAttribute("policyTypes", policyService.getAllPolicyTypes());
        return "policy/form";
    }

    @PostMapping("/add")
    public String addPolicy(@ModelAttribute Policy policy, Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        
        try {
            User agent = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Logged in agent not found"));
            
            if (agent.getRole() != com.example.icsm.model.enums.UserRole.Agent) {
                return "redirect:/policies?error=Unauthorized";
            }
            
            policy.setAgent(agent);
            policy.setCustomer(agent); // Temporary owner
            policy.setStatus(com.example.icsm.model.enums.PolicyStatus.Pending);
            
            // Basic validation check
            if (policy.getName() == null || policy.getName().isEmpty()) {
                throw new RuntimeException("Policy name is required");
            }
            
            policyService.savePolicy(policy);
            return "redirect:/policies";
        } catch (Exception e) {
            model.addAttribute("error", "Error creating policy: " + e.getMessage());
            model.addAttribute("policy", policy);
            model.addAttribute("policyTypes", policyService.getAllPolicyTypes());
            return "policy/form";
        }
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
