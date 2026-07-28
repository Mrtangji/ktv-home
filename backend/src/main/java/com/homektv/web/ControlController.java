package com.homektv.web;

import com.homektv.domain.QueueItem;
import com.homektv.queue.PlaybackService;
import com.homektv.queue.QueueService;
import com.homektv.queue.SnapshotService;
import com.homektv.queue.UserService;
import com.homektv.repo.QueueItemRepository;
import com.homektv.web.dto.ControlRequest;
import com.homektv.web.dto.QueueSnapshot;
import com.homektv.ws.WsBroadcaster;
import com.homektv.ws.WsEvent;
import org.springframework.web.bind.annotation.*;

/**
 * 统一控制入口 + 队列快照（P1.12，详设§4.3/§11.1）。
 * 变更后返回最新快照；WebSocket 广播接入见 P1.14。
 */
@RestController
@RequestMapping("/api")
public class ControlController {

    private final QueueService queueService;
    private final PlaybackService playbackService;
    private final SnapshotService snapshotService;
    private final UserService userService;
    private final QueueItemRepository queueRepo;
    private final WsBroadcaster broadcaster;

    public ControlController(QueueService queueService, PlaybackService playbackService,
                             SnapshotService snapshotService, UserService userService,
                             QueueItemRepository queueRepo, WsBroadcaster broadcaster) {
        this.queueService = queueService;
        this.playbackService = playbackService;
        this.snapshotService = snapshotService;
        this.userService = userService;
        this.queueRepo = queueRepo;
        this.broadcaster = broadcaster;
    }

    /** 当前队列 + 播放状态快照 */
    @GetMapping("/queue")
    public QueueSnapshot queue() {
        return snapshotService.snapshot();
    }

    /** 统一控制指令 */
    @PostMapping("/control")
    public QueueSnapshot control(@RequestBody ControlRequest req) {
        String action = req.action() == null ? "" : req.action();
        Long userId = userService.resolveUserId(req.clientToken());

        switch (action) {
            case "order" -> {
                queueService.order(req.longParam("song_id"), userId, req.boolParam("force"));
                boolean started = playbackService.startIfIdle();
                broadcast(WsEvent.QUEUE_UPDATED);
                if (started) {
                    broadcast(WsEvent.NOW_PLAYING);
                }
            }
            case "shuffle" -> {
                queueService.shuffleWaiting();
                broadcast(WsEvent.QUEUE_UPDATED);
            }
            case "top" -> {
                queueService.top(req.longParam("queue_id"));
                broadcast(WsEvent.QUEUE_UPDATED);
            }
            case "cancel" -> {
                cancelWithPermission(req.longParam("queue_id"), userId);
                broadcast(WsEvent.QUEUE_UPDATED);
            }
            case "play", "pause" -> {
                dispatchPlayback(action);
                broadcast(WsEvent.PLAYER_STATE);
            }
            case "restart" -> {
                dispatchPlayback(action);
                broadcast(WsEvent.PLAYBACK_RESTARTED);
            }
            case "next", "finished" -> {
                dispatchPlayback(action);
                broadcast(WsEvent.NOW_PLAYING);
            }
            case "set_volume" -> {
                playbackService.setVolume(req.intParam("volume", 60));
                broadcast(WsEvent.VOLUME_CHANGED);
            }
            case "mute" -> {
                playbackService.setMuted(req.boolParam("muted"));
                broadcast(WsEvent.VOLUME_CHANGED);
            }
            case "set_vocal" -> {
                playbackService.setVocalMode(req.strParam("mode"));
                broadcast(WsEvent.VOCAL_CHANGED);
            }
            case "swap_vocal_tracks" -> {
                playbackService.swapVocalTracks();
                broadcast(WsEvent.VOCAL_CHANGED);
            }
            case "effect" -> broadcaster.broadcast(
                    WsEvent.of(WsEvent.EFFECT_PLAY, java.util.Map.of("effect_id", req.strParam("effect_id"))));
            default -> throw new ApiException("INVALID_ACTION", "未知指令：" + action);
        }
        return snapshotService.snapshot();
    }

    private void dispatchPlayback(String action) {
        switch (action) {
            case "play" -> playbackService.play();
            case "pause" -> playbackService.pause();
            case "restart" -> playbackService.restart();
            case "next" -> playbackService.next();
            case "finished" -> playbackService.onFinished();
        }
    }

    /** 广播当前快照到所有端（详设§4.1：客户端以广播为准） */
    private void broadcast(String eventType) {
        broadcaster.broadcast(WsEvent.of(eventType, snapshotService.snapshot()));
    }

    /** 删歌权限：本人点的才能删（详设§4.4.3；TV/后台删任意由其它入口处理） */
    private void cancelWithPermission(Long queueId, Long userId) {
        QueueItem item = queueRepo.findById(queueId)
                .orElseThrow(() -> new ApiException("QUEUE_ITEM_NOT_FOUND", "队列项不存在"));
        if (item.getOrderedBy() != null && !item.getOrderedBy().equals(userId)) {
            throw new ApiException("FORBIDDEN", "只能删除自己点的歌曲");
        }
        queueService.cancel(queueId);
    }
}
