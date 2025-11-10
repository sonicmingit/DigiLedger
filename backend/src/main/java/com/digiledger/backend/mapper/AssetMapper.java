package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.DeviceAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssetMapper {

    List<DeviceAsset> findAll();

    DeviceAsset findById(@Param("id") Long id);
}
