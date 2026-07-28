package com.homektv.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话注册与事件广播（P1.14，详设§4.1/§4.2）。
 * 线程安全：会话存于 ConcurrentHashMap，发送时对单会话加锁（WebSocketSession 非并发安全）。
 */
@Component
public class WsBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(WsBroadcaster.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public WsBroadcaster(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    // 记录每个会话的 client_type（tv/h5），用于 TV 在线检测（P2.13）
    private final Map<String, String> sessionTypes = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
        Object type = session.getAttributes().get("client_type");
        if (type != null) sessionTypes.put(session.getId(), type.toString());
    }

    /** 注销会话，返回其 client_type（tv/h5，可能为 null），供离线清理等逻辑判断。 */
    public String unregister(WebSocketSession session) {
        sessions.remove(session.getId());
        return sessionTypes.remove(session.getId());
    }

    public int sessionCount() {
        return sessions.size();
    }

    /** TV 是否在线（详设§4.5：点了歌但 TV 不在线时 H5 显示横幅） */
    public boolean isTvOnline() {
        return sessionTypes.containsValue("tv");
    }

    /** 已连接的 H5 手机数 */
    public long h5Count() {
        return sessionTypes.values().stream().filter("h5"::equals).count();
    }

    /** 向单个会话发送事件（如连接后的 sync_full） */
    public void sendTo(WebSocketSession session, WsEvent event) {
        send(session, serialize(event));
    }

    /** 向所有在线会话广播事件 */
    public void broadcast(WsEvent event) {
        String json = serialize(event);
        for (WebSocketSession s : sessions.values()) {
            send(s, json);
        }
    }

    private void send(WebSocketSession session, String json) {
        if (json == null || !session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.debug("发送失败，移除会话 {}: {}", session.getId(), e.getMessage());
            unregister(session);
        }
    }

    private String serialize(WsEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            log.warn("事件序列化失败: {}", e.getMessage());
            return null;
        }
    }
}
