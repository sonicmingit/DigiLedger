package com.digiledger.backend.service;

import com.digiledger.backend.mapper.SystemSettingMapper;
import com.digiledger.backend.model.dto.setting.PreferencesRequest;
import com.digiledger.backend.model.entity.SystemSetting;
import com.digiledger.backend.service.impl.PreferencesServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PreferencesServiceImplTest {
    @Test void createsPreferencesWithoutOverwritingStorageConfiguration() {
        SystemSettingMapper mapper = mock(SystemSettingMapper.class);
        when(mapper.findLatest()).thenReturn(null);
        PreferencesServiceImpl service = new PreferencesServiceImpl(mapper);
        service.update(new PreferencesRequest("cny", "YYYY-MM-DD", true, "03:30"));
        verify(mapper).insertPreferences(argThat(s -> "CNY".equals(s.getCurrency()) && Boolean.TRUE.equals(s.getAutoBackupEnabled())));
        assertEquals("CNY", service.get().currency());
    }
}
