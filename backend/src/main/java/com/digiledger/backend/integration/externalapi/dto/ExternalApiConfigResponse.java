package com.digiledger.backend.integration.externalapi.dto;

import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;

public record ExternalApiConfigResponse(
        String apiCode,
        String displayName,
        String baseUrl,
        String authType,
        boolean apiKeyConfigured,
        String maskedApiKey,
        String configJson,
        int timeoutMs,
        boolean enabled
) {
    public static ExternalApiConfigResponse from(ExternalApiConfig config) {
        String secret = config.getCredentialSecret();
        return new ExternalApiConfigResponse(
                config.getApiCode(), config.getDisplayName(), config.getBaseUrl(), config.getAuthType(),
                secret != null && !secret.isBlank(), mask(secret), config.getConfigJson(),
                config.getTimeoutMs() == null ? 15000 : config.getTimeoutMs(), Boolean.TRUE.equals(config.getEnabled())
        );
    }

    private static String mask(String secret) {
        if (secret == null || secret.isBlank()) return null;
        return "********";
    }
}
