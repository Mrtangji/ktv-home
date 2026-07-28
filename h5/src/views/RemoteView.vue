<template>
  <div class="page">
    <!-- 当前曲目卡 -->
    <section class="sec">
      <div class="now">
        <div class="cover"></div>
        <div class="grow">
          <div class="t">{{ song?.title || '暂无播放' }}</div>
          <div class="s" v-if="song">{{ song.artist }} · {{ player.nowPlaying?.orderedByNick || '' }} 点</div>
          <div class="bar"><i :style="{ width: progressPct + '%' }"></i></div>
        </div>
      </div>
    </section>

    <!-- 主控制排 -->
    <section class="sec ctlrow">
      <button class="ctl" @click="restart"><span>🔁</span><em>重唱</em></button>
      <button class="bigctl" @click="togglePlay">{{ player.isPlaying ? '⏸' : '▶' }}</button>
      <button class="ctl" @click="confirmNext"><span>⏭</span><em>切歌</em></button>
    </section>

    <!-- 音量 -->
    <section class="sec">
      <div class="row vol">
        <span>🔉</span>
        <input type="range" min="0" max="100" v-model.number="vol" @change="onVol" class="grow" />
        <span class="val">{{ vol }}</span>
      </div>
      <div class="note">电视音量（伴奏）</div>
    </section>

    <!-- 原/伴唱 -->
    <section class="sec">
      <div class="seg" :class="{ disabled: !canVocal }">
        <div :class="{ on: player.vocalMode === 'original' }" @click="setVocal('original')">原唱</div>
        <div :class="{ on: player.vocalMode === 'accompaniment' }" @click="setVocal('accompaniment')">伴唱</div>
      </div>
      <div class="note">{{ canVocal ? '当前为 KTV 版，支持音轨切换' : 'MV/音频版无伴唱音轨，此处禁用' }}</div>
      <button v-if="canVocal" class="track-fix" @click="swapVocalTracks">原唱和伴唱弄反了？纠正并记住</button>
    </section>

    <!-- 氛围音效（P3.1 完整；此处已可发送） -->
    <section class="sec">
      <div class="grid4">
        <div v-for="e in effects" :key="e.id" class="fx" @click="effect(e)">
          <div class="ic">{{ e.ic }}</div>{{ e.label }}
        </div>
      </div>
    </section>

    <div class="tip">💡 人声音量请在麦克风上调节</div>

    <TabBar active="remote" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { usePlayerStore } from '../stores/player'
import { useUserStore } from '../stores/user'
import { makeControls } from '../api/client'
import { useToast } from '../composables/useToast'
import { confirmDialog } from '../composables/useDialog'
import TabBar from '../components/TabBar.vue'

const player = usePlayerStore()
const user = useUserStore()
const { toast } = useToast()
const controls = makeControls(user.clientToken)

const song = computed(() => player.nowPlaying?.song)
const canVocal = computed(() => song.value?.hasVocalTrack === true)
const progressPct = computed(() => {
  const dur = song.value?.durationMs || 0
  return dur ? Math.min(100, Math.round((player.positionMs / dur) * 100)) : 0
})

const vol = ref(player.volume)
watch(() => player.volume, v => { vol.value = v })

const effects = [
  { id: 'clap', ic: '👏', label: '鼓掌' }, { id: 'cheer', ic: '🎉', label: '欢呼' },
  { id: 'boo', ic: '😜', label: '倒彩' }, { id: 'toast', ic: '🍻', label: '干杯' }
]

