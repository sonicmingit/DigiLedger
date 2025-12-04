package com.digiledger.backend.model.dto.attachment;

/**
 * 未使用附件的返回模型，包含对象键与可访问的完整URL。
 */
public record UnusedAttachmentResponse(
        String objectKey,
        String url
) {
}
