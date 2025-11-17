package com.digiledger.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bing 图片搜索配置
 */
@Component
@ConfigurationProperties(prefix = "app.external.bing.image-search")
public class BingImageSearchProperties {

    /** Bing API Key */
    private String apiKey;

    /** 请求地址 */
    private String endpoint = "https://api.bing.microsoft.com/v7.0/images/search";

    /** 默认每次获取数量 */
    private int defaultCount = 12;

    /** 最大数量限制 */
    private int maxCount = 20;

    /** 接口超时时间，单位毫秒 */
    private int timeoutMs = 10000;

    /** 搜索市场 */
    private String market = "zh-CN";

    /** 安全搜索级别 */
    private String safeSearch = "Moderate";

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

    public int getDefaultCount() {
        return defaultCount;
    }

    public void setDefaultCount(int defaultCount) {
        this.defaultCount = defaultCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getSafeSearch() {
        return safeSearch;
    }

    public void setSafeSearch(String safeSearch) {
        this.safeSearch = safeSearch;
    }
}
