package com.digiledger.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, T data, String msg) {

    private static final int SUCCESS_CODE = 200;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, data, "OK");
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(SUCCESS_CODE, null, "OK");
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return new ApiResponse<>(code, null, message);
    }
}
