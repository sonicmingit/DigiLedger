package com.digiledger.backend.integration.externalapi.dto;

public record ExternalApiTestItem(
        String thumbnailUrl,
        String originalUrl,
        String title,
        String sourceUrl
) {
}
