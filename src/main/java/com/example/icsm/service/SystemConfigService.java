package com.example.icsm.service;

import com.example.icsm.model.SystemConfig;
import com.example.icsm.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
