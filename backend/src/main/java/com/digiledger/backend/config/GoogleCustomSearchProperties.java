package com.digiledger.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Google Custom Search 配置
 */
@Component
@ConfigurationProperties(prefix = "app.external.google.custom-search")
public class GoogleCustomSearchProperties {

    /** API Key */
    private String apiKey = "";

    /** 自定义搜索引擎 ID */
    private String cseId ="";

    /** 请求地址 */
    private String endpoint = "https://www.googleapis.com/customsearch/v1";

    /** 是否启用 */
    private boolean enabled = true;

    /** 最大返回数量 */
    private int maxResults = 8;

    /** 接口超时时间（毫秒） */
    private int timeoutMs = 10000;

    /** SafeSearch 配置 */
    private String safe = "off";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCseId() {
        return cseId;
    }

    public void setCseId(String cseId) {
        this.cseId = cseId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getSafe() {
        return safe;
    }

    public void setSafe(String safe) {
        this.safe = safe;
    }
}
