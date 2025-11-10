package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.WishlistItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 心愿单 Mapper。
 */
@Mapper
public interface WishlistMapper {

    List<WishlistItem> findAll();

    WishlistItem findById(@Param("id") Long id);

    int insert(WishlistItem item);

    int update(WishlistItem item);

    int delete(@Param("id") Long id);
+    int markConverted(@Param("id") Long id, @Param("assetId") Long assetId);
 }
