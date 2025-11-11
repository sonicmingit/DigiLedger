package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.DictPlatform;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DictPlatformMapper {

    List<DictPlatform> findAll();

    DictPlatform findById(@Param("id") Long id);

    int insert(DictPlatform platform);

    int update(DictPlatform platform);

    int delete(@Param("id") Long id);

    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);
}
