package com.digiledger.backend.service.impl;

import com.digiledger.backend.mapper.SystemSettingMapper;
import com.digiledger.backend.model.dto.setting.*;
import com.digiledger.backend.model.entity.SystemSetting;
import com.digiledger.backend.service.PreferencesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
/** 仅更新用户偏好列，保留同一 sys_setting 行中的对象存储与找图配置。 */
public class PreferencesServiceImpl implements PreferencesService {
    private final SystemSettingMapper mapper;
    public PreferencesServiceImpl(SystemSettingMapper mapper) { this.mapper = mapper; }
    @Override public PreferencesDTO get() {
        SystemSetting s = mapper.findLatest();
        if (s == null) return new PreferencesDTO("CNY", "YYYY-MM-DD", false, "02:00");
        return new PreferencesDTO(Optional.ofNullable(s.getCurrency()).orElse("CNY"),
                Optional.ofNullable(s.getDateFormat()).orElse("YYYY-MM-DD"),
                Boolean.TRUE.equals(s.getAutoBackupEnabled()), Optional.ofNullable(s.getAutoBackupTime()).orElse("02:00"));
    }
    @Override @Transactional public void update(PreferencesRequest request) {
        SystemSetting s = mapper.findLatest();
        boolean insert = s == null;
        if (insert) s = new SystemSetting();
        s.setCurrency(request.currency().trim().toUpperCase()); s.setDateFormat(request.dateFormat().trim());
        s.setAutoBackupEnabled(request.autoBackupEnabled()); s.setAutoBackupTime(request.autoBackupTime());
        if (insert) mapper.insertPreferences(s); else mapper.updatePreferences(s);
    }
}
