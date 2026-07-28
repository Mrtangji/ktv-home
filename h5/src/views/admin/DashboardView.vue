<template>
  <AdminLayout active="dashboard">
    <header class="page-head"><div><h1>仪表盘</h1><p>扫描源路径并查看曲库、转码与播放服务状态</p></div><button class="primary" :disabled="scanning" @click="scan">{{ scanning ? '扫描中…' : '扫描源路径' }}</button></header>
    <section class="stats"><article><span>原始素材</span><strong>{{ sourceTotal }}</strong><small>/source-music</small></article><article><span>KTV曲库</span><strong>{{ d.totalSongs ?? 0 }}</strong><small>/music，可点歌</small></article><article><span>待转码</span><strong>{{ pendingCount }}</strong><small>等待批量转码入库</small></article><article><span>未识别</span><strong>{{ d.unrecognizedCount ?? 0 }}</strong><small>需补录元数据</small></article></section>
    <section v-if="scanResult" class="notice"><strong>扫描完成</strong><span>扫描 {{ scanResult.scanned }} 个文件，自动直拷 {{ scanResult.copied }} 个，待转码 {{ scanResult.pendingTranscode }} 个，源 MD5 重复 {{ scanResult.skippedSourceDuplicate }} 个，输出 MD5 重复 {{ scanResult.skippedOutputDuplicate }} 个，未识别 {{ scanResult.unrecognized }} 个，失败 {{ scanResult.failed }} 个。</span></section>
    <section class="panel"><div class="panel-head"><strong>运行状态</strong><button class="text-btn" @click="load">刷新</button></div><table><thead><tr><th>模块</th><th>当前状态</th><th>详情</th><th>操作</th></tr></thead><tbody>
      <tr><td><strong>源路径扫描</strong><small>分析、去重、自动直拷</small></td><td><span class="status green">{{ scanning ? '扫描中' : '就绪' }}</span></td><td>需转码文件只进入待处理列表，不会在扫描时自动转码。</td><td><button class="link" @click="scan" :disabled="scanning">重新扫描</button></td></tr>
      <tr><td><strong>批量转码</strong><small>原始音乐管理任务</small></td><td><span class="status" :class="progress.running?'blue':'neutral'">{{ progress.running ? '进行中' : '空闲' }}</span></td><td>{{ progress.running ? `${progress.completed}/${progress.total}，当前：${progress.currentFile || '准备中'}` : lastProgressText }}</td><td><router-link class="link" :to="{name:'admin-source-library'}">查看进度</router-link></td></tr>
      <tr><td><strong>播放服务</strong><small>TV 与手机点歌</small></td><td><span class="status green">{{ queueState }}</span></td><td>当前连接 {{ d.connectedClients ?? 0 }} 台客户端，正式曲库 KTV {{ d.ktvCount||0 }} / MV {{ d.mvCount||0 }} / 音频 {{ d.audioCount||0 }}。</td><td><router-link class="link" :to="{name:'admin-ktv-library'}">管理曲库</router-link></td></tr>
    </tbody></table></section>
  </AdminLayout>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../../api/client'
import AdminLayout from './AdminLayout.vue'
import { alertDialog } from '../../composables/useDialog'
const d=ref({}),queue=ref({}),progress=ref({}),scanning=ref(false),scanResult=ref(null),sourceTotal=ref(0),pendingCount=ref(0)
const queueState=computed(()=>({playing:'播放中',paused:'已暂停',idle:'空闲'}[queue.value.state]||'空闲'))
const lastProgressText=computed(()=>progress.value.finishedAt?`上次完成：成功 ${progress.value.transcoded||0}，失败 ${progress.value.failed||0}`:'暂无批量转码记录')
async function load(){const [status,q,p,sources,pending]=await Promise.all([api.adminStatus().catch(()=>({})),api.getQueue().catch(()=>({})),api.adminSourceTranscodeProgress().catch(()=>({})),api.adminSourceLibrary({page:0,size:1}).catch(()=>({})),api.adminSourceLibrary({status:'pending',page:0,size:1}).catch(()=>({}))]);d.value=status;queue.value=q;progress.value=p;sourceTotal.value=sources.total||0;pendingCount.value=pending.total||0}
async function scan(){scanning.value=true;try{const r=await api.adminScan();scanResult.value=r.sourceScan||null;await load()}catch(e){await alertDialog(e.message||'扫描失败')}finally{scanning.value=false}}
onMounted(load)
</script>
<style scoped>
.page-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px}.page-head h1{font-size:22px}.page-head p{color:#64748b;font-size:13px;margin-top:6px}.primary{height:36px;padding:0 15px;border-radius:6px;background:#2563eb;color:#fff;font-size:13px}.primary:disabled{opacity:.5}.stats{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:14px}.stats article{background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:16px}.stats span,.stats small{display:block;color:#64748b;font-size:12px}.stats strong{display:block;font-size:26px;margin:8px 0 6px}.stats small{color:#94a3b8}.notice{display:flex;gap:14px;padding:13px 16px;background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;margin-bottom:14px;font-size:12px;color:#1e40af}.panel{background:#fff;border:1px solid #e2e8f0;border-radius:8px}.panel-head{display:flex;justify-content:space-between;padding:14px 16px;border-bottom:1px solid #e2e8f0}.text-btn,.link{color:#2563eb;font-size:12px}.link:disabled{color:#94a3b8}table{width:100%;border-collapse:collapse;font-size:12px}th{padding:11px 14px;text-align:left;background:#f8fafc;color:#64748b}td{padding:13px 14px;border-top:1px solid #eef2f7;color:#475569}td strong,td small{display:block}td small{color:#94a3b8;margin-top:4px}.status{display:inline-flex;padding:3px 8px;border-radius:999px;font-weight:600}.green{background:#dcfce7;color:#166534}.blue{background:#dbeafe;color:#1d4ed8}.neutral{background:#f1f5f9;color:#475569}@media(max-width:900px){.stats{grid-template-columns:1fr 1fr}.panel{overflow:auto}table{min-width:760px}}
</style>
