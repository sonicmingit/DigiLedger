package com.digiledger.backend.model.dto.dict;

public record BrandDTO(
        Long id,
        String name,
        String alias,
        String initial,
        Integer sort
) {
}
