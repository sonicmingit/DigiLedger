package com.digiledger.backend.integration.externalapi.dto;

import java.util.List;

public record ExternalApiTestResponse(
        String apiCode,
        boolean success,
        String message,
        int resultCount,
        List<ExternalApiTestItem> items
) {
}
