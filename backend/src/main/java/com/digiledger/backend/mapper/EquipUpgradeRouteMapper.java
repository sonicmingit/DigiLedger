package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.EquipUpgradeRoute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipUpgradeRouteMapper {

    List<EquipUpgradeRoute> findAll();

    EquipUpgradeRoute findById(@Param("id") Long id);

    int insert(EquipUpgradeRoute route);

    int update(EquipUpgradeRoute route);

    int softDelete(@Param("id") Long id);
}
