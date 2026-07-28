<template>
  <div class="page">
    <header class="top"><button @click="$router.back()">‹</button><strong>主题歌单</strong><span></span></header>
    <main>
      <div class="hero"><div class="spark">✨</div><h1>今晚唱什么</h1><p>根据曲风、年代和聚会主题整理的精选歌单</p></div>
      <div v-if="loading" class="tip">加载中…</div>
      <div v-else-if="!playlists.length" class="tip">暂无公开歌单，请先在管理后台生成</div>
      <router-link v-for="playlist in playlists" :key="playlist.id" class="card" :to="{ name: 'playlist-detail', params: { id: playlist.id } }">
        <div class="cover" :style="playlist.coverUrl ? { backgroundImage: `url(${playlist.coverUrl})` } : {}"><span v-if="!playlist.coverUrl">{{ coverEmoji(playlist.theme) }}</span><em v-if="playlist.aiGenerated">AI</em></div>
        <div class="info"><h2>{{ playlist.name }}</h2><p>{{ playlist.description || playlist.theme || '家庭欢唱精选' }}</p><small>{{ playlist.songCount }} 首 · {{ previewText(playlist.preview) }}</small></div>
        <span class="arrow">›</span>
      </router-link>
    </main>
    <TabBar active="home" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api/client'
import TabBar from '../components/TabBar.vue'

const playlists = ref([])
const loading = ref(true)
onMounted(async () => { try { playlists.value = await api.playlists() } finally { loading.value = false } })
function previewText(values = []) { return values.filter(Boolean).map(song => song.title).join('、') || '等待添加歌曲' }
function coverEmoji(theme = '') {
  if (/儿歌|儿童/.test(theme)) return '🧸'
  if (/摇滚|热血/.test(theme)) return '🎸'
  if (/情歌|浪漫/.test(theme)) return '💞'
  if (/怀旧|年代|经典/.test(theme)) return '📻'
  if (/对唱|合唱/.test(theme)) return '👥'
  return '🎶'
}
</script>

<style scoped>
.page{min-height:100vh;padding-bottom:74px}.top{height:52px;display:flex;align-items:center;justify-content:space-between;padding:0 14px;border-bottom:1px solid var(--line);position:sticky;top:0;background:rgba(8,10,15,.94);backdrop-filter:blur(14px);z-index:2}.top button{border:0;background:none;color:var(--text);font-size:30px;width:35px}.top span{width:35px}main{padding:16px}.hero{padding:20px 18px;margin-bottom:14px;border-radius:18px;background:radial-gradient(circle at 80% 0,rgba(240,199,66,.2),transparent 42%),linear-gradient(135deg,rgba(255,255,255,.06),rgba(255,255,255,.02));border:1px solid var(--glass-border)}.spark{font-size:26px}.hero h1{font-size:22px;margin:7px 0 4px}.hero p{color:var(--dim);font-size:12px;margin:0}.card{display:flex;align-items:center;gap:13px;padding:13px;margin-bottom:10px;background:var(--panel2);border:1px solid var(--glass-border);border-radius:15px;color:var(--text)}.cover{width:64px;height:64px;display:grid;place-items:center;position:relative;border-radius:13px;background:linear-gradient(145deg,rgba(240,199,66,.22),rgba(139,92,246,.18));background-size:cover;background-position:center;font-size:27px;flex:none}.cover em{position:absolute;right:4px;top:4px;font-size:8px;font-style:normal;color:var(--gold);border:1px solid rgba(240,199,66,.35);border-radius:6px;padding:1px 4px}.info{min-width:0;flex:1}.info h2{font-size:15px;margin:0 0 5px}.info p,.info small{display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.info p{font-size:12px;color:var(--dim);margin:0 0 7px}.info small{font-size:10px;color:var(--dim2)}.arrow{color:var(--dim2);font-size:24px}.tip{text-align:center;color:var(--dim2);padding:50px 10px;font-size:13px}
</style>