async function togglePlay() {
  try { player.isPlaying ? await controls.pause() : await controls.play() }
  catch (e) { toast(e.message || '操作失败') }
}
async function restart() {
  try { await controls.restart(); toast('重唱：从头播放') }
  catch (e) { toast(e.message || '操作失败') }
}
async function confirmNext() {
  if (!await confirmDialog(`将切掉《${song.value?.title || ''}》。`, { title: '确认切歌', tone: 'warning' })) return
  try { await controls.next(); toast('已切歌') }
  catch (e) { toast(e.message || '切歌失败') }
}
async function onVol() {
  try { await controls.setVolume(vol.value) }
  catch (e) { toast(e.message || '调音量失败') }
}
async function setVocal(mode) {
  if (!canVocal.value) { toast('该版本无伴唱音轨'); return }
  try { await controls.setVocal(mode); toast(mode === 'original' ? '已切原唱' : '已切伴唱') }
  catch (e) { toast(e.message || '切换失败') }
}
async function swapVocalTracks() {
  if (!canVocal.value) { toast('该歌曲没有双音轨'); return }
  if (!await confirmDialog(`将永久更正《${song.value?.title || ''}》的原唱和伴唱标记。`, { title: '纠正音轨标记' })) return
  try { await controls.swapVocalTracks(); toast('已纠正，本歌曲下次播放继续生效') }
  catch (e) { toast(e.message || '纠正失败') }
}
async function effect(e) {
  try { await controls.effect(e.id); toast('已发送：' + e.label) }
  catch (err) { toast(err.message || '发送失败') }
}
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 74px; display: flex; flex-direction: column; }
.sec { padding: 0 16px; margin-top: 20px; }
.sec:first-child { margin-top: 12px; }
.now {
  background: var(--panel2); border: 1px solid rgba(240,199,66,.1); border-radius: var(--radius);
  padding: 16px; display: flex; gap: 12px; align-items: center;
}
.cover {
  width: 64px; height: 64px; border-radius: 12px; flex: none;
  background: radial-gradient(circle at 30% 26%, rgba(240,199,66,.12), transparent 42%),
              linear-gradient(145deg, rgba(40,46,66,.85), rgba(20,24,36,.92));
  border: 1px solid var(--glass-border);
}
.now .t { font-size: 16px; font-weight: 800; }
.now .s { font-size: 12px; color: var(--dim); margin-top: 3px; }
.bar { height: 4px; background: rgba(255,255,255,.06); border-radius: 3px; margin-top: 8px; overflow: hidden; }
.bar i { display: block; height: 100%; background: linear-gradient(90deg, var(--gold), var(--gold2)); }
.ctlrow { display: flex; align-items: center; justify-content: center; gap: 36px; }
.ctl { display: flex; flex-direction: column; align-items: center; gap: 7px; color: var(--dim); }
.ctl span { width: 56px; height: 56px; border-radius: 50%; background: var(--panel2);
  border: 1px solid var(--glass-border); display: flex; align-items: center; justify-content: center; font-size: 20px; }
.ctl em { font-size: 11px; font-style: normal; }
.bigctl {
  width: 78px; height: 78px; border-radius: 50%; background: linear-gradient(145deg, var(--gold), #dba70e);
  font-size: 28px; color: #1a1400; box-shadow: 0 8px 32px rgba(240,199,66,.35);
}
.vol { gap: 12px; }
.vol input { accent-color: var(--gold); }
.vol .val { color: var(--gold); font-weight: 600; width: 30px; text-align: right; }
.note { font-size: 11px; color: var(--dim2); margin-top: 7px; }
.track-fix { width: 100%; margin-top: 10px; padding: 9px 12px; border-radius: 10px; border: 1px dashed rgba(240,199,66,.35);
  background: rgba(240,199,66,.06); color: var(--gold); font-size: 12px; }
.seg {
  display: flex; background: var(--panel2); border: 1px solid var(--glass-border);
  border-radius: var(--radius-sm); overflow: hidden;
}
.seg.disabled { opacity: .5; }
.seg div { flex: 1; text-align: center; padding: 10px; font-size: 14px; color: var(--dim); }
.seg div.on { background: linear-gradient(135deg, var(--gold), #dba70e); color: #1a1400; font-weight: 700; }
.grid4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.fx {
  background: var(--panel2); border: 1px solid var(--glass-border); border-radius: 14px;
  padding: 12px 4px; text-align: center; font-size: 12px;
}
.fx .ic { font-size: 22px; margin-bottom: 4px; }
.tip { text-align: center; font-size: 11px; color: var(--dim2); margin-top: 20px; }
</style>
