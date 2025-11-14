package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件实体，对应统一附件表。
 */
@Data
public class FileAttachment {
    /** 主键 ID */
    private Long id;
    /** 业务类型 */
    private String bizType;
    /** 业务主键 ID */
    private Long bizId;
    /** 对象存储相对路径 */
    private String objectKey;
    /** 原始文件名 */
    private String fileName;
    /** 文件类型 */
    private String fileType;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 扩展信息（JSON） */
    private String extra;
    /** 是否已删除 */
    private Boolean deleted;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
