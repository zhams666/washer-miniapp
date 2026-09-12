package com.washer.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldError() != null
            ? ex.getBindingResult().getFieldError().getDefaultMessage()
            : "请求参数不合法";
        logRequestFailure(request, ex, message, false);
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ApiResponse<Void> handleDuplicateKeyException(DuplicateKeyException ex, HttpServletRequest request) {
        String message = "唯一键冲突，请检查编号、手机号、单号等字段是否重复";
        logRequestFailure(request, ex, message, true);
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        String message = safeMessage(ex.getMessage(), "请求参数不合法");
        logRequestFailure(request, ex, message, false);
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex, HttpServletRequest request) {
        String message = safeMessage(ex.getMessage(), "系统异常");
        logRequestFailure(request, ex, message, true);
        return ApiResponse.fail(message);
    }

    private void logRequestFailure(HttpServletRequest request, Exception exception, String message, boolean includeStackTrace) {
        String traceId = request != null ? safeTraceId(request.getHeader("X-Washer-Trace-Id")) : "missing";
        String method = request != null ? request.getMethod() : "unknown";
        String path = request != null ? request.getRequestURI() : "unknown";
        if (includeStackTrace) {
            LOGGER.error(
                "api_request_failed traceId={}, method={}, path={}, exception={}, message={}",
                traceId,
                method,
                path,
                exception.getClass().getSimpleName(),
                message,
                exception
            );
            return;
        }
        LOGGER.warn(
            "api_request_failed traceId={}, method={}, path={}, exception={}, message={}",
            traceId,
            method,
            path,
            exception.getClass().getSimpleName(),
            message
        );
    }

    private String safeTraceId(String value) {
        if (value == null || value.isBlank()) {
            return "missing";
        }
        String normalized = value.replaceAll("[^A-Za-z0-9_-]", "");
        return normalized.isBlank() ? "invalid" : normalized.substring(0, Math.min(normalized.length(), 80));
    }

    private String safeMessage(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.replaceAll("[\\r\\n]+", " ");
        return normalized.substring(0, Math.min(normalized.length(), 500));
    }
}
