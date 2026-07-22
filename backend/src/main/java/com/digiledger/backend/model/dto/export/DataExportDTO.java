package com.digiledger.backend.model.dto.export;

/** 导出内容仍置于统一 ApiResponse.data，客户端可依据 contentType 创建下载文件。 */
public record DataExportDTO(String format, String filename, String contentType, Object content) { }
