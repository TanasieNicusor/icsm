package com.example.icsm.controller;

import com.example.icsm.model.ChangeLog;
import com.example.icsm.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/changelog")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Admin')")
public class ChangeLogController {

    private final AuditService auditService;

    @GetMapping
    public String viewChangeLog(@RequestParam(required = false) String entityName,
                                @RequestParam(required = false) String changedBy,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                Model model) {
        
        List<ChangeLog> logs = auditService.getLogs(entityName, changedBy, startDate, endDate);
        model.addAttribute("logs", logs);
        model.addAttribute("entityName", entityName);
        model.addAttribute("changedBy", changedBy);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        return "admin/changelog";
    }

    @PostMapping("/rollback/{id}")
    public String rollback(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            auditService.rollbackChange(id);
            redirectAttributes.addFlashAttribute("successMessage", "Change successfully rolled back.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rollback failed: " + e.getMessage());
        }
        return "redirect:/admin/changelog";
    }
}
