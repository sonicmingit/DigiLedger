package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.DictCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DictCategoryMapper {

    List<DictCategory> findAll();

    DictCategory findById(@Param("id") Long id);

    int insert(DictCategory category);

    int update(DictCategory category);

    int delete(@Param("id") Long id);

    long countChildren(@Param("parentId") Long parentId);

    long countByNameAndParent(@Param("parentId") Long parentId, @Param("name") String name, @Param("excludeId") Long excludeId);
}
