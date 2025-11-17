package com.digiledger.backend.model.dto.asset;

import jakarta.validation.constraints.NotNull;

/**
 * 抠图请求参数
 */
public class RemoveBgRequest {

    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    private Long attachmentId;

    private String coverUrl;

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}
