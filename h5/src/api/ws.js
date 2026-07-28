// WebSocket 客户端（详设§4.1）：连接 /ws，处理广播事件，断线指数退避重连。
// 心跳：H5 每 25s 发 ping（微信后台挂起后靠重连恢复）。

const BACKOFF = [1000, 2000, 5000, 10000] // 指数退避，10s 封顶

export class KtvSocket {
  constructor({ onEvent, onStatus } = {}) {
    this.onEvent = onEvent || (() => {})
    this.onStatus = onStatus || (() => {})
    this.ws = null
    this.retry = 0
    this.pingTimer = null
    this.closed = false
  }

  connect() {
    this.closed = false
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    // 开发时 Vite 代理 /ws；生产同源
    const url = `${proto}://${location.host}/ws`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.retry = 0
      this.onStatus(true)
      this._startPing()
    }
    this.ws.onmessage = (e) => {
      let msg
      try { msg = JSON.parse(e.data) } catch { return }
      if (msg.type === 'pong') return
      this.onEvent(msg.type, msg.payload)
    }
    this.ws.onclose = () => {
      this._stopPing()
      this.onStatus(false)
      if (!this.closed) this._scheduleReconnect()
    }
    this.ws.onerror = () => { this.ws && this.ws.close() }
  }

  send(obj) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(obj))
    }
  }

  close() {
    this.closed = true
    this._stopPing()
    this.ws && this.ws.close()
  }

  _scheduleReconnect() {
    const delay = BACKOFF[Math.min(this.retry, BACKOFF.length - 1)]
    this.retry++
    setTimeout(() => { if (!this.closed) this.connect() }, delay)
  }

  _startPing() {
    this._stopPing()
    this.pingTimer = setInterval(() => this.send({ type: 'ping' }), 25000)
  }

  _stopPing() {
    if (this.pingTimer) { clearInterval(this.pingTimer); this.pingTimer = null }
  }
}
