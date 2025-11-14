package com.digiledger.backend.common;

/**
 * 错误码枚举，对齐设计文档 5.8。
 */
public enum ErrorCode {
    SUCCESS(200, "成功"),
    VALIDATION_ERROR(400, "参数校验失败"),
    ASSET_NOT_FOUND(404, "资产不存在"),
    PURCHASE_NOT_FOUND(404, "购买记录不存在"),
    SALE_NOT_FOUND(404, "售出记录不存在"),
    WISHLIST_NOT_FOUND(404, "心愿单条目不存在"),
    ATTACHMENT_NOT_FOUND(404, "附件不存在"),
    ASSET_DELETE_CONFLICT(409, "资产存在关联记录，无法删除"),
    SALE_STATUS_CONFLICT(409, "资产状态不允许售出"),
    DATE_RANGE_CONFLICT(409, "日期范围不合法"),
    FILE_TOO_LARGE(413, "文件过大"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的文件类型"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
