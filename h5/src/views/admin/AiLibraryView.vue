<template>
  <AdminLayout active="ai">
    <div class="head">
      <div>
        <h1>AI 曲库与主题歌单</h1>
        <p>DeepSeek 置信度达到 90% 自动入库，低于 90% 才进入人工复核。</p>
      </div>
      <button class="btn ghost" @click="refreshAll" :disabled="loading">刷新</button>
    </div>

    <div class="tabs">
      <button :class="{ on: tab === 'tasks' }" @click="tab = 'tasks'">AI 任务</button>
      <button :class="{ on: tab === 'playlists' }" @click="tab = 'playlists'">主题歌单</button>
    </div>

    <section v-if="tab === 'tasks'">
      <div class="toolbar card">
        <div class="field compact"><label>单曲 ID</label><input v-model.number="singleSongId" type="number" min="1" placeholder="歌曲 ID" /></div>
        <button class="btn ghost" @click="createSingle" :disabled="busy || !singleSongId">分析单曲</button>
        <div class="divider"></div>
        <div class="field compact"><label>批量数量</label><input v-model.number="batchLimit" type="number" min="1" max="500" /></div>
        <button class="btn" @click="createBatch" :disabled="busy">分析未分类歌曲</button>
      </div>

      <div v-if="message" class="notice">{{ message }}</div>
      <div class="task-list">
        <article v-for="task in tasks" :key="task.id" class="task card" :class="`status-${task.status}`">
          <div class="task-top">
            <div><strong>任务 #{{ task.id }}</strong><span class="song-id">歌曲 #{{ task.songId }}</span></div>
            <span class="status">{{ statusLabel(task.status) }}</span>
          </div>
          <div class="meta">模型 {{ task.model }} · 置信度 <b :class="confidenceClass(task)">{{ confidenceText(task) }}</b> · 已尝试 {{ task.attemptCount }} 次 · {{ formatTime(task.createdAt) }}</div>
          <div v-if="task.errorMessage" class="error">{{ task.errorMessage }}</div>
          <div v-if="task.status === 'review'" class="result">
            <div class="result-grid">
              <div class="field"><label>语种</label><input v-model="draft(task).language" /></div>
              <div class="field"><label>年代</label><input v-model="draft(task).era" /></div>
              <div class="field"><label>适龄</label><input v-model="draft(task).ageRange" /></div>
              <div class="field"><label>演唱形式</label><input v-model="draft(task).vocalForm" /></div>
              <div class="field"><label>模型置信度</label><input :value="confidenceText(task)" disabled /></div>
              <div class="field wide"><label>曲风（逗号分隔）</label><input v-model="draft(task).genresText" /></div>
              <div class="field wide"><label>主题（逗号分隔）</label><input v-model="draft(task).themesText" /></div>
              <div class="field wide"><label>推荐歌单（逗号分隔）</label><input v-model="draft(task).playlistsText" /></div>
              <div class="field wide"><label>判断说明</label><textarea v-model="draft(task).reason" rows="2"></textarea></div>
            </div>
            <div class="actions"><button class="btn" @click="applyTask(task)" :disabled="busy">确认并应用</button></div>
          </div>
          <div v-if="task.status === 'failed'" class="actions"><button class="btn ghost" @click="retryTask(task.id)" :disabled="busy">重试</button></div>
        </article>
        <div v-if="!tasks.length && !loading" class="empty card">暂无 AI 分析任务</div>
      </div>
    </section>

    <section v-else class="playlist-layout">
      <div class="playlist-list card">
        <div class="section-title"><strong>主题歌单</strong><button class="text-btn" @click="newPlaylist">＋ 新建</button></div>
        <button v-for="item in playlists" :key="item.id" class="playlist-item" :class="{ on: selectedPlaylist?.id === item.id }" @click="selectPlaylist(item.id)">
          <span><strong>{{ item.name }}</strong><small>{{ item.theme || '未设置主题' }} · {{ item.songCount }} 首</small></span>
          <em v-if="item.aiGenerated">AI</em>
        </button>
        <div v-if="!playlists.length" class="empty mini">暂无歌单</div>
      </div>

      <div class="playlist-main">
        <div class="card generator">
          <div class="section-title"><strong>按 AI 标签生成</strong><span>会保留人工加入的歌曲</span></div>
          <div class="inline-form">
            <input v-model="generateForm.name" placeholder="歌单名称，如 90年代金曲" />
            <input v-model="generateForm.tag" placeholder="AI 标签，如 90年代 / 摇滚" />
            <input v-model.number="generateForm.limit" class="short" type="number" min="1" max="500" />
            <button class="btn" @click="generatePlaylist" :disabled="busy">生成/更新</button>
          </div>
        </div>

        <div class="card editor">
          <div class="section-title"><strong>{{ playlistForm.id ? '编辑歌单' : '新建歌单' }}</strong></div>
          <div class="result-grid">
            <div class="field"><label>名称</label><input v-model="playlistForm.name" /></div>
            <div class="field"><label>主题</label><input v-model="playlistForm.theme" /></div>
            <div class="field wide"><label>描述</label><textarea v-model="playlistForm.description" rows="2"></textarea></div>
          </div>
          <div v-if="playlistForm.id" class="cover-editor">
            <div class="cover-preview" :style="coverStyle(selectedPlaylist)"><span v-if="!selectedPlaylist?.coverUrl">🎶</span></div>
            <div><label class="upload-btn">上传自定义封面<input type="file" accept="image/jpeg,image/png,image/webp" @change="uploadCover" /></label><small>支持 JPG、PNG、WebP，最大 5MB</small></div>
          </div>
          <label class="check"><input v-model="playlistForm.publicVisible" type="checkbox" /> H5 公开展示</label>
          <div class="actions">
            <button class="btn" @click="savePlaylist" :disabled="busy || !playlistForm.name">保存歌单</button>
            <button v-if="playlistForm.id" class="btn danger" @click="deletePlaylist" :disabled="busy">删除</button>
          </div>
        </div>

        <div v-if="selectedPlaylist" class="card song-panel">
          <div class="section-title"><strong>歌曲列表（{{ selectedPlaylist.songs.length }}）</strong><span>人工歌曲会在 AI 更新时保留</span></div>
          <div class="inline-form add-song">
            <input v-model.number="addSongId" type="number" min="1" placeholder="输入歌曲 ID 人工加入" />
            <button class="btn ghost" @click="addSong" :disabled="busy || !addSongId">加入</button>
          </div>
          <div class="songs">
            <div v-for="(song,index) in selectedPlaylist.songs" :key="song.songId" class="song-row" draggable="true"
                 :class="{ dragging: draggedSongId === song.songId }" @dragstart="startDrag(song.songId)" @dragover.prevent @drop="dropSong(song.songId)" @dragend="draggedSongId = null">
              <span class="drag">⋮⋮</span>
              <span class="order">{{ song.sortOrder + 1 }}</span>
              <span class="song-name"><strong>{{ song.title }}</strong><small>{{ song.artist }} · #{{ song.songId }}</small></span>
              <span class="source" :class="{ manual: song.manual }">{{ song.manual ? '人工' : 'AI' }}</span>
              <button class="move" @click="moveSong(index,-1)" :disabled="busy || index === 0">↑</button>
              <button class="move" @click="moveSong(index,1)" :disabled="busy || index === selectedPlaylist.songs.length-1">↓</button>
              <button class="text-btn red" @click="removeSong(song.songId)" :disabled="busy">移除</button>
            </div>
            <div v-if="!selectedPlaylist.songs.length" class="empty mini">歌单中暂无歌曲</div>
          </div>
        </div>
      </div>
    </section>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '../../api/client'
