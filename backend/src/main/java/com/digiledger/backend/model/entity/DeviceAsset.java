package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeviceAsset {
    /** 主键 ID */
    private Long id;
    /** 资产名称 */
    private String name;
    /** 资产类别 */
    private String category;
    /** 品牌 */
    private String brand;
    /** 型号 */
    private String model;
    /** 序列号 */
    private String serialNo;
    /** 状态（中文枚举） */
    private String status;
    /** 首次购买记录 ID（冗余） */
    private Long purchaseId;
    /** 首次购买日期（冗余） */
    private LocalDate purchaseDate;
    /** 启用日期 */
    private LocalDate enabledDate;
    /** 报废日期 */
    private LocalDate retiredDate;
    /** 标签 JSON 字符串 */
    private String tags;
    /** 封面图 URL */
    private String coverImageUrl;
    /** 备注 */
    private String notes;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
