package com.digiledger.backend.model.dto.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 单条物品相关链接。 */
public record AssetRelatedLinkRequest(
        @NotBlank(message = "相关链接地址不能为空") @Size(max = 2000, message = "相关链接地址过长") String url,
        @Size(max = 200, message = "相关链接说明过长") String description
) {
}
