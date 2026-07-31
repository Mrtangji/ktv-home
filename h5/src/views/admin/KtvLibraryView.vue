<template>
  <AdminLayout active="ktv">
    <header class="page-head"><div><h1>KTV曲库管理</h1><p>正式可播放曲库，手机点歌和 TV 播放均从这里读取</p></div></header>
    <!-- 筛选面板 / Filter Panel -->
    <section class="filter-panel">
      <label><span>关键词</span><input v-model.trim="filters.keyword" placeholder="歌名或歌手" @keyup.enter="search" /></label>
      <label><span>版本类型</span><select v-model="filters.type" @change="search"><option value="">全部类型</option><option value="KTV_VIDEO">KTV版</option><option value="MV">MV版</option><option value="AUDIO">音频版</option><option value="unrecognized">未识别</option></select></label>
      <label><span>入库来源</span><select v-model="filters.source" @change="search"><option value="">全部来源</option><option value="COPIED">自动直拷</option><option value="TRANSCODED">转码入库</option><option value="UNKNOWN">历史曲库</option></select></label>
      <div class="filter-actions"><button class="secondary" @click="reset">重置</button><button class="primary" @click="search">查询</button></div>
    </section>
    <!-- 歌曲列表表格 / Song List Table -->
    <section class="table-panel">
      <div class="toolbar"><span>共 {{ total }} 首可点歌曲</span><button class="danger" :disabled="!selected.size" @click="deleteSelected">批量删除（{{ selected.size }}）</button></div>
      <div class="table-scroll"><table><thead><tr><th><input type="checkbox" :checked="allSelected" @change="toggleAll" /></th><th>歌名</th><th>歌手</th><th>类型</th><th>语种 / 标签</th><th>KTV 文件</th><th>来源</th><th>点唱</th><th>操作</th></tr></thead><tbody>
        <tr v-for="song in songs" :key="song.id"><td><input type="checkbox" :checked="selected.has(song.id)" @change="toggle(song.id)" /></td><td><strong>{{ song.title }}</strong></td><td>{{ song.artist }}</td><td><span class="status" :class="typeClass(song.mediaType)">{{ typeText(song.mediaType) }}</span></td><td>{{ song.language || '—' }}<small>{{ (song.tags || []).join(' / ') || '无标签' }}</small></td><td class="path">{{ song.filePath || '—' }}</td><td>{{ sourceText(song.importSource) }}</td><td>{{ song.playCount || 0 }}</td><td><button class="link" @click="edit(song)">编辑</button><button class="link danger-text" @click="deleteOne(song)">删除</button></td></tr>
        <tr v-if="!songs.length"><td colspan="9" class="empty">暂无符合条件的 KTV 曲库歌曲</td></tr>
      </tbody></table></div>
      <div class="pager"><span>第 {{ page + 1 }} / {{ totalPages || 1 }} 页</span><div><button class="secondary" :disabled="page===0" @click="go(page-1)">上一页</button><button class="secondary" :disabled="page>=totalPages-1" @click="go(page+1)">下一页</button></div></div>
    </section>
    <!-- 编辑弹窗 / Edit Modal -->
    <div v-if="editing" class="mask" @click.self="editing=null"><div class="modal"><h2>编辑歌曲信息</h2><label>歌名<input v-model="form.title" /></label><label>歌手<input v-model="form.artist" /></label><label>语种<select v-model="form.language"><option value="国语">国语</option><option value="粤语">粤语</option><option value="英语">英语</option><option value="日语">日语</option><option value="其他">其他</option></select></label><label>歌词<textarea v-model="form.lyricText" rows="5" :disabled="lyricLoading" :placeholder="lyricLoading?'正在读取歌词…':'可选，支持 LRC'"></textarea></label><div class="modal-actions"><button class="secondary" @click="editing=null">取消</button><button class="primary" :disabled="lyricLoading" @click="save">保存</button></div></div></div>
  </AdminLayout>
</template>