import AdminLayout from './AdminLayout.vue'
import { alertDialog, confirmDialog } from '../../composables/useDialog'

const tab = ref('tasks')
const loading = ref(false)
const busy = ref(false)
const message = ref('')
const tasks = ref([])
const drafts = reactive({})
const singleSongId = ref(null)
const batchLimit = ref(50)
const playlists = ref([])
const selectedPlaylist = ref(null)
const addSongId = ref(null)
const draggedSongId = ref(null)
const generateForm = reactive({ name: '', tag: '', limit: 100 })
const playlistForm = reactive({ id: null, name: '', description: '', theme: '', publicVisible: true })

onMounted(refreshAll)

async function refreshAll() {
  loading.value = true
  try { await Promise.all([loadTasks(), loadPlaylists()]) } finally { loading.value = false }
}

async function loadTasks() { tasks.value = await api.adminAiTasks().catch(() => []) }
async function loadPlaylists() { playlists.value = await api.adminAiPlaylists().catch(() => []) }

function parseResult(task) {
  try { return JSON.parse(task.resultJson || '{}') } catch { return {} }
}

function draft(task) {
  if (!drafts[task.id]) {
    const value = parseResult(task)
    drafts[task.id] = {
      language: value.language || '未知', era: value.era || '未知', ageRange: value.ageRange || '未知',
      vocalForm: value.vocalForm || '未知', confidence: Number(value.confidence || 0), genresText: (value.genres || []).join(', '),
      themesText: (value.themes || []).join(', '), playlistsText: (value.recommendedPlaylists || []).join(', '), reason: value.reason || ''
    }
  }
  return drafts[task.id]
}

