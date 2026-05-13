package com.example.icsm.service;

import com.example.icsm.model.Report;
import com.example.icsm.model.enums.ReportType;
import com.example.icsm.repository.ReportRepository;
import com.example.icsm.repository.UserRepository;
import com.example.icsm.repository.ClaimRepository;
import com.example.icsm.repository.PolicyRepository;
import com.example.icsm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final PaymentRepository paymentRepository;

    private static final java.util.List<Report> mockReports = new java.util.ArrayList<>(java.util.List.of(
        Report.builder().type(ReportType.SALES_REPORT).parameters("Initial System Audit").createdAt(java.time.LocalDateTime.now().minusDays(1)).build()
    ));

    public List<Report> getAllReports() {
        return mockReports;
    }

    public void generateReport(ReportType type) {
        Report newReport = Report.builder()
                .type(type)
                .parameters("User-generated " + type.name())
                .createdAt(java.time.LocalDateTime.now())
                .build();
        mockReports.add(newReport);
        log.info("Successfully added report to live list: " + type);
    }
}
