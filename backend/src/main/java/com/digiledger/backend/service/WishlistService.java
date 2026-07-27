package com.digiledger.backend.service;

import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.model.dto.wishlist.WishlistDTO;
import com.digiledger.backend.model.dto.wishlist.WishlistRequest;

import java.util.List;
import com.digiledger.backend.model.dto.wishlist.WishlistPriceRequest;
import com.digiledger.backend.model.dto.wishlist.WishlistPriceHistoryDTO;

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
    void updatePrice(Long id, WishlistPriceRequest request);
    List<WishlistPriceHistoryDTO> getPriceHistory(Long id);
    /** 将心愿用购买确认信息转成物品，并保留心愿与新物品的关联。 */
    Long markPurchased(Long id, AssetCreateRequest request);
}
