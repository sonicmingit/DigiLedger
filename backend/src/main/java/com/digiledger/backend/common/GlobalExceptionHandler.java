package com.digiledger.backend.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，统一转换为 {code,data,msg}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException ex) {
        log.warn("业务异常：{}", ex.getMessage(), ex);
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + "：" + error.getDefaultMessage())
                .orElse(ex.getMessage());
        log.warn("参数校验失败：{}", message, ex);
        return ApiResponse.failure(ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("参数约束异常：{}", ex.getMessage(), ex);
        return ApiResponse.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("非法参数：{}", ex.getMessage(), ex);
        return ApiResponse.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleDefault(Exception ex) {
        log.error("系统未捕获异常", ex);
        return ApiResponse.failure(ErrorCode.INTERNAL_ERROR, ex.getMessage());
    }
}
