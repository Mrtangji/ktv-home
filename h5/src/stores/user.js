import { defineStore } from 'pinia'

const TOKEN_KEY = 'ktv_client_token'
const NICK_KEY = 'ktv_nickname'

function genToken() {
  // 优先用原生 UUID，降级到随机串
  if (globalThis.crypto?.randomUUID) return crypto.randomUUID()
  return 'tok-' + Math.random().toString(36).slice(2) + Date.now().toString(36)
}

function genNickname() {
  return '家人' + Math.floor(1000 + Math.random() * 9000)
}

// 点歌人身份（详设 H5-01）：client_token + nickname，写 localStorage，非首次免填
export const useUserStore = defineStore('user', {
  state: () => ({
    clientToken: localStorage.getItem(TOKEN_KEY) || '',
    nickname: localStorage.getItem(NICK_KEY) || ''
  }),
  getters: {
    // 是否已完成初次进入（有 token 且有昵称）
    isRegistered: (s) => !!s.clientToken && !!s.nickname
  },
  actions: {
    // 确保有 token（首次生成并持久化）
    ensureToken() {
      if (!this.clientToken) {
        this.clientToken = genToken()
        localStorage.setItem(TOKEN_KEY, this.clientToken)
      }
      return this.clientToken
    },
    // 建议昵称（未设置时给随机默认值）
    suggestNickname() {
      return this.nickname || genNickname()
    },
    // 保存昵称（H5-01 提交时调用）
    register(nickname) {
      this.ensureToken()
      this.nickname = (nickname || '').trim() || genNickname()
      localStorage.setItem(NICK_KEY, this.nickname)
    },

    // 用服务端去重后的最终昵称覆盖（P2.13）
    setNickname(nickname) {
      if (!nickname) return
      this.nickname = nickname
      localStorage.setItem(NICK_KEY, nickname)
    }
  }
})
