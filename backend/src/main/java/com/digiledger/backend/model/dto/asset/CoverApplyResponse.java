package com.digiledger.backend.model.dto.asset;

/**
 * 封面设置结果
 */
public record CoverApplyResponse(
        Long attachmentId,
        String url,
        String objectKey
) {
}
