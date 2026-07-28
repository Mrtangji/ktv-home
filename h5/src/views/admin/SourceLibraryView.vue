<template>
  <AdminLayout active="source">
    <header class="page-head">
      <div><h1>原始音乐管理</h1><p>管理扫描源路径中的素材、重复结果和转码入库任务</p></div>
      <div class="header-actions">
        <button class="primary action-button" :disabled="progress.running || cleaning" @click="transcodeAll"><RefreshCw :size="15" />{{ progress.running ? '转码进行中' : '批量转码所有歌曲' }}</button>
        <button class="danger cleanup-button action-button" :disabled="progress.running || cleaning" @click="cleanupImported"><Trash2 :size="16" />{{ cleaning ? '清理中…' : '自动清理' }}</button>
      </div>
    </header>

    <section class="filter-panel">
      <label><span>关键词</span><input v-model.trim="filters.keyword" placeholder="歌名、歌手或文件名" @keyup.enter="search" /></label>
      <label><span>处理状态</span><select v-model="filters.status"><option value="">全部状态</option><option value="pending">待转码</option><option value="duplicate">重复</option><option value="copied">已自动直拷</option><option value="transcoded">已转码</option><option value="unrecognized">未识别</option><option value="failed">失败</option></select></label>
      <label><span>格式分析</span><select v-model="filters.formatAnalysis"><option value="">全部结果</option><option value="transcode">需转码</option><option value="copy">可直拷</option></select></label>
      <div class="filter-actions"><button class="secondary" @click="reset">重置</button><button class="primary" @click="search">查询</button></div>
    </section>

    <section v-if="progress.running || progress.total" class="progress-panel">
      <div class="progress-copy"><strong>批量转码进度</strong><span>{{ progress.completed || 0 }} / {{ progress.total || 0 }}，成功 {{ progress.transcoded || 0 }}，重复跳过 {{ (progress.skippedSourceDuplicate || 0) + (progress.skippedOutputDuplicate || 0) }}，失败 {{ progress.failed || 0 }}</span></div>
      <div class="progress-value">{{ progressPercent }}%</div>
      <div class="track"><i :style="{ width: progressPercent + '%' }"></i></div>
      <div v-if="progress.currentFile" class="current">正在处理：{{ progress.currentFile }}</div>
    </section>

    <section class="table-panel">
      <div class="toolbar">
        <span>共 {{ total }} 条源素材</span>
        <div class="batch-actions">
          <button class="secondary action-button" :disabled="progress.running || !selectedTranscodableCount" @click="transcodeSelected"><RefreshCw :size="14" />批量转码（{{ selected.length }}）</button>
          <button class="danger action-button" :disabled="!selected.length" @click="deleteSelected"><Trash2 :size="14" />批量删除（{{ selected.length }}）</button>
        </div>
      </div>
      <div class="table-scroll">
        <table>
          <thead><tr><th><input type="checkbox" aria-label="全选当前页" title="全选当前页" :checked="allSelected" @change="setAll($event.target.checked)" /></th><th>源文件</th><th>识别结果</th><th>格式分析</th><th>重复状态</th><th>入库结果</th><th>MD5</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in rows" :key="item.id">
              <td><input type="checkbox" :aria-label="`选择 ${item.sourceFilename}`" :disabled="item.sourceDeleted" :checked="selected.includes(item.id)" @change="setSelected(item.id, $event.target.checked)" /></td>
              <td><strong>{{ item.sourceFilename }}</strong><small>{{ item.sourcePath }}</small></td>
              <td><strong>{{ item.parsedTitle || '未识别' }}</strong><small>{{ item.parsedArtist || '未知歌手' }} · {{ mediaText(item.mediaType) }}</small></td>
              <td><span class="status" :class="item.transcodeRequired ? 'blue' : 'green'">{{ item.transcodeRequired ? '需转码' : '可直拷' }}</span><small>{{ item.videoCodec || '—' }} / {{ item.audioCodec || '—' }} / {{ item.sourceFormat || '—' }}</small></td>
              <td><span class="status" :class="item.duplicate ? 'amber' : 'neutral'">{{ item.duplicate ? '重复' : '未重复' }}</span><small>{{ item.duplicate ? item.reason : '源/输出 MD5 未命中' }}</small></td>
              <td><span class="status" :class="statusClass(item.displayStatus)">{{ statusText(item.displayStatus) }}</span><small>{{ item.outputPath || item.reason || '—' }}</small></td>
              <td class="md5"><small>源：{{ shortMd5(item.sourceMd5) }}</small><small>输出：{{ shortMd5(item.outputMd5) }}</small></td>
              <td><div class="row-actions">
                <button class="link action-button" :disabled="progress.running || !isTranscodable(item)" @click="transcodeOne(item)"><RefreshCw :size="14" />{{ progress.currentRecordId === item.id ? '转码中' : '转码' }}</button>
                <button class="link priority-text action-button" :disabled="!canPrioritize(item)" @click="prioritize(item)"><ListRestart :size="14" />{{ priorityText(item) }}</button>
                <button class="link danger-text action-button" :disabled="item.sourceDeleted" @click="deleteOne(item)"><Trash2 :size="14" />删除</button>
              </div></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="8" class="empty">暂无符合条件的源素材</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>第 {{ page + 1 }} / {{ totalPages || 1 }} 页</span><div><button class="secondary" :disabled="page === 0" @click="go(page - 1)">上一页</button><button class="secondary" :disabled="page >= totalPages - 1" @click="go(page + 1)">下一页</button></div></div>
    </section>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ListRestart, RefreshCw, Trash2 } from 'lucide-vue-next'
