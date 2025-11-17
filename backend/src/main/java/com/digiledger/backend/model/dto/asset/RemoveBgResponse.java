package com.digiledger.backend.model.dto.asset;

/**
 * 抠图后的封面信息
 */
public record RemoveBgResponse(
        Long attachmentId,
        String url
) {
}
