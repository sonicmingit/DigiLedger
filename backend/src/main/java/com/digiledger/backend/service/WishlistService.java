package com.digiledger.backend.service;

import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.model.dto.wishlist.WishlistDTO;
import com.digiledger.backend.model.dto.wishlist.WishlistRequest;

import java.util.List;

/**
 * 心愿单服务接口。
 */
public interface WishlistService {

    List<WishlistDTO> listAll(String status);

    WishlistDTO getById(Long id);

    Long create(WishlistRequest request);

    void update(Long id, WishlistRequest request);

    void delete(Long id);

    Long convertToAsset(Long id, AssetCreateRequest request);
}
