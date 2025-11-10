package com.digiledger.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, T data, String msg) {

    private static final int SUCCESS_CODE = 200;

    /** 成功返回 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, data, "OK");
    }

    /** 成功无数据返回 */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(SUCCESS_CODE, null, "OK");
    }

    /** 通过错误码快速构建失败返回 */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), null, errorCode.getDefaultMessage());
    }

    /** 自定义失败消息 */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), null, message);
    }

    /** 兼容直接输入状态码 */
    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }
}
