package com.example.icsm.service;

import com.example.icsm.model.AdminLog;
import com.example.icsm.repository.AdminLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository adminLogRepository;

    public void logAction(String adminEmail, String actionType, String description) {
        AdminLog log = AdminLog.builder()
                .adminEmail(adminEmail)
                .actionType(actionType)
                .description(description)
                .build();
        adminLogRepository.save(log);
    }

    public List<AdminLog> getAllLogs() {
        return adminLogRepository.findAllByOrderByTimestampDesc();
    }
}
