package org.example.nongye.common;

/**
 * REST 接口业务异常：携带 HTTP 状态码，由 ApiExceptionHandler 统一转 JSON
 */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(String message) {
        this(400, message);
    }

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(401, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(403, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public int getStatus() {
        return status;
    }
}
