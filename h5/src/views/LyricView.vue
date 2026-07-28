<template>
  <div class="page">
    <!-- 顶部迷你曲目条 -->
    <div class="topbar">
      <span class="down" @click="$router.back()">⌄</span>
      <div class="grow center">
        <div class="t">{{ song?.title || '暂无播放' }}
          <span class="a" v-if="song">· {{ song.artist }}</span></div>
        <div class="bar"><i :style="{ width: progressPct + '%' }"></i></div>
      </div>
      <div class="cover"></div>
    </div>

    <!-- 歌词滚动区 -->
    <div class="lyric grow">
      <div v-if="!lines.length" class="nolyric">
        {{ song ? '这首歌暂无歌词' : '还没有歌曲播放' }}
      </div>
      <div v-else class="lines" :style="{ transform: `translateY(${offset}px)` }">
        <div v-for="(l, i) in lines" :key="i" class="ln" :class="{ cur: i === curLine }">
          <template v-if="i === curLine && l.words?.length">
            <span v-for="(word, wi) in l.words" :key="wi" :class="{ sung: word.time <= player.positionMs }">{{ word.text }}</span>
          </template>
          <template v-else>{{ l.text }}</template>
        </div>
      </div>
    </div>

    <!-- 底部迷你控制 -->
    <div class="mini">
      <button class="c" @click="restart">🔁</button>
      <button class="c big" @click="togglePlay">{{ player.isPlaying ? '⏸' : '▶' }}</button>
      <button class="c" @click="next">⏭</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { usePlayerStore } from '../stores/player'
import { useUserStore } from '../stores/user'
import { makeControls } from '../api/client'
import { parseLrc } from '../composables/lrc'
import { confirmDialog } from '../composables/useDialog'

const player = usePlayerStore()
const user = useUserStore()
const controls = makeControls(user.clientToken)

const song = computed(() => player.nowPlaying?.song)
const lines = ref([])       // [{time, text}]
const LINE_H = 44

const progressPct = computed(() => {
  const dur = song.value?.durationMs || 0
  return dur ? Math.min(100, Math.round((player.positionMs / dur) * 100)) : 0
})

// 当前高亮行：最后一个 time <= position
const curLine = computed(() => {
  const pos = player.positionMs
  let idx = 0
  for (let i = 0; i < lines.value.length; i++) {
    if (lines.value[i].time <= pos) idx = i; else break
  }
  return idx
})

// 让当前行居中：容器约 5 行可视，居中偏移
const offset = computed(() => -(curLine.value * LINE_H))

async function loadLyric(id) {
  lines.value = []
  if (!id || !song.value || song.value.lyricType === 'none') return
  try {
    const res = await fetch('/api/lyric/' + id)
    if (!res.ok) return
    lines.value = parseLrc(await res.text())
  } catch { /* ignore */ }
}

watch(() => player.nowPlaying?.queueId, () => loadLyric(song.value?.id))
onMounted(() => loadLyric(song.value?.id))

async function togglePlay() { player.isPlaying ? await controls.pause() : await controls.play() }
async function restart() { await controls.restart() }
async function next() {
  if (await confirmDialog(`将切掉《${song.value?.title || ''}》。`, { title: '确认切歌', tone: 'warning' })) await controls.next()
}
</script>

<style scoped>
.page {
  min-height: 100vh; display: flex; flex-direction: column;
  background: radial-gradient(ellipse 350px 350px at 50% 45%, rgba(240,199,66,.04), transparent),
              linear-gradient(175deg, rgba(20,26,42,.9), var(--bg));
}
.topbar { display: flex; align-items: center; gap: 10px; padding: 16px; }
.down { font-size: 20px; color: var(--dim); }
.center { text-align: center; }
.center .t { font-size: 14px; font-weight: 700; }
.center .a { color: var(--dim); font-weight: 400; font-size: 11px; }
.bar { height: 3px; background: rgba(255,255,255,.06); border-radius: 3px; margin-top: 7px; overflow: hidden; }
.bar i { display: block; height: 100%; background: linear-gradient(90deg, var(--gold), var(--gold2)); transition: width .5s linear; }
.cover {
  width: 34px; height: 34px; border-radius: 8px; flex: none;
  background: linear-gradient(145deg, rgba(40,46,66,.85), rgba(20,24,36,.92)); border: 1px solid var(--glass-border);
}
.lyric { overflow: hidden; display: flex; flex-direction: column; justify-content: center; padding: 0 30px; text-align: center; }
.nolyric { color: var(--dim2); text-align: center; }
.lines { transition: transform .4s cubic-bezier(.4,0,.2,1); }
.ln { font-size: 15px; color: var(--dim2); padding: 10px 0; transition: var(--transition); height: 44px; }
.ln.cur { font-size: 20px; color: var(--gold); font-weight: 700; text-shadow: 0 0 20px rgba(240,199,66,.2); }
.ln.cur span { transition: color .12s linear; }
.ln.cur span.sung { color: var(--gold2); text-shadow: 0 0 16px rgba(240,199,66,.3); }
.mini { display: flex; justify-content: center; gap: 28px; padding: 20px 0 40px; }
.c { width: 44px; height: 44px; border-radius: 50%; background: var(--panel2);
  border: 1px solid var(--glass-border); font-size: 16px; }
.c.big { width: 52px; height: 52px; background: var(--gold-glow); border-color: rgba(240,199,66,.2); }
</style>
