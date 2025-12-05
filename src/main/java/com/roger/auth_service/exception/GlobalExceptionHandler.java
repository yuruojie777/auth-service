package com.roger.auth_service.exception;

import com.roger.auth_service.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 方法不支持，例如用 GET 调 /login（只支持 POST）
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("method", ex.getMethod());

        if (ex.getSupportedHttpMethods() != null) {
            details.put(
                    "supportedMethods",
                    ex.getSupportedHttpMethods()
                            .stream()
                            .map(HttpMethod::name) // 转成 "GET", "POST" 这样的字符串
                            .toList()
            );
        }

        return ResponseEntity
                .status(405)
                .body(ApiResponse.error(
                        "METHOD_NOT_ALLOWED",
                        "HTTP method not supported for this endpoint",
                        details
                ));
    }

    // 2. 请求体无法解析（JSON 错了、body 为空但用了 @RequestBody）
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Bad request body: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("BAD_REQUEST_BODY",
                        "Request body is missing or malformed JSON",
                        null));
    }

    // 3. @Valid 校验失败（DTO 上的 @NotBlank, @Email 等）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR",
                        "Request validation failed",
                        fieldErrors));
    }

    // 4. 例如 @RequestParam / @PathVariable 校验失败
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("CONSTRAINT_VIOLATION",
                        "Request parameter validation failed",
                        ex.getMessage()));
    }

    // 5. 业务异常：登录失败（用户/密码错误）
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Invalid credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(401)
                .body(ApiResponse.error("INVALID_CREDENTIALS",
                        ex.getMessage(),
                        null));
    }

    // 6. 业务异常：项目不存在
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectNotFound(ProjectNotFoundException ex) {
        log.warn("Project not found: {}", ex.getMessage());
        return ResponseEntity
                .status(404)
                .body(ApiResponse.error("PROJECT_NOT_FOUND",
                        ex.getMessage(),
                        null));
    }

    // 7. 业务异常：用户对项目无权限
    @ExceptionHandler(ProjectAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectAccessDenied(ProjectAccessDeniedException ex) {
        log.warn("Project access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(403)
                .body(ApiResponse.error("PROJECT_ACCESS_DENIED",
                        ex.getMessage(),
                        null));
    }

    // 8. 业务异常：邮箱已存在
    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailUsed(EmailAlreadyUsedException ex) {
        log.warn("Email already used: {}", ex.getMessage());
        return ResponseEntity
                .status(409)
                .body(ApiResponse.error("EMAIL_ALREADY_USED",
                        ex.getMessage(),
                        null));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        log.warn("Invalid refresh token: {}", ex.getMessage());
        return ResponseEntity
                .status(401)
                .body(ApiResponse.error(
                        "INVALID_REFRESH_TOKEN",
                        ex.getMessage(),
                        null
                ));
    }

    // 9. 最后的兜底：真正未预料到的异常，才是 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex) {
        // 这里确实是我们没预料到的错误，需要重点关注
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(500)
                .body(ApiResponse.error("INTERNAL_ERROR",
                        "Unexpected server error",
                        null));
    }
}
