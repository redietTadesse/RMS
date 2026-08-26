package com.rms.service;

import com.rms.entity.SystemSetting;
import com.rms.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private SystemSettingRepository settingsRepository;

    @InjectMocks
    private SettingsService settingsService;

    // ✅ TEST 1: Get int setting
    @Test
    void getIntSetting_ShouldReturnValue_WhenExists() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey("penalty_rate");
        setting.setSettingValue("25");

        when(settingsRepository.findBySettingKey("penalty_rate")).thenReturn(Optional.of(setting));

        int result = settingsService.getIntSetting("penalty_rate", 20);

        assertEquals(25, result);
    }

    // ✅ TEST 2: Get int setting - return default
    @Test
    void getIntSetting_ShouldReturnDefault_WhenNotExists() {
        when(settingsRepository.findBySettingKey("penalty_rate")).thenReturn(Optional.empty());

        int result = settingsService.getIntSetting("penalty_rate", 20);

        assertEquals(20, result);
    }

    // ✅ TEST 3: Update setting
    @Test
    void updateSetting_ShouldSave_WhenKeyExists() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey("penalty_rate");
        setting.setSettingValue("20");

        when(settingsRepository.findBySettingKey("penalty_rate")).thenReturn(Optional.of(setting));
        when(settingsRepository.save(any(SystemSetting.class))).thenReturn(setting);

        settingsService.updateSetting("penalty_rate", "25", "Updated rate", "admin");

        verify(settingsRepository).save(any(SystemSetting.class));
        assertEquals("25", setting.getSettingValue());
    }
}