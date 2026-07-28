<template>
  <div v-if="player.nowPlaying" class="now" @click="$router.push({ name: 'lyric' })">
    <div class="cover"></div>
    <div class="grow">
      <div class="title">
        {{ player.nowPlaying.song?.title }}
        <span class="artist">· {{ player.nowPlaying.song?.artist }}</span>
      </div>
      <div class="bar"><i :style="{ width: progressPct + '%' }"></i></div>
    </div>
    <span class="chip">{{ stateText }}</span>
  </div>
  <div v-else class="now empty">
    <div class="grow hint">还没人点歌，来点第一首吧</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { usePlayerStore } from '../stores/player'

const player = usePlayerStore()

const progressPct = computed(() => {
  const dur = player.nowPlaying?.song?.durationMs || 0
  if (!dur) return 0
  return Math.min(100, Math.round((player.positionMs / dur) * 100))
})

const stateText = computed(() => ({
  playing: '演唱中', paused: '已暂停', idle: ''
}[player.state] || ''))
</script>

<style scoped>
.now {
  background: var(--panel2); border: 1px solid rgba(240,199,66,.15); border-radius: var(--radius);
  padding: 14px; display: flex; gap: 12px; align-items: center;
  box-shadow: 0 4px 20px rgba(0,0,0,.2);
}
.now.empty { border-color: var(--glass-border); justify-content: center; }
.hint { color: var(--dim2); font-size: 13px; text-align: center; }
.cover {
  width: 52px; height: 52px; border-radius: 10px; flex: none;
  background: radial-gradient(circle at 30% 26%, rgba(240,199,66,.12), transparent 42%),
              linear-gradient(145deg, rgba(40,46,66,.85), rgba(20,24,36,.92));
  border: 1px solid var(--glass-border);
}
.title { font-size: 14px; font-weight: 700; }
.artist { color: var(--dim); font-weight: 400; font-size: 12px; }
.bar { height: 4px; background: rgba(255,255,255,.06); border-radius: 3px; margin-top: 8px; overflow: hidden; }
.bar i { display: block; height: 100%; background: linear-gradient(90deg, var(--gold), var(--gold2)); transition: width .5s linear; }
.chip {
  flex: none; color: var(--gold); border: 1px solid rgba(240,199,66,.25); border-radius: 999px;
  padding: 4px 10px; font-size: 11px; background: var(--panel2);
}
</style>
