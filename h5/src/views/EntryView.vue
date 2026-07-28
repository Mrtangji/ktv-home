<template>
  <div class="entry">
    <div class="logo">🎤</div>
    <div class="title">家庭KTV</div>
    <div class="room">房间：客厅</div>

    <div class="field">
      <label>你的昵称（点歌时显示）</label>
      <div class="input-wrap">
        <input v-model="nickname" maxlength="12" placeholder="输入昵称" />
      </div>
      <div class="hint">已为你随机生成，可修改；本机记忆，下次免填</div>
    </div>

    <button class="btn enter-btn" @click="enter">进入点歌</button>
    <div class="foot">仅限家庭局域网使用 · 无需注册</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { usePlayerStore } from '../stores/player'
import api from '../api/client'

const router = useRouter()
const user = useUserStore()
const player = usePlayerStore()

// 默认回填已存昵称或随机建议值（详设 H5-01）
const nickname = ref(user.suggestNickname())

async function enter() {
  user.register(nickname.value)
  // 同步到服务端并取回去重后的最终昵称（P2.13 昵称冲突显序号）
  try {
    const res = await api.registerUser(user.clientToken, user.nickname)
    if (res?.nickname) user.setNickname(res.nickname)
  } catch { /* 离线也可继续，稍后重连同步 */ }
  player.connect()
  router.replace({ name: 'home' })
}
</script>

<style scoped>
.entry {
  min-height: 100vh;
  display: flex; flex-direction: column; align-items: center;
  padding: 0 32px calc(28px + var(--safe-bottom));
  background: radial-gradient(ellipse 400px 300px at 50% 30%, rgba(240,199,66,.06), transparent),
              linear-gradient(175deg, rgba(20,26,42,.9), var(--bg));
}
.logo {
  width: 88px; height: 88px; border-radius: 24px; margin-top: 22vh;
  background: linear-gradient(135deg, var(--gold), #dba70e);
  display: flex; align-items: center; justify-content: center; font-size: 40px;
  box-shadow: 0 12px 40px rgba(240,199,66,.25);
}
.title { font-size: 28px; font-weight: 800; letter-spacing: -.5px; margin-top: 22px; }
.room {
  font-size: 13px; color: var(--dim); margin-top: 12px;
  background: var(--panel2); border: 1px solid var(--glass-border);
  border-radius: 999px; padding: 5px 13px;
}
.field { width: 100%; margin-top: 42px; }
.field label { font-size: 13px; color: var(--dim); display: block; margin-bottom: 10px; }
.input-wrap {
  background: var(--panel2); border: 1px solid rgba(240,199,66,.2);
  border-radius: 12px; padding: 12px 14px;
}
.input-wrap input {
  width: 100%; background: none; border: none; outline: none;
  color: var(--text); font-size: 15px;
}
.hint { font-size: 11px; color: var(--dim2); margin-top: 10px; }
.enter-btn { width: 100%; padding: 16px; font-size: 17px; border-radius: 14px; margin-top: 28px; }
.foot { margin-top: auto; font-size: 11px; color: var(--dim2); }
</style>
