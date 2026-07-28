package com.homektv.web;

/**
 * 业务异常，携带错误码（详设§11.2）。
 * 业务码：SONG_IN_QUEUE / FILE_MISSING / TV_OFFLINE / INVALID_ACTION
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final Object data;

    public ApiException(String code, String message) {
        this(code, message, null);
    }

    public ApiException(String code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public String getCode() { return code; }
    public Object getData() { return data; }
}
