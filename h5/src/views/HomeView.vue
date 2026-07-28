<template>
  <div class="page">
    <div class="statusbar row"><span class="grow">🎤 家庭KTV</span><span class="nick">{{ user.nickname }}</span></div>

    <!-- ① 当前播放条 -->
    <section class="sec"><NowPlayingBar /></section>

    <!-- ② 搜索框（点击进搜索页） -->
    <section class="sec">
      <div class="search" @click="$router.push({ name: 'search' })">
        🔍 <span class="ph">歌名 / 歌手 / 拼音首字母</span>
      </div>
    </section>

    <!-- ③ 分类宫格 -->
    <section class="sec">
      <div class="grid4">
        <div v-for="c in cats" :key="c.label" class="cat" @click="onCat(c)">
          <div class="ic">{{ c.ic }}</div>{{ c.label }}
        </div>
      </div>
    </section>

    <!-- ④ 热门榜 -->
    <section class="sec grow">
      <div class="row hd"><b>🔥 热门榜</b><span class="sub">近 30 天点唱</span></div>
      <div v-if="loading" class="tip">加载中…</div>
      <div v-else-if="!hot.length" class="tip">曲库还没有歌，先去后台扫描入库</div>
      <SongRow v-for="(s, i) in hot" :key="s.id" :song="s" :rank="i + 1"
               :ordered="orderedIds.has(s.id)" @order="order" />
    </section>

    <TabBar active="home" />
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import api, { makeControls } from '../api/client'
import { useUserStore } from '../stores/user'
import { useToast } from '../composables/useToast'
import TabBar from '../components/TabBar.vue'
import SongRow from '../components/SongRow.vue'
import NowPlayingBar from '../components/NowPlayingBar.vue'

const router = useRouter()
const user = useUserStore()
const { toast } = useToast()
const controls = makeControls(user.clientToken)

const hot = ref([])
const loading = ref(true)
const orderedIds = reactive(new Set())

const cats = [
  { ic: '🎤', label: '歌手' }, { ic: '🌏', label: '语种' },
  { ic: '📚', label: '歌单' }, { ic: '✨', label: '新歌' },
  { ic: '👥', label: '对唱' }, { ic: '🧒', label: '儿歌' },
  { ic: '🎬', label: '影视金曲' }, { ic: '🕘', label: '最近唱' },
  { ic: '❤️', label: '我的收藏' }
]

onMounted(async () => {
  try {
    // 真实点唱排行（P3.4）；为空时（新库无播放记录）退回最新入库
    let list = await api.ranking(30).catch(() => [])
    if (!list.length) list = await api.newSongs().catch(() => [])
    hot.value = list
  } finally {
    loading.value = false
  }
})

async function order(song) {
  try {
    await controls.order(song.id)
    orderedIds.add(song.id)
    toast('已加入队列')
  } catch (e) {
    if (e.code === 'SONG_IN_QUEUE') {
      toast(e.message || '这首歌已在队列中')
    } else {
      toast(e.message || '点歌失败')
    }
  }
}

function onCat(c) {
  if (c.label === '歌手') router.push({ name: 'browse', query: { tab: 'artists' } })
  else if (c.label === '语种') router.push({ name: 'browse', query: { tab: 'languages' } })
  else if (c.label === '歌单') router.push({ name: 'playlists' })
  else if (c.label === '新歌') router.push({ name: 'artist', params: { name: 'all' }, query: { mode: 'all', sort: 'new' } })
  else if (c.label === '对唱') router.push({ name: 'artist', params: { name: 'all' }, query: { mode: 'vocalForm', value: '对唱' } })
  else if (c.label === '儿歌') router.push({ name: 'artist', params: { name: 'all' }, query: { mode: 'tag', value: '儿歌' } })
  else if (c.label === '影视金曲') router.push({ name: 'artist', params: { name: 'all' }, query: { mode: 'tag', value: '影视' } })
  else if (c.label === '最近唱') router.push({ name: 'recent-history' })
  else if (c.label === '我的收藏') router.push({ name: 'favorites' })
  else toast('分类「' + c.label + '」即将开放')
}
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 74px; display: flex; flex-direction: column; }
.statusbar { padding: 12px 16px 4px; font-size: 15px; font-weight: 700; }
.nick { font-size: 12px; color: var(--dim); font-weight: 400; }
.sec { padding: 0 16px; margin-top: 12px; }
.search {
  background: var(--panel2); border: 1px solid var(--glass-border); border-radius: 12px;
  padding: 12px 14px; color: var(--dim); font-size: 14px;
}
.search .ph { color: var(--dim2); }
.grid4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.cat {
  background: var(--panel2); border: 1px solid var(--glass-border); border-radius: 14px;
  padding: 14px 4px; text-align: center; font-size: 12px; transition: var(--transition);
}
.cat:active { transform: scale(.95); }
.cat .ic { font-size: 22px; margin-bottom: 6px; }
.hd { margin-bottom: 4px; }
.hd b { font-size: 15px; }
.hd .sub { font-size: 11px; color: var(--dim2); margin-left: 8px; }
.tip { color: var(--dim2); font-size: 13px; padding: 20px 0; text-align: center; }
</style>
