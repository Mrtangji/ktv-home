<template>
  <div class="page">
    <header class="head"><button @click="$router.back()">‹</button><div><h1>{{ pageTitle }}</h1><p>{{ songs.length }} 首歌曲</p></div></header>
    <div class="sorts"><button :class="{ on: sort === 'hot' }" @click="setSort('hot')">按热度</button><button :class="{ on: sort === 'title' }" @click="setSort('title')">按歌名</button><button :class="{ on: sort === 'new' }" @click="setSort('new')">最新</button></div>
    <main>
      <div v-if="loading" class="tip">加载中…</div><div v-else-if="!songs.length" class="tip">没有找到相关歌曲</div>
      <SongRow v-for="song in songs" :key="song.id" :song="song" :extra="fmtDur(song.durationMs)" :ordered="orderedIds.has(song.id)" @order="order" />
    </main>
    <TabBar active="home" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api, { makeControls } from '../api/client'
import { useUserStore } from '../stores/user'
import { useToast } from '../composables/useToast'
import TabBar from '../components/TabBar.vue'
import SongRow from '../components/SongRow.vue'

const route = useRoute(), user = useUserStore(), { toast } = useToast(), controls = makeControls(user.clientToken)
const songs = ref([]), loading = ref(true), sort = ref(route.query.sort || 'hot'), orderedIds = reactive(new Set())
const mode = computed(() => route.query.mode || (route.params.name === 'all' ? 'all' : 'artist'))
const value = computed(() => route.query.value || route.params.name || '')
const pageTitle = computed(() => mode.value === 'all' ? '全部歌曲' : mode.value === 'language' ? `${value.value}歌曲` : mode.value === 'tag' ? value.value : mode.value === 'vocalForm' ? value.value : value.value)

async function load() {
  loading.value = true
  const params = { sort: sort.value, limit: 200 }
  if (mode.value !== 'all') params[mode.value] = value.value
  try { songs.value = await api.browseSongs(params) } finally { loading.value = false }
}
onMounted(load); watch(() => route.fullPath, load)
function setSort(value) { sort.value = value; load() }
async function order(song) { try { await controls.order(song.id); orderedIds.add(song.id); toast('已加入队列') } catch (error) { toast(error.message || '点歌失败') } }
function fmtDur(ms) { if (!ms) return ''; const value = Math.round(ms / 1000); return `${Math.floor(value / 60)}:${String(value % 60).padStart(2, '0')}` }
</script>

<style scoped>
.page{min-height:100vh;padding-bottom:74px}.head{display:flex;align-items:center;gap:12px;padding:13px 16px 8px}.head button{border:0;background:none;color:var(--text);font-size:30px;width:30px}.head h1{font-size:19px;margin:0}.head p{font-size:11px;color:var(--dim);margin:3px 0 0}.sorts{display:flex;gap:7px;padding:7px 16px 11px}.sorts button{border:1px solid var(--glass-border);background:var(--panel2);color:var(--dim);border-radius:999px;padding:6px 14px}.sorts button.on{background:var(--gold-glow);border-color:rgba(240,199,66,.28);color:var(--gold)}main{padding:0 16px}.tip{text-align:center;color:var(--dim2);padding:45px 10px}
</style>
