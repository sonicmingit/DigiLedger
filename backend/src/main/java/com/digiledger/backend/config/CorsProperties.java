package com.digiledger.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * H5 客户端跨域配置。
 *
 * <p>默认仅允许本机开发地址和常见局域网地址。部署到公网域名时，应通过
 * {@code DL_CORS_ALLOWED_ORIGIN_PATTERNS} 显式设置允许的前端来源。</p>
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "http://localhost:[*]",
            "https://localhost",
            "https://localhost:[*]",
            "http://127.0.0.1:[*]",
            "http://[::1]:[*]",
            "http://10.*:[*]",
            "http://172.*:[*]",
            "http://192.168.*:[*]"
    ));

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }
}
