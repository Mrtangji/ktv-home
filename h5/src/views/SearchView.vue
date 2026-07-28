<template>
  <div class="page">
    <!-- 搜索框 -->
    <div class="sec sbar">
      <span class="back" @click="$router.back()">‹</span>
      <div class="search grow">
        <input ref="inp" v-model="kw" placeholder="歌名 / 歌手 / 拼音首字母" @input="onInput" />
        <span v-if="kw" class="clear" @click="clear">✕</span>
      </div>
    </div>

    <!-- 结果 -->
    <div class="sec grow results">
      <div v-if="loading" class="tip">搜索中…</div>
      <template v-else-if="results.length">
        <div class="cnt">歌曲 · {{ results.length }} 个结果</div>
        <SongRow v-for="s in results" :key="s.id" :song="s" :keyword="kw"
                 :extra="fmtDur(s.durationMs)" :ordered="orderedIds.has(s.id)" @order="order" />
      </template>
      <div v-else-if="kw && !loading" class="empty">
        <div class="e-title">曲库还没有这首歌</div>
        <button class="btn ghost" @click="addWish">告诉我们想唱《{{ kw }}》</button>
      </div>
      <div v-else class="tip">输入关键词开始搜索</div>
    </div>

    <TabBar active="home" />
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import api, { makeControls } from '../api/client'
import { useUserStore } from '../stores/user'
import { useToast } from '../composables/useToast'
import TabBar from '../components/TabBar.vue'
import SongRow from '../components/SongRow.vue'

const user = useUserStore()
const { toast } = useToast()
const controls = makeControls(user.clientToken)

const kw = ref('')
const results = ref([])
const loading = ref(false)
const orderedIds = reactive(new Set())
const inp = ref(null)
let debounce = null

onMounted(() => inp.value?.focus())

// 300ms 防抖（详设 H5-03）
function onInput() {
  if (debounce) clearTimeout(debounce)
  const q = kw.value.trim()
  if (!q) { results.value = []; loading.value = false; return }
  loading.value = true
  debounce = setTimeout(doSearch, 300)
}

async function doSearch() {
  const q = kw.value.trim()
  if (!q) return
  try {
    results.value = await api.searchSongs(q)
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

function clear() { kw.value = ''; results.value = []; inp.value?.focus() }

async function order(song) {
  try {
    await controls.order(song.id)
    orderedIds.add(song.id)
    toast('已加入队列')
  } catch (e) {
    toast(e.code === 'SONG_IN_QUEUE' ? (e.message || '已在队列中') : (e.message || '点歌失败'))
  }
}

async function addWish() {
  try {
    await api.addWish(kw.value.trim(), user.clientToken)
    toast('已记下《' + kw.value.trim() + '》，稍后补充到曲库')
  } catch (e) {
    toast(e.message || '提交失败')
  }
}

function fmtDur(ms) {
  if (!ms) return ''
  const s = Math.round(ms / 1000)
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 74px; display: flex; flex-direction: column; }
.sec { padding: 0 16px; }
.sbar { display: flex; align-items: center; gap: 10px; padding-top: 12px; }
.back { font-size: 22px; color: var(--dim); padding: 4px; }
.search {
  background: var(--panel2); border: 1px solid rgba(240,199,66,.2); border-radius: 12px;
  padding: 11px 14px; display: flex; align-items: center;
}
.search input { flex: 1; background: none; border: none; outline: none; color: var(--text); font-size: 14px; }
.clear { color: var(--dim2); padding-left: 8px; }
.results { margin-top: 14px; overflow-y: auto; }
.cnt { font-size: 12px; color: var(--dim2); margin-bottom: 4px; }
.tip { color: var(--dim2); font-size: 13px; padding: 30px 0; text-align: center; }
.empty { text-align: center; padding: 40px 0; }
.e-title { color: var(--dim); margin-bottom: 16px; }
</style>
