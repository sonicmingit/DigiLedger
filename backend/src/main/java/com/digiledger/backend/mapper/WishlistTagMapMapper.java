package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.WishlistTagMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WishlistTagMapMapper {

    int deleteByWishlistId(@Param("wishlistId") Long wishlistId);

    int batchInsert(@Param("list") List<WishlistTagMap> mappings);

    List<WishlistTagMap> findByWishlistIds(@Param("wishlistIds") List<Long> wishlistIds);

    List<Long> findTagIdsByWishlistId(@Param("wishlistId") Long wishlistId);

    long countByTagId(@Param("tagId") Long tagId);
}
