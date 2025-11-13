package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.DictBrand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 品牌字典 Mapper。
 */
@Mapper
public interface DictBrandMapper {

    List<DictBrand> findAll();

    DictBrand findById(@Param("id") Long id);

    int insert(DictBrand brand);

    int update(DictBrand brand);

    int delete(@Param("id") Long id);

    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);
}
