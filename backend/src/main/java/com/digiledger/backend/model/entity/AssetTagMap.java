package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetTagMap {
    private Long assetId;
    private Long tagId;
    private LocalDateTime createdAt;
}
