package org.example.nongye.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * REST 接口全局异常处理：只拦截 controller.api 包下的 @RestController，
 * 返回 JSON（与页面端 GlobalExceptionHandler 互不干扰）
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "org.example.nongye.controller.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResult<Void>> handleApi(ApiException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatus());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        log.warn("API 业务异常：{}", e.getMessage());
        return ResponseEntity.status(status).body(ApiResult.error(e.getStatus(), e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(ApiResult.error(400, "文件过大：单个不超过 10MB，一次总计不超过 50MB"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResult<Void>> handleRuntime(RuntimeException e) {
        String msg = e.getMessage() == null ? "操作失败" : e.getMessage();
        // 兼容直接抛 RuntimeException("未登录"/"无权限") 的旧写法
        if (msg.contains("未登录")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResult.error(401, "请先登录"));
        }
        if (msg.contains("无权限") || msg.contains("权限")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResult.error(403, msg));
        }
        log.warn("API 业务异常：{}", msg);
        return ResponseEntity.badRequest().body(ApiResult.error(400, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleAll(Exception e) {
        log.error("API 系统异常", e);
        return ResponseEntity.internalServerError().body(ApiResult.error(500, "系统开小差了，请稍后重试"));
    }
}
