package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.DictTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DictTagMapper {

    List<DictTag> findAll();

    DictTag findById(@Param("id") Long id);

    List<DictTag> findByIds(@Param("ids") List<Long> ids);

    List<DictTag> findByAssetId(@Param("assetId") Long assetId);

    int insert(DictTag tag);

    int update(DictTag tag);

    int delete(@Param("id") Long id);

    long countChildren(@Param("parentId") Long parentId);

    long countByNameAndParent(@Param("parentId") Long parentId, @Param("name") String name, @Param("excludeId") Long excludeId);
}
