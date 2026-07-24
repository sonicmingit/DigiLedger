package com.digiledger.backend.integration.externalapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExternalApiConfigRequest(
        @NotBlank(message = "服务名称不能为空") String displayName,
        @NotBlank(message = "服务地址不能为空") String baseUrl,
        String authType,
        String apiKey,
        String configJson,
        @Min(value = 1000, message = "超时时间不能小于 1000ms")
        @Max(value = 120000, message = "超时时间不能大于 120000ms") Integer timeoutMs,
        Boolean enabled
) {
}
