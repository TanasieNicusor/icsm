package com.example.icsm.controller;

import com.example.icsm.model.Policy;
import com.example.icsm.service.PolicyService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyRestController {

    private final PolicyService policyService;

    @GetMapping("/{id}/preview")
    public ResponseEntity<PolicyPreviewDTO> getPolicyPreview(@PathVariable Long id) {
        return policyService.getPolicyById(id)
                .map(policy -> ResponseEntity.ok(PolicyPreviewDTO.builder()
                        .id(policy.getId())
                        .customerName(policy.getCustomer().getFullName())
                        .typeName(policy.getPolicyType() != null ? policy.getPolicyType().getName() : "General")
                        .coverage(policy.getCoverageAmount().toString())
                        .status(policy.getStatus().name())
                        .startDate(policy.getStartDate() != null ? policy.getStartDate().toString() : "N/A")
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    @Data
    @Builder
    public static class PolicyPreviewDTO {
        private Long id;
        private String customerName;
        private String typeName;
        private String coverage;
        private String status;
        private String startDate;
    }
}
