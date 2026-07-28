import { defineStore } from 'pinia'
import { KtvSocket } from '../api/ws'
import { useUserStore } from './user'

// 播放器/队列全局状态（详设§4.1：服务端为唯一事实源，此处镜像广播状态）
export const usePlayerStore = defineStore('player', {
  state: () => ({
    connected: false,
    socket: null,
    nowPlaying: null,      // {queueId, song, orderedByNick}
    state: 'idle',         // idle / playing / paused
    positionMs: 0,
    volume: 60,
    muted: false,
    vocalMode: 'accompaniment', // original / accompaniment
    queue: [],             // [{queueId, song, orderedBy, orderedByNick, status}]
    tvOnline: true,        // TV 是否在线（P2.13）
    connectedPhones: 0,
    lastEffect: null
  }),
  getters: {
    queueCount: (s) => s.queue.length,
    isPlaying: (s) => s.state === 'playing'
  },
  actions: {
    // 建立 WebSocket 连接（进入 App 后调用一次）
    connect() {
      if (this.socket) return
      this.socket = new KtvSocket({
        onEvent: (type, payload) => this.handleEvent(type, payload),
        onStatus: (ok) => { this.connected = ok }
      })
      this.socket.connect()
    },

    disconnect() {
      if (this.socket) { this.socket.close(); this.socket = null }
    },

    // TV 端才上报 progress；H5 仅接收。此处保留发送能力供调试
    handleEvent(type, payload) {
      switch (type) {
        case 'sync_full':
          this.applySnapshot(payload)
          break
        case 'queue_updated':
        case 'now_playing':
        case 'player_state':
        case 'playback_restarted':
        case 'volume_changed':
        case 'vocal_changed':
          this.applySnapshot(payload)
          break
        case 'progress':
          if (payload && typeof payload.position_ms === 'number') {
            this.positionMs = payload.position_ms
          }
          break
        case 'effect_play':
          this.lastEffect = payload?.effect_id ?? null
          break
        default:
          break
      }
    },

    // 应用服务端快照（QueueSnapshot 结构）
    applySnapshot(snap) {
      if (!snap) return
      // now_playing / player_state 等事件 payload 也是完整 snapshot
      const playing = snap.playing ?? null
      this.nowPlaying = playing
        ? { queueId: playing.queueId, song: playing.song, orderedByNick: playing.orderedByNick }
        : null
      this.state = snap.state ?? 'idle'
      this.volume = snap.volume ?? this.volume
      this.muted = snap.muted ?? false
      this.vocalMode = snap.vocalMode ?? this.vocalMode
      this.queue = snap.list ?? []
      this.tvOnline = snap.tvOnline ?? true
      this.connectedPhones = snap.connectedPhones ?? 0
    }
  }
})
