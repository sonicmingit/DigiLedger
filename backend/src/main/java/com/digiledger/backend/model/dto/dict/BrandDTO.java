package com.digiledger.backend.model.dto.dict;

/**
 * 品牌字典 DTO。
 */
public record BrandDTO(
        Long id,
        String name,
        String alias,
        String initial,
        Integer sort
) {
}
