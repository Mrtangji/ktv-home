<template>
  <div class="page">
    <div v-if="!player.tvOnline" class="offline">📺 电视未连接，歌曲会先排队，电视上线后自动播放</div>
    <div class="sec hd"><b>已点歌曲</b><span class="chip">{{ player.queueCount }} 首待唱</span></div>

    <section class="sec host-wrap">
      <div class="host-card">
        <div><strong>{{ host.isHost ? '👑 你是房主' : host.claimed ? `👑 房主：${host.hostNickname}` : '👑 尚未设置房主' }}</strong><small>{{ host.isHost ? '可以管理聚会队列' : host.claimed ? '所有人都可以智能打散演唱顺序' : '首位认领者可管理聚会队列' }}</small></div>
        <button v-if="!host.claimed" class="chip gold" @click="claimHost">认领房主</button>
        <button v-else-if="host.isHost" class="chip" @click="releaseHost">释放</button>
      </div>
      <button v-if="player.queue.length > 1" class="shuffle" @click="shuffleQueue">🔀 智能打散队列</button>
    </section>

    <!-- 正在演唱卡 -->
    <section class="sec" v-if="player.nowPlaying">
      <div class="now">
        <div class="cover"></div>
        <div class="grow">
          <div class="t">{{ player.nowPlaying.song?.title }}
            <span class="a">· {{ player.nowPlaying.song?.artist }}</span></div>
          <div class="s">{{ player.nowPlaying.orderedByNick || '' }} 点</div>
        </div>
        <button class="cut" @click="confirmNext">切歌</button>
      </div>
    </section>

    <!-- 待播列表 -->
    <section class="sec grow list">
      <div v-if="!player.queue.length" class="tip">还没有待播歌曲</div>
      <div v-for="(q, i) in player.queue" :key="q.queueId" class="qrow">
        <span class="no">{{ i + 1 }}</span>
        <div class="grow">
          <div class="t">{{ q.song?.title }}</div>
          <div class="s">{{ q.song?.artist }} · {{ q.orderedByNick || '他人' }} 点</div>
        </div>
        <template v-if="isMine(q)">
          <button class="chip gold" @click="top(q)">顶歌</button>
          <button class="chip" @click="cancel(q)">删除</button>
        </template>
        <span v-else class="mine-tip">他人的歌</span>
      </div>

      <!-- 已播历史（P3.2） -->
      <div class="hist" v-if="history.length">
        <div class="hist-hd" @click="showHist = !showHist">
          🕘 已播历史（{{ history.length }} 首）<span class="grow"></span>{{ showHist ? '收起 ▴' : '展开 ▾' }}
        </div>
        <div v-if="showHist" class="hist-chips">
          <span v-for="(h, i) in history" :key="i" class="chip" @click="reorder(h)">{{ h.title }} ↻</span>
        </div>
      </div>
    </section>

    <TabBar active="queue" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { usePlayerStore } from '../stores/player'
import { useUserStore } from '../stores/user'
import api, { makeControls } from '../api/client'
import { useToast } from '../composables/useToast'
import { confirmDialog } from '../composables/useDialog'
import TabBar from '../components/TabBar.vue'

const player = usePlayerStore()
const user = useUserStore()
const { toast } = useToast()
const controls = makeControls(user.clientToken)

const history = ref([])
const showHist = ref(false)
const host = ref({ claimed: false, isHost: false, hostNickname: null })

onMounted(async () => {
  const [loadedHistory, loadedHost] = await Promise.all([api.history().catch(() => []), api.roomHostStatus(user.clientToken).catch(() => null)])
  history.value = loadedHistory
  if (loadedHost) host.value = loadedHost
})

async function reorder(song) {
  try {
    await controls.order(song.id)
    toast('已重新点歌')
  } catch (e) {
    toast(e.code === 'SONG_IN_QUEUE' ? (e.message || '已在队列中') : (e.message || '点歌失败'))
  }
}

// 本人点的歌才显示顶歌/删除（详设 H5-05）。orderedBy 为服务端 user id，
// H5 无法直接比对 id，改用昵称匹配（家庭场景足够）。
function isMine(q) {
  return q.orderedByNick && q.orderedByNick === user.nickname
}

async function top(q) {
  try { await controls.top(q.queueId); toast('已顶到下一首') }
  catch (e) { toast(e.message || '操作失败') }
}

async function cancel(q) {
  if (!await confirmDialog(`将从队列删除《${q.song?.title}》。`, { title: '删除已点歌曲', tone: 'warning' })) return
  try { await controls.cancel(q.queueId); toast('已删除') }
  catch (e) { toast(e.message || '删除失败') }
}

