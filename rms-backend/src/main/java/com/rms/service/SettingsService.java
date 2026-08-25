package com.rms.service;

import com.rms.entity.SystemSetting;
import com.rms.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SettingsService {

    private final SystemSettingRepository settingsRepository;

    public SettingsService(SystemSettingRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @PostConstruct
    public void init() {
        initializeDefault("penalty_rate", "20", "Penalty rate percentage");
        initializeDefault("payment_window_end", "10", "Day of month after which penalties apply");
    }

    private void initializeDefault(String key, String value, String description) {
        if (settingsRepository.findBySettingKey(key).isEmpty()) {
            SystemSetting setting = new SystemSetting();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setDescription(description);
            setting.setUpdatedAt(LocalDateTime.now());
            setting.setUpdatedBy("system");
            settingsRepository.save(setting);
        }
    }

    public int getIntSetting(String key, int defaultValue) {
        return settingsRepository.findBySettingKey(key)
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public String getStringSetting(String key, String defaultValue) {
        return settingsRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    public void updateSetting(String key, String value, String description, String updatedBy) {
        SystemSetting setting = settingsRepository.findBySettingKey(key)
                .orElse(new SystemSetting());

        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription(description);
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy(updatedBy);

        settingsRepository.save(setting);
    }
}