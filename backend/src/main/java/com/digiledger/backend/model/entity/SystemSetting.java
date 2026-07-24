package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemSetting {
    /** 主键 ID */
    private Long id;
    /** 默认币种 */
    private String currency;
    /** 对象存储提供方 */
    private String storageProvider;
    /** 对象存储 endpoint */
    private String storageEndpoint;
    /** 区域 */
    private String storageRegion;
    /** 桶名称 */
    private String storageBucket;
    /** AccessKey */
    private String storageAccessKey;
    /** SecretKey */
    private String storageSecretKey;
    /** 对外访问基地址 */
    private String storageBaseUrl;
    /** 默认智能找图服务 */
    private String defaultCoverProvider;
    /** 允许在物品封面搜图中使用的外接服务编码（JSON 数组） */
    private String imageSearchProviderCodes;
    private String dateFormat;
    private Boolean autoBackupEnabled;
    private String autoBackupTime;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
