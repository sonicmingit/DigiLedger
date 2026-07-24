package com.digiledger.backend.integration.mtphotos;

/** 前端展示所需的最小图库文件信息；缩略图始终经由 DigiLedger 后端代理。 */
public record MtPhotosSearchItem(
        Long id,
        String fileName,
        String capturedAt,
        String fileType,
        String thumbnailUrl
) {
}
