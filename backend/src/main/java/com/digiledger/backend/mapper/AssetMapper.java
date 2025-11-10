package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.DeviceAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssetMapper {

    List<DeviceAsset> findAll(@Param("status") String status,
                              @Param("keyword") String keyword);

    DeviceAsset findById(@Param("id") Long id);

    int insert(DeviceAsset asset);

    int update(DeviceAsset asset);

    int delete(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updatePrimaryInfo(@Param("id") Long id,
                          @Param("purchaseId") Long purchaseId,
                          @Param("purchaseDate") java.time.LocalDate purchaseDate);
}
