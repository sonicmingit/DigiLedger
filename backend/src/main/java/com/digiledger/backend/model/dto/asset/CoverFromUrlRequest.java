package com.digiledger.backend.model.dto.asset;

import jakarta.validation.constraints.NotBlank;

/**
 * 通过 URL 设置封面
 */
public class CoverFromUrlRequest {

    @NotBlank(message = "封面图片地址不能为空")
    private String sourceUrl;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
