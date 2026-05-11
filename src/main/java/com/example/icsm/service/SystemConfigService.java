package com.example.icsm.service;

import com.example.icsm.model.SystemConfig;
import com.example.icsm.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @PostConstruct
    public void initDefaultSettings() {
        if (systemConfigRepository.count() == 0) {
            saveConfig("DEFAULT_CURRENCY", "USD");
            saveConfig("TAX_RATE", "0");
            saveConfig("GRACE_PERIOD_DAYS", "30");
        }
    }

    public String getConfigValue(String key) {
        return systemConfigRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .orElse(null);
    }

    private Optional<String> getConfigValueOptional(String key) {
        return systemConfigRepository.findById(key).map(SystemConfig::getConfigValue);
    }

    public int getGracePeriodDays() {
        return getConfigValueOptional("GRACE_PERIOD_DAYS")
                .map(Integer::parseInt)
                .orElse(15);
    }

    public java.math.BigDecimal getLateFeeAmount() {
        return getConfigValueOptional("LATE_FEE_AMOUNT")
                .map(java.math.BigDecimal::new)
                .orElse(new java.math.BigDecimal("25.00"));
    }

    public void saveConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.findById(key).orElse(new SystemConfig());
        config.setConfigKey(key);
        config.setConfigValue(value);
        systemConfigRepository.save(config);
    }

    public Map<String, String> getAllConfigs() {
        List<SystemConfig> configs = systemConfigRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (SystemConfig config : configs) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }
}
