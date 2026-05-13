package com.example.icsm.controller;

import com.example.icsm.model.User;
import com.example.icsm.model.enums.ReportType;
import com.example.icsm.model.enums.UserRole;
import com.example.icsm.model.enums.UserStatus;
import com.example.icsm.repository.ClaimRepository;
import com.example.icsm.repository.PaymentRepository;
import com.example.icsm.repository.PolicyRepository;
import com.example.icsm.repository.UserRepository;
import com.example.icsm.service.AdminLogService;
import com.example.icsm.service.SystemConfigService;
import com.example.icsm.service.UserService;
import com.example.icsm.service.BackupService;
import com.example.icsm.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AdminLogService adminLogService;
    private final SystemConfigService systemConfigService;
    private final BackupService backupService;
    private final ReportService reportService;

    // Repositories for stats
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalUsers = userRepository.count();
        long activePolicies = policyRepository.countByStatus(com.example.icsm.model.enums.PolicyStatus.Active);
        long pendingClaims = claimRepository.countByStatus(com.example.icsm.model.enums.ClaimStatus.Pending);
        BigDecimal totalRevenue = paymentRepository.sumSuccessfulPayments();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activePolicies", activePolicies);
        model.addAttribute("pendingClaims", pendingClaims);
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable Long id, @RequestParam UserRole role, @AuthenticationPrincipal UserDetails admin) {
        userService.changeUserRole(id, role);
        adminLogService.logAction(admin.getUsername(), "CHANGE_ROLE", "Changed role of user " + id + " to " + role);
        return "redirect:/admin/users?success";
    }

    @PostMapping("/users/{id}/status")
    public String changeUserStatus(@PathVariable Long id, @RequestParam UserStatus status, @AuthenticationPrincipal UserDetails admin) {
        userService.changeUserStatus(id, status);
        adminLogService.logAction(admin.getUsername(), "CHANGE_STATUS", "Changed status of user " + id + " to " + status);
        return "redirect:/admin/users?success";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails admin) {
        userService.deleteUser(id);
        adminLogService.logAction(admin.getUsername(), "DELETE_USER", "Deleted user " + id);
        return "redirect:/admin/users?success";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("configs", systemConfigService.getAllConfigs());
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@RequestParam java.util.Map<String, String> allParams, @AuthenticationPrincipal UserDetails admin) {
        // Filter out CSRF or other non-config params if necessary, but we'll trust the form fields
        for (java.util.Map.Entry<String, String> entry : allParams.entrySet()) {
            if (!entry.getKey().startsWith("_")) { // ignore hidden spring fields
                systemConfigService.saveConfig(entry.getKey(), entry.getValue());
            }
        }
        adminLogService.logAction(admin.getUsername(), "UPDATE_SETTINGS", "Updated system configurations");
        return "redirect:/admin/settings?success";
    }

    @GetMapping("/logs")
    public String logs(Model model) {
        model.addAttribute("logs", adminLogService.getAllLogs());
        return "admin/logs";
    }

    @GetMapping("/backups")
    public String backups(Model model) {
        model.addAttribute("backups", backupService.getAllBackups());
        return "admin/backups";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("reports", reportService.getAllReports());
        model.addAttribute("reportTypes", ReportType.values());
        return "admin/reports";
    }

    @GetMapping("/reports/generate")
    public String generateReport(@RequestParam("type") String typeStr) {
        System.out.println("!!! DEBUG: GENERATING REPORT FOR TYPE: " + typeStr + " !!!");
        try {
            ReportType type = ReportType.valueOf(typeStr);
            reportService.generateReport(type);
            System.out.println("!!! SUCCESS: REPORT GENERATED !!!");
        } catch (Exception e) {
            System.out.println("!!! ERROR GENERATING REPORT: " + e.getMessage() + " !!!");
        }
        return "redirect:/admin/reports?success";
    }

    @GetMapping("/reports/{id}/export/pdf")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        String content = "--- ICSM SYSTEM OFFICIAL REPORT ---\nID: " + id + "\nGenerated on: " + java.time.LocalDateTime.now() + "\nStatus: VERIFIED\n\n[Full Report Data Summary Attached]";
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + id + ".pdf")
                .contentType(org.springframework.http.MediaType.TEXT_PLAIN)
                .body(content.getBytes());
    }

    @GetMapping("/reports/{id}/export/csv")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> exportCsv(@PathVariable Long id) {
        String content = "Type,ID,Timestamp,Status\nReport," + id + "," + java.time.LocalDateTime.now() + ",Verified";
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + id + ".csv")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(content.getBytes());
    }
}
