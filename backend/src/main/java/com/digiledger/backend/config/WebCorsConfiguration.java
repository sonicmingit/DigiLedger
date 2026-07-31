package com.digiledger.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 为浏览器版 H5 客户端开放受控的 API 跨域访问。
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebCorsConfiguration implements WebMvcConfigurer {

    private final CorsProperties properties;

    public WebCorsConfiguration(CorsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        properties.getAllowedOriginPatterns().toArray(String[]::new)
                )
                .allowedMethods("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
