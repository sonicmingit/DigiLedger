package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemSettingMapper {

    SystemSetting findLatest();

    void insertDefaultProvider(@Param("provider") String provider);

    void updateDefaultProvider(@Param("id") Long id, @Param("provider") String provider);
    void insertPreferences(SystemSetting setting);
    void updatePreferences(SystemSetting setting);
}