<script setup>
/**
 * KTV曲库管理页面 — 管理正式可播放曲库，支持筛选、编辑、删除歌曲。
 *
 * KTV Library Management Page — manages the official playable song library,
 * supports filtering, editing, and deleting songs.
 */
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../../api/client'
import AdminLayout from './AdminLayout.vue'
import { alertDialog, confirmDialog } from '../../composables/useDialog'
/** 歌曲列表、总数、当前页、总页数、已选集合、编辑中歌曲、歌词加载状态 / Song list, total, page, total pages, selected set, editing song, lyric loading state */
const songs=ref([]),total=ref(0),page=ref(0),totalPages=ref(1),selected=ref(new Set()),editing=ref(null),lyricLoading=ref(false)
/** 筛选条件与编辑表单 / Filter criteria and edit form */
const filters=reactive({keyword:'',type:'',source:''}),form=reactive({title:'',artist:'',language:'国语',lyricText:''})
/** 是否全选 / Whether all items are selected */
const allSelected=computed(()=>songs.value.length>0&&songs.value.every(s=>selected.value.has(s.id)))
/**
 * 加载歌曲列表，根据筛选条件和分页查询，并清理已删除的选中项。
 *
 * Loads the song list based on filter criteria and pagination,
 * and cleans up selected items that no longer exist.
 */
async function load(){const r=await api.adminSongs({...filters,page:page.value,size:20});songs.value=r.content||[];total.value=r.total||0;totalPages.value=r.totalPages||1;selected.value=new Set([...selected.value].filter(id=>songs.value.some(s=>s.id===id)))}
/** 搜索：重置到第一页并加载 / Search: reset to first page and load */
function search(){page.value=0;load()}
/** 重置筛选条件并搜索 / Reset filter criteria and search */
function reset(){Object.assign(filters,{keyword:'',type:'',source:''});search()}
/** 跳转到指定页 / Go to a specific page */
function go(p){if(p>=0&&p<totalPages.value){page.value=p;load()}}
/** 切换单首歌曲的选中状态 / Toggle selection of a single song */
function toggle(id){const n=new Set(selected.value);n.has(id)?n.delete(id):n.add(id);selected.value=n}
/** 全选/取消全选 / Select all / deselect all */
function toggleAll(){selected.value=allSelected.value?new Set():new Set(songs.value.map(s=>s.id))}
/**
 * 打开编辑弹窗，加载歌曲信息和歌词文本。
 *
 * Opens the edit modal, loads song info and lyric text.
 * @param {Object} song - 歌曲对象 / Song object
 */
async function edit(song){editing.value=song;lyricLoading.value=false;Object.assign(form,{title:song.title,artist:song.artist,language:song.language||'国语',lyricText:''});if(!song.lyricType||song.lyricType==='none')return;lyricLoading.value=true;try{const text=await api.lyricText(song.id);if(editing.value?.id===song.id)form.lyricText=text||''}catch(e){if(editing.value?.id===song.id)await alertDialog(e.message||'歌词读取失败')}finally{if(editing.value?.id===song.id)lyricLoading.value=false}}
/**
 * 保存编辑后的歌曲信息。
 *
 * Saves the edited song information.
 */
async function save(){try{await api.adminEditSong(editing.value.id,{title:form.title,artist:form.artist,language:form.language,lyricText:form.lyricText||null});editing.value=null;await load()}catch(e){await alertDialog(e.message||'保存失败')}}
/**
 * 删除单首歌曲，确认后删除歌曲及文件。
 *
 * Deletes a single song after confirmation, including the actual file.
 * @param {Object} song - 歌曲对象 / Song object
 */
async function deleteOne(song){if(!await confirmDialog(`《${song.title}》及 /music 中的实际文件将被删除。`,{title:'删除 KTV 歌曲',tone:'warning'}))return;try{await api.adminDeleteSong(song.id);await load()}catch(e){await alertDialog(e.message||'删除失败')}}
/**
 * 批量删除选中的歌曲，确认后删除歌曲及文件。
 *
 * Batch deletes selected songs after confirmation, including actual files.
 */
