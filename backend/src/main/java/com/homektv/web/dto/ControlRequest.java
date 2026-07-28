package com.homektv.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * 统一控制指令请求（详设§4.3）。
 * action: order/shuffle/top/cancel/play/pause/next/restart/set_volume/mute/set_vocal/effect
 */
public record ControlRequest(
        String action,
        Map<String, Object> params,
        @JsonProperty("client_token") String clientToken
) {
    public Map<String, Object> safeParams() {
        return params == null ? Map.of() : params;
    }

    public Long longParam(String key) {
        Object v = safeParams().get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) return Long.parseLong(s);
        return null;
    }

    public int intParam(String key, int def) {
        Object v = safeParams().get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && !s.isBlank()) return Integer.parseInt(s);
        return def;
    }

    public boolean boolParam(String key) {
        Object v = safeParams().get(key);
        if (v instanceof Boolean b) return b;
        return "true".equals(String.valueOf(v));
    }

    public String strParam(String key) {
        Object v = safeParams().get(key);
        return v == null ? null : String.valueOf(v);
    }
}
