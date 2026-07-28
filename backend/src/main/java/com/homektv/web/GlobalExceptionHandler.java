package com.homektv.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一错误响应 {code, message}（详设§11.2）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", e.getCode());
        body.put("message", e.getMessage());
        if (e.getData() != null) body.put("data", e.getData());
        // SONG_IN_QUEUE / TV_OFFLINE 属提示性，用 409/200 语义；这里统一 400 + code 区分
        HttpStatus status = "TV_OFFLINE".equals(e.getCode()) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegal(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", "INVALID_ARGUMENT",
                "message", e.getMessage() == null ? "参数错误" : e.getMessage()));
    }
}