async function deleteSelected(){if(!await confirmDialog(`将删除 ${selected.value.size} 首歌曲及其 /music 中的实际文件。`,{title:'批量删除 KTV 歌曲',tone:'warning'}))return;try{await api.adminDeleteSongs([...selected.value]);selected.value=new Set();await load()}catch(e){await alertDialog(e.message||'批量删除失败')}}
/** 媒体类型文本映射 / Media type text mapping */
function typeText(v){return{KTV_VIDEO:'KTV版',MV:'MV版',AUDIO:'音频版'}[v]||v}
/** 媒体类型样式类名 / Media type CSS class */
function typeClass(v){return v==='KTV_VIDEO'?'green':v==='MV'?'blue':'neutral'}
/** 导入来源文本映射 / Import source text mapping */
function sourceText(v){return{COPIED:'自动直拷',TRANSCODED:'转码入库',UNKNOWN:'历史曲库'}[v]||'历史曲库'}
onMounted(load)
</script>

<style scoped>
.page-head{margin-bottom:18px}.page-head h1{font-size:22px}.page-head p{color:#64748b;font-size:13px;margin-top:6px}.primary,.secondary,.danger{height:34px;padding:0 14px;border-radius:6px;font-size:13px}.primary{background:#2563eb;color:#fff}.secondary{background:#fff;border:1px solid #cbd5e1;color:#334155}.danger{background:#fff;border:1px solid #fecaca;color:#b91c1c}.primary:disabled,.secondary:disabled,.danger:disabled{opacity:.45;cursor:not-allowed}.filter-panel{display:flex;align-items:flex-end;flex-wrap:wrap;gap:10px;padding:12px 14px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;margin-bottom:14px}.filter-panel label{display:flex;flex:0 0 180px;flex-direction:column;gap:5px;color:#475569;font-size:12px}.filter-panel label:first-child{flex-basis:280px}.filter-panel input,.filter-panel select{width:100%;height:34px;border:1px solid #cbd5e1;border-radius:6px;padding:0 10px;background:#fff;color:#172033;font-size:13px;box-shadow:0 1px 2px rgba(15,23,42,.03)}.filter-panel input:focus,.filter-panel select:focus{border-color:#60a5fa;box-shadow:0 0 0 3px rgba(37,99,235,.1);outline:0}.modal label{display:flex;flex-direction:column;gap:6px;color:#475569;font-size:12px}.modal input,.modal select,.modal textarea{border:1px solid #cbd5e1;border-radius:6px;padding:0 10px;background:#fff;color:#172033}.modal input,.modal select{height:36px}.modal textarea{padding:9px 10px;resize:vertical}.filter-actions{display:flex;align-items:flex-end;gap:8px}.table-panel{background:#fff;border:1px solid #e2e8f0;border-radius:8px}.toolbar,.pager{display:flex;align-items:center;justify-content:space-between;padding:13px 16px;color:#64748b;font-size:12px}.toolbar{border-bottom:1px solid #e2e8f0}.pager{border-top:1px solid #e2e8f0}.pager div{display:flex;gap:8px}.table-scroll{overflow:auto}table{width:100%;border-collapse:collapse;min-width:1050px;font-size:12px}th{padding:11px 10px;text-align:left;background:#f8fafc;color:#64748b;border-bottom:1px solid #e2e8f0}td{padding:12px 10px;border-bottom:1px solid #eef2f7;color:#334155}td small{display:block;color:#94a3b8;margin-top:4px}.path{max-width:260px;word-break:break-all;color:#64748b}.status{display:inline-flex;padding:3px 8px;border-radius:999px;font-weight:600}.green{background:#dcfce7;color:#166534}.blue{background:#dbeafe;color:#1d4ed8}.neutral{background:#f1f5f9;color:#475569}.link{color:#2563eb;margin-right:12px;font-size:12px}.danger-text{color:#b91c1c}.empty{text-align:center;padding:36px;color:#94a3b8}.mask{position:fixed;inset:0;background:rgba(15,23,42,.45);display:grid;place-items:center;z-index:100}.modal{width:min(440px,calc(100vw - 32px));background:#fff;border-radius:8px;padding:22px;box-shadow:0 18px 50px rgba(15,23,42,.18)}.modal h2{font-size:17px;margin-bottom:18px}.modal label{margin-bottom:13px}.modal-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:18px}@media(max-width:700px){.filter-panel label:first-child{flex-basis:100%}.filter-panel label:not(:first-child){flex:1 1 140px}.filter-actions{width:100%;justify-content:flex-end}}
</style>
