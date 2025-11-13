package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 品牌字典实体。
 */
@Data
public class DictBrand {
    /** 主键 ID */
    private Long id;
    /** 品牌名称 */
    private String name;
    /** 品牌别名 */
    private String alias;
    /** 首字母 */
    private String initial;
    /** 排序值 */
    private Integer sort;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
