package com.homektv.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homektv.queue.PlaybackService;
import com.homektv.queue.SnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * KTV WebSocket 处理器（P1.13/P1.15/P1.16，详设§4.1/§4.2）。
 * - 连接建立即推送 sync_full 全量快照
 * - 接收 TV 上行 progress → 转发广播给 H5（歌词/进度同步）
 * - 接收 ping → 回 pong（心跳）
 */
@Component
public class KtvWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(KtvWebSocketHandler.class);

    private final WsBroadcaster broadcaster;
    private final SnapshotService snapshotService;
    private final PlaybackService playbackService;
    private final TvOfflineWatcher tvOfflineWatcher;
    private final ObjectMapper mapper;

    public KtvWebSocketHandler(WsBroadcaster broadcaster, SnapshotService snapshotService,
                               PlaybackService playbackService, TvOfflineWatcher tvOfflineWatcher,
                               ObjectMapper mapper) {
        this.broadcaster = broadcaster;
        this.snapshotService = snapshotService;
        this.playbackService = playbackService;
        this.tvOfflineWatcher = tvOfflineWatcher;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        broadcaster.register(session);
        if (isTv(session)) {
            tvOfflineWatcher.onTvConnected();
        }
        // 连接/重连即推全量快照（详设§4.1）
        broadcaster.sendTo(session, WsEvent.of(WsEvent.SYNC_FULL, snapshotService.snapshot()));
        log.debug("WS 连接建立: {}，当前在线 {}", session.getId(), broadcaster.sessionCount());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = mapper.readTree(message.getPayload());
        String type = node.path("type").asText("");

        switch (type) {
            case "ping" -> broadcaster.sendTo(session, WsEvent.of("pong", null));
            case "progress" -> {
                // TV 上行播放进度 → 转发给所有端（详设§4.2 progress）
                long positionMs = node.path("payload").path("position_ms").asLong(0);
                broadcaster.broadcast(WsEvent.of(WsEvent.PROGRESS,
                        java.util.Map.of("position_ms", positionMs)));
            }
            case "finished" -> {
                // TV 上报当前曲目播放完成 → 推进队列并广播
                playbackService.onFinished();
                broadcaster.broadcast(WsEvent.of(WsEvent.NOW_PLAYING, snapshotService.snapshot()));
            }
            case "play_error" -> {
                // TV 无法读取当前媒体时按异常切歌，避免队列卡死；将原因同步给手机端。
                String reason = node.path("payload").path("message").asText("媒体读取失败");
                Long fileId = node.path("payload").path("file_id").isNumber()
                        ? node.path("payload").path("file_id").asLong() : null;
                playbackService.onPlayError(fileId);
                broadcaster.broadcast(WsEvent.of(WsEvent.TOAST,
                        java.util.Map.of("text", "当前歌曲播放失败，已自动切换下一首：" + reason)));
                broadcaster.broadcast(WsEvent.of(WsEvent.NOW_PLAYING, snapshotService.snapshot()));
            }
            default -> log.debug("未知 WS 消息类型: {}", type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        notifyOfflineIfTv(broadcaster.unregister(session));
        log.debug("WS 连接关闭: {}，剩余在线 {}", session.getId(), broadcaster.sessionCount());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("WS 传输错误 {}: {}", session.getId(), exception.getMessage());
        notifyOfflineIfTv(broadcaster.unregister(session));
    }

    private boolean isTv(WebSocketSession session) {
        Object type = session.getAttributes().get("client_type");
        return "tv".equals(type == null ? null : type.toString());
    }

    private void notifyOfflineIfTv(String clientType) {
        if ("tv".equals(clientType)) {
            tvOfflineWatcher.onTvDisconnected();
        }
    }
}
