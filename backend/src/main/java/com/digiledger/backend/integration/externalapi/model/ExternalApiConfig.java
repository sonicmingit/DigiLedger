package com.digiledger.backend.integration.externalapi.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方/私有服务连接配置。凭据仅允许由后端读取。
 */
@Data
public class ExternalApiConfig {
    private Long id;
    private String apiCode;
    private String displayName;
    private String baseUrl;
    private String authType;
    private String credentialSecret;
    private String configJson;
    private Integer timeoutMs;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