import api from '../../api/client'
import AdminLayout from './AdminLayout.vue'
import { alertDialog, confirmDialog } from '../../composables/useDialog'

const rows = ref([]), total = ref(0), page = ref(0), totalPages = ref(1), selected = ref([])
const cleaning = ref(false)
const filters = reactive({ keyword: '', status: '', formatAnalysis: '' })
const progress = ref({ running:false, total:0, completed:0 })
let timer = null
const allSelected = computed(() => rows.value.some(x => !x.sourceDeleted) && rows.value.filter(x => !x.sourceDeleted).every(x => selected.value.includes(x.id)))
const selectedTranscodableCount = computed(() => rows.value.filter(x => selected.value.includes(x.id) && isTranscodable(x)).length)
const progressPercent = computed(() => progress.value.total ? Math.round(progress.value.completed * 100 / progress.value.total) : 0)

async function load() { const r = await api.adminSourceLibrary({ ...filters, page:page.value, size:20 }); rows.value=r.content||[]; total.value=r.total||0; totalPages.value=r.totalPages||1; selected.value=selected.value.filter(id=>rows.value.some(x=>x.id===id)) }
async function loadProgress() { progress.value = await api.adminSourceTranscodeProgress().catch(()=>progress.value); if (!progress.value.running && timer) { clearInterval(timer); timer=null; await load() } }
function search(){ page.value=0; load() } function reset(){ Object.assign(filters,{keyword:'',status:'',formatAnalysis:''}); search() } function go(p){ if(p>=0&&p<totalPages.value){page.value=p;load()} }
function setSelected(id, checked){ selected.value=checked ? [...new Set([...selected.value,id])] : selected.value.filter(x=>x!==id) }
function setAll(checked){ selected.value=checked ? rows.value.filter(x=>!x.sourceDeleted).map(x=>x.id) : [] }
function isTranscodable(item){ return item.transcodeRequired && ['PENDING_TRANSCODE','FAILED'].includes(item.displayStatus) && !item.sourceDeleted }
function isPrioritized(item){ return (progress.value.priorityRecordIds||[]).includes(item.id) }
function canPrioritize(item){ return progress.value.running && isTranscodable(item) && progress.value.currentRecordId!==item.id && !isPrioritized(item) }
function priorityText(item){ return progress.value.currentRecordId===item.id?'转码中':isPrioritized(item)?'已插队':'插队' }
async function startTranscode(ids){ progress.value=await api.adminStartSourceTranscode(ids); if(timer)clearInterval(timer); timer=setInterval(loadProgress,1500) }
async function transcodeAll(){
  if(progress.value.running)return
  if(!await confirmDialog('将处理全部待转码和转码失败的源文件。',{title:'批量转码所有歌曲'}))return
  try { progress.value=await api.adminStartSourceTranscode([],true); if(timer)clearInterval(timer); timer=setInterval(loadProgress,1500) } catch(e) { await alertDialog(e.message||'全量转码启动失败') }
}
async function cleanupImported(){
  if(progress.value.running||cleaning.value)return
  if(!await confirmDialog('仅删除已经成功入库、且曲库文件仍然存在的原始素材。待转码、失败、重复、未识别和曲库文件缺失的素材都会保留。源文件删除后不可恢复。',{title:'自动清理已入库素材',tone:'warning'}))return
  cleaning.value=true
  try {
    const result=await api.adminCleanupImportedSources()
    selected.value=[]
    await load()
    await alertDialog(`清理完成：删除 ${result.deleted||0} 个源文件，跳过 ${result.skipped||0} 个，不符合安全条件或无需清理；失败 ${result.failed||0} 个。`,{title:'自动清理完成',tone:result.failed?'warning':'success'})
  } catch(e) { await alertDialog(e.message||'自动清理失败') }
  finally { cleaning.value=false }
}
async function transcodeSelected(){
  const ids=rows.value.filter(x=>selected.value.includes(x.id)&&isTranscodable(x)).map(x=>x.id)
  if(!ids.length||progress.value.running)return
  if(!await confirmDialog(`将开始处理 ${ids.length} 个已选择的待处理源文件。`,{title:'批量转码'}))return
  try { await startTranscode(ids) } catch(e) { await alertDialog(e.message||'批量转码启动失败') }
}
async function transcodeOne(item){
  if(!isTranscodable(item)||progress.value.running)return
  if(!await confirmDialog(item.sourcePath,{title:'转码该源文件'}))return
  try { await startTranscode([item.id]) } catch(e) { await alertDialog(e.message||'转码启动失败') }
}
async function prioritize(item){
  if(!canPrioritize(item))return
  try { progress.value=(await api.adminPrioritizeSourceTranscode(item.id)).progress } catch(e) { await alertDialog(e.message||'插队失败') }
}
async function deleteOne(item){
  if(!await confirmDialog(item.sourcePath,{title:'删除源文件',tone:'warning'}))return
  try { await api.adminDeleteSources([item.id]); await load() } catch (e) { await alertDialog(e.message || '删除失败') }
}
async function deleteSelected(){
  const selectedIds=[...selected.value]
  if(!selectedIds.length)return
  if(!await confirmDialog(`将永久删除 ${selectedIds.length} 个已选择的源文件，此操作不可撤销。`,{title:'批量删除源文件',tone:'warning'}))return
  try {
    await api.adminDeleteSources(selectedIds)
    selected.value=[]; await load()
  } catch (e) { await alertDialog(e.message || '批量删除失败') }
}
function shortMd5(v){return v?`${v.slice(0,8)}...${v.slice(-8)}`:'—'} function mediaText(v){return{KTV_VIDEO:'KTV 视频',MV:'MV',AUDIO:'音频'}[v]||'未知格式'}
function statusText(v){return{AUTO_COPIED:'已自动直拷',TRANSCODED:'已转码入库',PENDING_TRANSCODE:'待转码',DUPLICATE:'重复跳过',UNRECOGNIZED:'未识别',FAILED:'失败'}[v]||v||'待处理'}
function statusClass(v){return{AUTO_COPIED:'green',TRANSCODED:'green',PENDING_TRANSCODE:'blue',DUPLICATE:'amber',UNRECOGNIZED:'red',FAILED:'red'}[v]||'neutral'}
onMounted(async()=>{await Promise.all([load(),loadProgress()]);if(progress.value.running)timer=setInterval(loadProgress,1500)})
onUnmounted(()=>{if(timer)clearInterval(timer)})
</script>

