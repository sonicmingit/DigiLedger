package com.digiledger.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 上传配置属性，用于限制文件大小与类型。
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** 单文件最大体积（MB） */
    private long maxSizeMb = 5;

    /** 允许的 MIME 类型列表 */
    private List<String> allowedContentTypes;

    public long getMaxSizeMb() {
        return maxSizeMb;
    }

    public void setMaxSizeMb(long maxSizeMb) {
        this.maxSizeMb = maxSizeMb;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }
}