function splitTags(value) { return value.split(/[,，]/).map(v => v.trim()).filter(Boolean) }
function statusLabel(status) { return ({ pending: '待处理', processing: '处理中', review: '待人工复核', applied: '人工已应用', auto_applied: '高置信自动入库', failed: '失败' })[status] || status }
function confidence(task) { return Number(parseResult(task).confidence || 0) }
function confidenceText(task) { return task.resultJson ? `${Math.round(confidence(task) * 100)}%` : '—' }
function confidenceClass(task) { return confidence(task) >= 0.9 ? 'confidence-high' : task.resultJson ? 'confidence-low' : '' }
function formatTime(value) { return value ? new Date(value).toLocaleString() : '' }
function flash(value) { message.value = value; setTimeout(() => { message.value = '' }, 2500) }

async function createSingle() {
  await run(async () => { await api.adminAiCreateTask(singleSongId.value); singleSongId.value = null; flash('已创建单曲分析任务'); await loadTasks() })
}
async function createBatch() {
  await run(async () => { const result = await api.adminAiCreateUnclassified(batchLimit.value); flash(`已创建 ${result.created} 个任务`); await loadTasks() })
}
async function retryTask(id) { await run(async () => { await api.adminAiRetryTask(id); delete drafts[id]; await loadTasks() }) }
async function applyTask(task) {
  const value = draft(task)
  await run(async () => {
    await api.adminAiApplyTask(task.id, {
      language: value.language, era: value.era, ageRange: value.ageRange, vocalForm: value.vocalForm,
      genres: splitTags(value.genresText), themes: splitTags(value.themesText),
      recommendedPlaylists: splitTags(value.playlistsText), reason: value.reason, confidence: value.confidence
    })
    delete drafts[task.id]
    flash('AI 标签已应用到歌曲')
    await loadTasks()
  })
}

