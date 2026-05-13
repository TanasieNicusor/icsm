package com.example.icsm.controller;

import com.example.icsm.model.Policy;
import com.example.icsm.service.DocumentService;
import com.example.icsm.service.PolicyService;
import com.example.icsm.repository.UserRepository;
import com.example.icsm.model.enums.PolicyStatus;
import com.example.icsm.model.enums.UserRole;
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
    private final com.example.icsm.service.NotificationService notificationService;
    private final com.example.icsm.repository.PolicyRepository policyRepository;
    private final com.example.icsm.service.PolicySearchService policySearchService;

    @GetMapping
    public String listPolicies(com.example.icsm.dto.SearchCriteria criteria, Model model, Principal principal, jakarta.servlet.http.HttpServletRequest request) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        if (user.getRole() != null && user.getRole() == UserRole.Agent) {
            return "redirect:/policies/portfolio";
        }
        
        // Filter by current customer
        org.springframework.data.domain.Page<Policy> policyPage = policySearchService.searchPolicies(criteria, user.getId(), null);

        model.addAttribute("policies", policyPage.getContent());
        model.addAttribute("page", policyPage);
        model.addAttribute("criteria", criteria);
        model.addAttribute("history", policySearchService.getSearchHistory(user));
        
        // Save history if searching
        if (criteria.getKeyword() != null || criteria.getStatus() != null) {
            policySearchService.saveSearchHistory(user, criteria, "/policies");
        }
        
        return "policy/list";
    }

    @GetMapping("/portfolio")
    public String viewPortfolio(com.example.icsm.dto.SearchCriteria criteria, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        if (user.getRole() == null || user.getRole() != UserRole.Agent) return "redirect:/policies";
        
        // Show only the "Template" policies posted by this agent
        // Use search service to filter by agent and isTemplate
        criteria.setAgentId(user.getId());
        org.springframework.data.domain.Page<Policy> policyPage = policySearchService.searchPolicies(criteria, null, true);
        
        model.addAttribute("policies", policyPage.getContent());
        model.addAttribute("page", policyPage);
        model.addAttribute("criteria", criteria);
        model.addAttribute("history", policySearchService.getSearchHistory(user));
        
        // Save history if searching
        if (criteria.getKeyword() != null || criteria.getStatus() != null) {
            policySearchService.saveSearchHistory(user, criteria, "/policies/portfolio");
        }
        
        return "policy/portfolio";
    }

    @GetMapping("/browse")
    public String browsePolicies(com.example.icsm.dto.SearchCriteria criteria, Model model, Principal principal) {
        // Only show templates with an assigned agent
        org.springframework.data.domain.Page<Policy> policyPage = policySearchService.searchPolicies(criteria, null, true);
        
        // Filter out templates with no agent (if any) - though spec could handle this too
        List<Policy> templates = policyPage.getContent().stream()
                .filter(p -> p.getAgent() != null)
                .collect(Collectors.toList());
        
        model.addAttribute("policies", templates);
        model.addAttribute("page", policyPage);
        model.addAttribute("criteria", criteria);
        
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                model.addAttribute("history", policySearchService.getSearchHistory(user));
                // Save history if searching
                if (criteria.getKeyword() != null || criteria.getStatus() != null) {
                    policySearchService.saveSearchHistory(user, criteria, "/policies/browse");
                }
            }
        }
        
        return "policy/browse";
    }

    @GetMapping("/{id}/enroll")
    public String showEnrollmentForm(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        if (user.getRole() == null || user.getRole() != UserRole.Customer) {
            return "redirect:/policies/browse?error=Only customers can enroll";
        }

        Policy template = policyService.getPolicyById(id).orElseThrow();
        model.addAttribute("policy", template);
        return "policy/enroll";
    }

    @PostMapping("/{id}/purchase")
    public String purchasePolicy(@PathVariable Long id, 
                               @RequestParam LocalDate startDate, 
                               @RequestParam LocalDate endDate, 
                               Principal principal) {
        try {
            if (principal == null) return "redirect:/login";
            User customer = userRepository.findByEmail(principal.getName()).orElseThrow();
            Policy template = policyService.getPolicyById(id)
                    .orElseThrow(() -> new RuntimeException("Policy template not found: " + id));

            // Resolve the agent from the template
            User agent = template.getAgent();
            if (agent == null) {
                System.err.println("WARNING: Policy template " + id + " has no assigned agent. Notification will not be sent.");
            }
            
            // Create a new personalized policy (PENDING approval)
            Policy personalPolicy = Policy.builder()
                    .name(template.getName())
                    .agent(agent)
                    .customer(customer)
                    .parentPolicy(template)
                    .policyType(template.getPolicyType())
                    .coverageAmount(template.getCoverageAmount())
                    .premiumAmount(template.getPremiumAmount())
                    .paymentFrequency(template.getPaymentFrequency())
                    .status(PolicyStatus.Pending)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
            
            // Save first to get the generated ID
            personalPolicy = policyService.savePolicy(personalPolicy);
            System.out.println("INFO: Enrollment policy created with ID=" + personalPolicy.getId() + " for customer " + customer.getFullName());

            // Send notification to the agent
            if (agent != null) {
                System.out.println("INFO: Sending enrollment notification to agent " + agent.getFullName() + " (id=" + agent.getId() + ")");
                notificationService.createNotification(
                    agent, 
                    customer, 
                    "New Pending Client Received", 
                    "Your policy '" + template.getName() + "' has received a new pending client: " + customer.getFullName() + ". Please review and confirm the enrollment.", 
                    com.example.icsm.model.enums.NotificationType.enrollment_request,
                    personalPolicy.getId()
                );
                System.out.println("INFO: Notification sent successfully.");
            }

            return "redirect:/policies?success=Enrollment submitted! Awaiting agent approval.";
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR in purchasePolicy: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/policies/browse?error=" + e.getMessage();
        }
    }

    @PostMapping("/confirm-enrollment/{id}")
    public String confirmEnrollment(@PathVariable Long id, Principal principal) {
        if (principal == null) return "redirect:/login";
        User agent = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        Policy personalPolicy = policyService.getPolicyById(id).orElseThrow();
        
        // Security check
        if (personalPolicy.getAgent() == null || !personalPolicy.getAgent().getId().equals(agent.getId())) {
            return "redirect:/policies/portfolio?error=Unauthorized";
        }
        
        personalPolicy.setStatus(PolicyStatus.Active);
        personalPolicy.setNextPaymentDate(personalPolicy.getStartDate());
        policyService.savePolicy(personalPolicy);
        
        // Notify Customer
        notificationService.createNotification(personalPolicy.getCustomer(), agent, 
            "Enrollment Confirmed", 
            "Your enrollment for " + personalPolicy.getName() + " has been confirmed by " + agent.getFullName() + ".", 
            com.example.icsm.model.enums.NotificationType.system);
            
        return "redirect:/policies/portfolio?success=Enrollment confirmed!";
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
        User agent = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        if (agent.getRole() == null || agent.getRole() != UserRole.Agent) return "redirect:/policies";
        
        policy.setAgent(agent);
        policy.setCustomer(agent); // Self-owned as a template
        policy.setStatus(PolicyStatus.Active);
        policyService.savePolicy(policy);
        
        return "redirect:/policies/portfolio";
    }

    @GetMapping("/{id}")
    public String viewPolicy(@PathVariable Long id, Model model, Principal principal) {
        Policy policy = policyService.getPolicyById(id).orElseThrow();
        User currentUser = null;
        if (principal != null) {
            currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
        }

        model.addAttribute("policy", policy);
        model.addAttribute("documents", documentService.getDocumentsByPolicy(id));
        
        // If it's a template and viewer is the agent, show enrolled clients
        if (policy.getParentPolicy() == null && currentUser != null && currentUser.getRole() == UserRole.Agent) {
            List<Policy> enrollments = policyRepository.findByParentPolicyId(id);
            model.addAttribute("enrolledClients", enrollments);
        }
        
        return "policy/details";
    }

    @GetMapping("/{id}/schedule")
    public String viewPaymentSchedule(@PathVariable Long id, Model model) {
        Policy policy = policyService.getPolicyById(id).orElseThrow();
        
        java.util.List<java.time.LocalDate> schedule = new java.util.ArrayList<>();
        java.time.LocalDate current = policy.getNextPaymentDate() != null ? policy.getNextPaymentDate() : policy.getStartDate();
        java.time.LocalDate end = policy.getEndDate() != null ? policy.getEndDate() : current.plusYears(1);

        while (current != null && !current.isAfter(end)) {
            schedule.add(current);
            switch (policy.getPaymentFrequency()) {
                case monthly: current = current.plusMonths(1); break;
                case quarterly: current = current.plusMonths(3); break;
                case yearly: current = current.plusYears(1); break;
                default: current = current.plusMonths(1); break;
            }
        }

        model.addAttribute("policy", policy);
        model.addAttribute("scheduleDates", schedule);
        return "policy/schedule";
    }

    @GetMapping("/{id}/add-document")
    public String addDocument(@PathVariable Long id, Model model) {
        model.addAttribute("policyId", id);
        return "policy/add-document";
    }

    @GetMapping("/{id}/edit")
    public String showEditPolicyForm(@PathVariable Long id, Model model) {
        Policy policy = policyService.getPolicyById(id).orElseThrow();
        model.addAttribute("policy", policy);
        model.addAttribute("policyTypes", policyService.getAllPolicyTypes());
        return "policy/form";
    }

    @PostMapping("/{id}/edit")
    public String editPolicy(@PathVariable Long id, @ModelAttribute Policy policy) {
        Policy existing = policyService.getPolicyById(id).orElseThrow();
        
        // Ensure we are updating the existing record
        policy.setId(id);
        policy.setCustomer(existing.getCustomer());
        policy.setAgent(existing.getAgent());
        policy.setParentPolicy(existing.getParentPolicy());
        policy.setStatus(existing.getStatus());
        policy.setCreatedAt(existing.getCreatedAt()); // Preserve creation date
        
        policyService.savePolicy(policy);
        return "redirect:/policies";
    }

    @PostMapping("/{id}/delete")
    public String deletePolicy(@PathVariable Long id, Principal principal) {
        User user = null;
        if (principal != null) {
            user = userRepository.findByEmail(principal.getName()).orElse(null);
        }
        
        policyService.deletePolicy(id);
        
        if (user != null && user.getRole() == UserRole.Agent) {
            return "redirect:/policies/portfolio";
        }
        return "redirect:/policies";
    }
}
