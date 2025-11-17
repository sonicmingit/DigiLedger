package com.digiledger.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * remove.bg 相关配置
 */
@Component
@ConfigurationProperties(prefix = "app.remove-bg")
public class RemoveBgProperties {

    /**
     * Remove.bg API Key
     */
    private String apiKey;

    /**
     * Remove.bg 接口地址
     */
    private String endpoint = "https://api.remove.bg/v1.0/removebg";

    /**
     * 请求超时时间，单位毫秒
     */
    private int timeoutMs = 120000;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
