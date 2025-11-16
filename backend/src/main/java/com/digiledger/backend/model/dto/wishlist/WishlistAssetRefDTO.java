package com.digiledger.backend.model.dto.wishlist;

/**
 * 心愿单关联资产信息，用于详情跳转展示。
 */
public record WishlistAssetRefDTO(
        Long assetId,
        String assetName,
        boolean available
) {
}
