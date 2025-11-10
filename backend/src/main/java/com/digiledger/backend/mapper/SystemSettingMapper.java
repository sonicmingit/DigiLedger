package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemSettingMapper {

    SystemSetting findLatest();
}