<style scoped>
.page-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px}.page-head h1{font-size:22px}.page-head p{color:#64748b;font-size:13px;margin-top:6px}.primary,.secondary,.danger{height:34px;padding:0 14px;border-radius:6px;font-size:13px}.primary{background:#2563eb;color:#fff}.secondary{background:#fff;border:1px solid #cbd5e1;color:#334155}.danger{background:#fff;border:1px solid #fecaca;color:#b91c1c}.primary:disabled,.secondary:disabled,.danger:disabled{opacity:.45;cursor:not-allowed}.filter-panel{display:flex;align-items:flex-end;flex-wrap:wrap;gap:10px;padding:12px 14px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;margin-bottom:14px}.filter-panel label{display:flex;flex:0 0 180px;flex-direction:column;gap:5px;color:#475569;font-size:12px}.filter-panel label:first-child{flex-basis:280px}.filter-panel input,.filter-panel select{width:100%;height:34px;border:1px solid #cbd5e1;border-radius:6px;padding:0 10px;background:#fff;color:#172033;font-size:13px;box-shadow:0 1px 2px rgba(15,23,42,.03)}.filter-panel input:focus,.filter-panel select:focus{border-color:#60a5fa;box-shadow:0 0 0 3px rgba(37,99,235,.1);outline:0}.filter-actions{display:flex;align-items:flex-end;gap:8px}.progress-panel,.table-panel{background:#fff;border:1px solid #e2e8f0;border-radius:8px}.progress-panel{padding:16px;margin-bottom:14px;display:grid;grid-template-columns:1fr auto;gap:8px}.progress-copy{display:flex;flex-direction:column;gap:4px}.progress-copy span,.current{font-size:12px;color:#64748b}.progress-value{color:#2563eb;font-weight:700}.track{grid-column:1/-1;height:6px;background:#e2e8f0;border-radius:4px;overflow:hidden}.track i{display:block;height:100%;background:#2563eb}.current{grid-column:1/-1}.toolbar,.pager{display:flex;align-items:center;justify-content:space-between;padding:13px 16px;color:#64748b;font-size:12px}.toolbar{border-bottom:1px solid #e2e8f0}.pager{border-top:1px solid #e2e8f0}.pager div{display:flex;gap:8px}.table-scroll{overflow:auto}table{width:100%;border-collapse:collapse;min-width:1160px;font-size:12px}th{padding:11px 10px;text-align:left;background:#f8fafc;color:#64748b;border-bottom:1px solid #e2e8f0}td{padding:12px 10px;border-bottom:1px solid #eef2f7;vertical-align:top;color:#334155}td strong,td small{display:block}td small{color:#64748b;margin-top:4px;line-height:1.4;max-width:240px;word-break:break-all}.status{display:inline-flex;padding:3px 8px;border-radius:999px;font-weight:600}.green{background:#dcfce7;color:#166534}.blue{background:#dbeafe;color:#1d4ed8}.amber{background:#fef3c7;color:#92400e}.red{background:#fee2e2;color:#b91c1c}.neutral{background:#f1f5f9;color:#475569}.md5{min-width:150px}.link{font-size:12px;color:#2563eb}.danger-text{color:#b91c1c}.link:disabled{color:#94a3b8;cursor:not-allowed}.empty{text-align:center;padding:36px;color:#94a3b8}@media(max-width:700px){.filter-panel label:first-child{flex-basis:100%}.filter-panel label:not(:first-child){flex:1 1 140px}.filter-actions{width:100%;justify-content:flex-end}}
.header-actions,.batch-actions,.row-actions,.action-button{display:flex;align-items:center}.header-actions,.batch-actions{gap:8px}.action-button{justify-content:center;gap:5px;white-space:nowrap}.row-actions{gap:12px}.priority-text{color:#b45309}@media(max-width:700px){.page-head{align-items:flex-start;gap:12px}.header-actions,.batch-actions{flex-wrap:wrap;justify-content:flex-end}.toolbar{align-items:flex-start;gap:10px}}
.cleanup-button{background:#dc2626;border-color:#dc2626;color:#fff;font-weight:700;box-shadow:0 3px 10px rgba(185,28,28,.28)}.cleanup-button:hover:not(:disabled){background:#b91c1c;border-color:#b91c1c;box-shadow:0 4px 14px rgba(185,28,28,.36)}.cleanup-button:focus-visible{outline:3px solid rgba(248,113,113,.35);outline-offset:2px}
</style>
