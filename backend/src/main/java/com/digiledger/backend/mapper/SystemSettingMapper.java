package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SystemSettingMapper {

    SystemSetting findLatest();

    void insertDefaultProvider(@Param("provider") String provider);

    void updateDefaultProvider(@Param("id") Long id, @Param("provider") String provider);
    void insertImageSearchProviders(@Param("providers") String providers);
    void updateImageSearchProviders(@Param("id") Long id, @Param("providers") String providers);
    void insertPreferences(SystemSetting setting);
    void updatePreferences(SystemSetting setting);
}
