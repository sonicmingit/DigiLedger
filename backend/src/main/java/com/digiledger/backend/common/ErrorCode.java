package com.digiledger.backend.common;

/**
 * 错误码定义
 */
public enum ErrorCode {
    SUCCESS(200, "成功"),
    VALIDATION_ERROR(400, "参数校验失败"),
    ASSET_NOT_FOUND(404, "资产不存在"),
    PURCHASE_NOT_FOUND(404, "采购记录不存在"),
    SALE_NOT_FOUND(404, "出售记录不存在"),
    WISHLIST_NOT_FOUND(404, "心愿单不存在"),
    ATTACHMENT_NOT_FOUND(404, "附件不存在"),
    ASSET_DELETE_CONFLICT(409, "存在关联记录，资产无法删除"),
    SALE_STATUS_CONFLICT(409, "当前状态无法进行售卖"),
    DATE_RANGE_CONFLICT(409, "日期范围冲突"),
    RATE_LIMIT_EXCEEDED(429, "操作频率过高，请稍后再试"),
    FILE_TOO_LARGE(413, "文件过大"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的文件类型"),
    INTERNAL_ERROR(500, "内部服务错误");

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