function newPlaylist() {
  selectedPlaylist.value = null
  Object.assign(playlistForm, { id: null, name: '', description: '', theme: '', publicVisible: true })
}
async function selectPlaylist(id) {
  selectedPlaylist.value = await api.adminAiPlaylist(id)
  Object.assign(playlistForm, {
    id: selectedPlaylist.value.id, name: selectedPlaylist.value.name, description: selectedPlaylist.value.description,
    theme: selectedPlaylist.value.theme || '', publicVisible: selectedPlaylist.value.publicVisible
  })
}
async function savePlaylist() {
  await run(async () => {
    const body = { name: playlistForm.name, description: playlistForm.description, theme: playlistForm.theme, publicVisible: playlistForm.publicVisible }
    const saved = playlistForm.id ? await api.adminAiUpdatePlaylist(playlistForm.id, body) : await api.adminAiCreatePlaylist(body)
    await loadPlaylists(); await selectPlaylist(saved.id); flash('歌单已保存')
  })
}
async function deletePlaylist() {
  if (!await confirmDialog(`歌单“${playlistForm.name}”将被删除。`, { title: '删除主题歌单', tone: 'warning' })) return
  await run(async () => { await api.adminAiDeletePlaylist(playlistForm.id); newPlaylist(); await loadPlaylists() })
}
async function generatePlaylist() {
  await run(async () => {
    const saved = await api.adminAiGeneratePlaylist({ ...generateForm })
    await loadPlaylists(); await selectPlaylist(saved.id); flash('主题歌单已生成')
  })
}
async function addSong() {
  await run(async () => { selectedPlaylist.value = await api.adminAiAddPlaylistSong(selectedPlaylist.value.id, addSongId.value); addSongId.value = null; await loadPlaylists() })
}
async function removeSong(songId) {
  await run(async () => { await api.adminAiRemovePlaylistSong(selectedPlaylist.value.id, songId); await selectPlaylist(selectedPlaylist.value.id); await loadPlaylists() })
}
async function uploadCover(event) {
  const file = event.target.files?.[0]
  if (!file) return
  await run(async () => { await api.adminAiUploadPlaylistCover(selectedPlaylist.value.id, file); await selectPlaylist(selectedPlaylist.value.id); await loadPlaylists(); flash('歌单封面已更新') })
  event.target.value = ''
}
function coverStyle(playlist) { return playlist?.coverUrl ? { backgroundImage: `url(${playlist.coverUrl})` } : {} }
function startDrag(songId) { draggedSongId.value = songId }
async function dropSong(targetSongId) {
  if (!draggedSongId.value || draggedSongId.value === targetSongId) return
  const songs = [...selectedPlaylist.value.songs]
  const from = songs.findIndex(song => song.songId === draggedSongId.value)
  const to = songs.findIndex(song => song.songId === targetSongId)
  const [moved] = songs.splice(from, 1)
  songs.splice(to, 0, moved)
  await saveOrder(songs)
  draggedSongId.value = null
}
async function moveSong(index, offset) {
  const songs = [...selectedPlaylist.value.songs]
  const target = index + offset
  if (target < 0 || target >= songs.length) return
  ;[songs[index], songs[target]] = [songs[target], songs[index]]
  await saveOrder(songs)
}
async function saveOrder(songs) {
  await run(async () => { selectedPlaylist.value = await api.adminAiReorderPlaylistSongs(selectedPlaylist.value.id, songs.map(song => song.songId)); flash('歌曲顺序已保存') })
}
async function run(action) {
  busy.value = true
  try { await action() } catch (error) { await alertDialog(error.message || '操作失败') } finally { busy.value = false }
}
</script>

