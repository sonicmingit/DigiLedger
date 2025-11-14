package com.digiledger.backend.model.dto.attachment;

import java.time.LocalDateTime;

/**
 * 附件响应模型。
 */
public record AttachmentResponse(
        Long id,
        String bizType,
        Long bizId,
        String objectKey,
        String url,
        String fileName,
        String fileType,
        Long fileSize,
        String extra,
        LocalDateTime createdAt
) {
}