async function confirmNext() {
  if (!await confirmDialog(`将切掉《${player.nowPlaying?.song?.title}》。`, { title: '确认切歌', tone: 'warning' })) return
  try { await controls.next(); toast('已切歌') }
  catch (e) { toast(e.message || '切歌失败') }
}
async function claimHost() {
  try { host.value = await api.claimRoomHost(user.clientToken); toast('你已成为房主') }
  catch (error) { toast(error.message || '认领失败'); host.value = await api.roomHostStatus(user.clientToken).catch(() => host.value) }
}
async function releaseHost() {
  if (!await confirmDialog('释放后其他人可认领房主身份。', { title: '释放房主身份' })) return
  try { host.value = await api.releaseRoomHost(user.clientToken); toast('已释放房主身份') }
  catch (error) { toast(error.message || '释放失败') }
}
async function shuffleQueue() {
  if (!await confirmDialog('将尽量避免同一位点歌人连续演唱。', { title: '智能打散队列' })) return
  try { await controls.shuffle(); toast('队列已智能打散') }
  catch (error) { toast(error.message || '打散失败') }
}
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 74px; display: flex; flex-direction: column; }
.sec { padding: 0 16px; }
.hd { display: flex; align-items: center; gap: 10px; padding-top: 14px; margin-bottom: 12px; }
.hd b { font-size: 17px; }
.host-wrap { margin-bottom: 12px; }.host-card { display:flex;align-items:center;gap:12px;padding:11px 13px;border:1px solid var(--glass-border);border-radius:12px;background:var(--panel2); }.host-card>div { flex:1;display:flex;flex-direction:column;gap:3px; }.host-card strong { font-size:12px; }.host-card small { color:var(--dim2);font-size:10px; }.shuffle { width:100%;margin-top:8px;padding:9px;border:1px solid rgba(240,199,66,.25);border-radius:10px;background:var(--gold-glow);color:var(--gold);font-weight:700; }
.chip {
  background: var(--panel2); border: 1px solid var(--glass-border); border-radius: 999px;
  padding: 5px 12px; font-size: 11px; color: var(--dim);
}
.chip.gold { color: var(--gold); border-color: rgba(240,199,66,.25); }
.now {
  background: linear-gradient(135deg, rgba(240,199,66,.04), transparent);
  border: 1px solid rgba(240,199,66,.2); border-radius: var(--radius);
  padding: 14px; display: flex; gap: 12px; align-items: center;
}
.cover {
  width: 56px; height: 56px; border-radius: 10px; flex: none;
  background: radial-gradient(circle at 30% 26%, rgba(240,199,66,.12), transparent 42%),
              linear-gradient(145deg, rgba(40,46,66,.85), rgba(20,24,36,.92));
  border: 1px solid var(--glass-border);
}
.now .t { font-size: 14px; font-weight: 700; }
.now .a { color: var(--dim); font-weight: 400; font-size: 12px; }
.now .s { font-size: 11px; color: var(--dim); margin-top: 3px; }
.cut {
  background: linear-gradient(135deg, var(--red), #c62828); color: #fff; font-weight: 700;
  font-size: 13px; border-radius: 8px; padding: 8px 16px; flex: none;
}
.list { margin-top: 14px; overflow-y: auto; }
.qrow { display: flex; align-items: center; gap: 10px; padding: 12px 0; border-bottom: 1px solid var(--line); }
.qrow .no { width: 22px; text-align: center; color: var(--dim); font-weight: 700; }
.qrow .t { font-size: 15px; font-weight: 600; }
.qrow .s { font-size: 12px; color: var(--dim); margin-top: 3px; }
.mine-tip { font-size: 11px; color: var(--dim2); }
.tip { color: var(--dim2); font-size: 13px; padding: 30px 0; text-align: center; }
.offline {
  margin: 12px 16px 0; padding: 10px 14px; border-radius: 10px; font-size: 12px;
  background: rgba(248,113,113,.1); border: 1px solid rgba(248,113,113,.3); color: var(--red);
}
.hist { margin-top: 16px; border-top: 1px solid var(--line); padding-top: 12px; }
.hist-hd { display: flex; align-items: center; font-size: 13px; color: var(--dim); padding: 6px 0; }
.hist-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.hist-chips .chip {
  background: var(--panel2); border: 1px solid var(--glass-border); border-radius: 999px;
  padding: 5px 12px; font-size: 11px; color: var(--dim);
}
</style>
