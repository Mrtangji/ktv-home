package com.homektv.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查端点（P0.2 验收）。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        // service 标识供 TV 端局域网扫描确认「这是点歌服务端」而非其它 http 服务
        return Map.of("status", "UP", "service", "home-ktv");
    }
}