<style scoped>
.head,.task-top,.section-title,.actions,.inline-form,.toolbar { display:flex; align-items:center; }
.head { justify-content:space-between; margin-bottom:16px; }
h1 { font-size:22px; margin:0 0 5px; } .head p { margin:0; color:var(--dim2); font-size:13px; }
.tabs { display:flex; gap:6px; margin-bottom:16px; border-bottom:1px solid var(--line); }
.tabs button { border:0; background:none; color:var(--dim); padding:10px 18px; cursor:pointer; border-bottom:2px solid transparent; }
.tabs button.on { color:var(--gold); border-color:var(--gold); }
.card { background:var(--panel2); border:1px solid var(--glass-border); border-radius:var(--radius); padding:16px; }
.toolbar { gap:12px; flex-wrap:wrap; }.divider { width:1px; height:34px; background:var(--line); margin:0 4px; }
.field { display:flex; flex-direction:column; gap:6px; }.field label { color:var(--dim2); font-size:11px; }
.field.compact { flex-direction:row; align-items:center; }.field.compact label { font-size:12px; }
input,textarea { background:rgba(255,255,255,.035); border:1px solid var(--glass-border); border-radius:8px; padding:9px 11px; color:var(--text); outline:none; font:inherit; }
input:focus,textarea:focus { border-color:rgba(240,199,66,.45); }.toolbar input { width:90px; }.notice { color:var(--green); font-size:12px; margin:12px 0; }
.task-list { display:grid; gap:12px; margin-top:14px; }.task { border-left:3px solid var(--dim2); }.task.status-review { border-left-color:var(--gold); }.task.status-failed { border-left-color:var(--red); }.task.status-applied,.task.status-auto_applied { border-left-color:var(--green); }
.task-top { justify-content:space-between; }.song-id { color:var(--dim); font-size:12px; margin-left:12px; }.status { font-size:11px; border-radius:999px; padding:4px 10px; background:rgba(255,255,255,.06); color:var(--dim); }
.meta { color:var(--dim2); font-size:11px; margin-top:7px; }.meta b{font-weight:700}.confidence-high{color:var(--green)}.confidence-low{color:var(--gold)}.error { color:#ff8b8b; background:rgba(239,68,68,.08); padding:9px 11px; border-radius:8px; margin-top:10px; font-size:12px; }
.result { margin-top:14px; padding-top:14px; border-top:1px solid var(--line); }.result-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; }.field.wide { grid-column:1/-1; }.actions { gap:9px; margin-top:14px; }
.playlist-layout { display:grid; grid-template-columns:260px minmax(0,1fr); gap:16px; }.playlist-main { display:grid; gap:16px; }.section-title { justify-content:space-between; margin-bottom:13px; }.section-title span { color:var(--dim2); font-size:11px; }
.playlist-item { width:100%; display:flex; align-items:center; justify-content:space-between; text-align:left; background:none; border:1px solid transparent; padding:11px; border-radius:9px; color:var(--text); cursor:pointer; }.playlist-item:hover,.playlist-item.on { background:var(--gold-glow); border-color:rgba(240,199,66,.15); }.playlist-item span { display:flex; flex-direction:column; gap:4px; }.playlist-item small { color:var(--dim2); }.playlist-item em,.source { font-style:normal; color:var(--gold); font-size:10px; border:1px solid rgba(240,199,66,.25); padding:2px 6px; border-radius:999px; }
.inline-form { gap:9px; }.inline-form input { flex:1; }.inline-form .short { max-width:72px; }.check { display:flex; align-items:center; gap:7px; color:var(--dim); font-size:12px; margin-top:13px; }.check input { width:auto; }
.text-btn { border:0; background:none; color:var(--gold); cursor:pointer; }.text-btn.red { color:#ff8b8b; }.btn.danger { background:rgba(239,68,68,.12); border-color:rgba(239,68,68,.3); color:#ff8b8b; }
.cover-editor{display:flex;align-items:center;gap:13px;margin-top:14px;padding-top:14px;border-top:1px solid var(--line)}.cover-preview{width:70px;height:70px;border-radius:12px;background:linear-gradient(145deg,rgba(240,199,66,.22),rgba(139,92,246,.18));background-size:cover;background-position:center;display:grid;place-items:center;font-size:26px}.cover-editor>div:last-child{display:flex;flex-direction:column;gap:6px}.cover-editor small{color:var(--dim2);font-size:10px}.upload-btn{color:var(--gold);font-size:12px;cursor:pointer}.upload-btn input{display:none}
.add-song { margin-bottom:10px; }.song-row { display:flex; align-items:center; gap:8px; padding:10px 3px; border-top:1px solid var(--line); transition:opacity .15s; }.song-row.dragging{opacity:.35}.drag{color:var(--dim2);cursor:grab;font-size:16px}.order { width:25px; color:var(--dim2); font-size:11px; }.song-name { flex:1; display:flex; flex-direction:column; gap:3px; }.song-name small { color:var(--dim2); }.source.manual { color:var(--green); border-color:rgba(52,211,153,.25); }.move{border:0;background:rgba(255,255,255,.05);color:var(--dim);border-radius:6px;width:25px;height:25px}.move:disabled{opacity:.25}.empty { text-align:center; color:var(--dim2); padding:30px; }.empty.mini { padding:18px 8px; font-size:12px; }
@media(max-width:850px){.playlist-layout{grid-template-columns:1fr}.result-grid{grid-template-columns:1fr}.field.wide{grid-column:auto}.inline-form{flex-wrap:wrap}.inline-form input{min-width:140px}}
</style>
