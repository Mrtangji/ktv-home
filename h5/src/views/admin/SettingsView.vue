<template>
  <AdminLayout active="settings">
    <header class="page-head"><h1>系统设置</h1><p>配置访问地址、TV 展示与基础维护选项</p></header>
    <div class="grp">基础配置</div>
    <div class="row"><span class="k">扫描源目录</span><span class="v dim">容器内 /source-music（只读，改路径请改 compose）</span></div>
    <div class="row"><span class="k">KTV 曲库目录</span><span class="v dim">容器内 /music（TV 实际播放目录）</span></div>
    <div class="row">
      <span class="k">二维码展示地址</span>
      <input class="v-input" v-model="s.qr_address" placeholder="192.168.1.10:8080" />
    </div>

    <div class="grp transcode-head"><span>视频直拷条件</span><button class="btn ghost sm" @click="restoreTranscodeDefaults">恢复默认值</button></div>
    <div class="rule-summary"><strong>三项必须同时满足才会直接入库</strong><span>容器匹配 + 视频编码匹配 + 音频编码匹配；任意一项不匹配就需要转码。</span></div>
    <div class="row column-row"><span class="k">容器白名单</span><div class="checks"><label v-for="item in containerOptions" :key="item.value"><input v-model="s.direct_copy_containers" type="checkbox" :value="item.value" />{{ item.label }}</label></div></div>
    <div class="row column-row"><span class="k">视频编码白名单</span><div class="checks"><label v-for="item in videoOptions" :key="item.value"><input v-model="s.direct_copy_video_codecs" type="checkbox" :value="item.value" />{{ item.label }}</label></div></div>
    <div class="row column-row"><span class="k">音频编码白名单</span><div class="checks"><label v-for="item in audioOptions" :key="item.value"><input v-model="s.direct_copy_audio_codecs" type="checkbox" :value="item.value" />{{ item.label }}</label></div></div>
    <div class="row"><span class="k">纯音频文件转码</span><Toggle v-model="s.transcode_audio_only" /></div>
    <div class="row"><span class="k">输出容器</span><select class="v-input" v-model="s.transcode_output_container"><option value="mkv">MKV</option><option value="mp4">MP4</option></select></div>
    <div class="row"><span class="k">输出视频编码</span><select class="v-input" v-model="s.transcode_video_codec"><option value="h264">H.264</option><option value="hevc">H.265 / HEVC</option></select></div>
    <div class="row"><span class="k">输出音频编码</span><select class="v-input" v-model="s.transcode_audio_codec"><option value="aac">AAC</option><option value="mp3">MP3</option><option value="opus">Opus</option></select></div>
    <div class="row"><span class="k">硬件加速</span><Toggle :model-value="s.transcode_hardware_acceleration" @update:model-value="toggleHardware" /><span class="hardware-state" :class="hardwareStatus.available ? 'available' : 'dim'">{{ hardwareStatusText }}</span></div>

    <div class="grp">TV 显示设置</div>
    <div class="row"><span class="k">待机热门轮播</span><Toggle v-model="s.standby_carousel" /></div>
    <div class="row"><span class="k">防烧屏微移</span><Toggle v-model="s.anti_burn" /></div>
    <div class="row"><span class="k">播放页迷你二维码</span><Toggle v-model="s.mini_qr" /></div>
    <div class="row"><span class="k">待机欢迎语</span><input class="v-input wide" v-model="s.standby_welcome" placeholder="今晚开唱" /></div>
    <div class="row"><span class="k">欢迎语副标题</span><textarea class="v-input wide" v-model="s.standby_subtitle" rows="2" placeholder="手机点歌，电视欢唱" /></div>
    <div class="row"><span class="k">轮播内容</span><select class="v-input" v-model="s.standby_source"><option value="mixed">热门 + 新歌</option><option value="hot">热门歌曲</option><option value="new">最近入库</option><option value="custom">自选歌曲</option></select></div>
    <div v-if="s.standby_source === 'custom'" class="row"><span class="k">自选歌曲 ID</span><input class="v-input wide" v-model="customSongIds" placeholder="例如 12, 35, 108" /><span class="hint">按顺序轮播</span></div>
    <div class="row"><span class="k">轮播间隔</span><input class="v-input short" v-model.number="s.standby_interval_sec" type="number" min="3" max="60" /><span class="v dim">秒</span></div>
    <div class="row logo-row"><span class="k">待机 Logo</span><div class="logo-preview" :style="logoStyle"><span v-if="!content.logoUrl">🎤</span></div><label class="upload">上传 Logo<input type="file" accept="image/jpeg,image/png,image/webp" @change="uploadLogo" /></label></div>

    <div class="grp">数据与维护</div>
    <div class="row"><span class="k">心愿单</span><span class="v">{{ wishes.length }} 条未处理</span>
      <button class="btn ghost sm" @click="showWishes = !showWishes">{{ showWishes ? '收起' : '查看' }}</button></div>
    <div v-if="showWishes" class="wishes">
      <div v-for="w in wishes" :key="w.id" class="wish">🎵 {{ w.keyword }}</div>
      <div v-if="!wishes.length" class="dim">暂无心愿</div>
    </div>
    <div class="row"><span class="k">关于</span><span class="v dim">home-ktv v0.1.0 · 家庭局域网自用</span></div>

    <div class="save-bar">
      <button class="btn" @click="save">{{ saved ? '已保存 ✓' : '保存设置' }}</button>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted, h, computed } from 'vue'
import api from '../../api/client'
import AdminLayout from './AdminLayout.vue'
import { alertDialog, confirmDialog } from '../../composables/useDialog'

// 内联开关组件
const Toggle = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () => h('div', {
      class: ['switch', props.modelValue ? 'on' : ''],
      onClick: () => emit('update:modelValue', !props.modelValue)
    })
  }
}

const transcodeDefaults = { direct_copy_containers:['mp4','m4v','mkv'], direct_copy_video_codecs:['h264','hevc'], direct_copy_audio_codecs:['aac','mp3'], transcode_audio_only:false, transcode_output_container:'mkv', transcode_video_codec:'h264', transcode_audio_codec:'aac', transcode_hardware_acceleration:false }
const s = reactive({ qr_address: '', standby_carousel: true, anti_burn: true, mini_qr: true, standby_welcome: '今晚开唱', standby_subtitle: '手机点歌，电视欢唱\n一家人的客厅 KTV', standby_source: 'mixed', standby_song_ids: [], standby_interval_sec: 8, transcode_hardware_auto_configured: false, ...transcodeDefaults })
const containerOptions=[
  {value:'mp4',label:'MP4'},{value:'m4v',label:'M4V'},{value:'mkv',label:'MKV'},{value:'mov',label:'MOV'},
  {value:'ts',label:'TS'},{value:'m2ts',label:'M2TS'},{value:'mts',label:'MTS'},{value:'mpg',label:'MPG'},
  {value:'mpeg',label:'MPEG'},{value:'vob',label:'VOB'},{value:'avi',label:'AVI'},{value:'webm',label:'WebM'},
  {value:'wmv',label:'WMV'},{value:'asf',label:'ASF'},{value:'flv',label:'FLV'},{value:'f4v',label:'F4V'},
  {value:'3gp',label:'3GP'},{value:'3g2',label:'3G2'},{value:'rm',label:'RM'},{value:'rmvb',label:'RMVB'}
]
const videoOptions=[
  {value:'h264',label:'H.264 / AVC'},{value:'hevc',label:'H.265 / HEVC'},{value:'mpeg2video',label:'MPEG-2 Video'},
  {value:'mpeg4',label:'MPEG-4 Part 2'},{value:'vp8',label:'VP8'},{value:'vp9',label:'VP9'},{value:'av1',label:'AV1'},
  {value:'vc1',label:'VC-1'},{value:'wmv3',label:'WMV9'},{value:'wmv2',label:'WMV8'},{value:'theora',label:'Theora'},
  {value:'prores',label:'Apple ProRes'},{value:'dnxhd',label:'DNxHD / DNxHR'},{value:'mjpeg',label:'Motion JPEG'},
  {value:'dvvideo',label:'DV Video'},{value:'h263',label:'H.263'},{value:'rawvideo',label:'Raw Video'}
]
const audioOptions=[
  {value:'aac',label:'AAC'},{value:'mp3',label:'MP3'},{value:'mp2',label:'MP2'},{value:'ac3',label:'AC-3'},
  {value:'eac3',label:'E-AC-3'},{value:'dts',label:'DTS'},{value:'truehd',label:'Dolby TrueHD'},{value:'flac',label:'FLAC'},
  {value:'alac',label:'ALAC'},{value:'opus',label:'Opus'},{value:'vorbis',label:'Vorbis'},{value:'ape',label:'Monkey’s Audio'},
  {value:'wmav1',label:'WMA v1'},{value:'wmav2',label:'WMA v2'},{value:'pcm_s16le',label:'PCM 16-bit'},
  {value:'pcm_s24le',label:'PCM 24-bit'},{value:'pcm_s32le',label:'PCM 32-bit'},{value:'pcm_f32le',label:'PCM Float 32-bit'}
]
const content = reactive({ logoUrl: null })
const customSongIds = ref('')
const wishes = ref([])
const showWishes = ref(false)
const saved = ref(false)
const hardwareStatus = reactive({ available:false, vendor:null, device:null, supportedCodecs:[], reason:'尚未检测' })
const hardwareStatusText = computed(() => hardwareStatus.available
  ? `${hardwareStatus.vendor} · ${(hardwareStatus.supportedCodecs||[]).map(x=>x.toUpperCase()).join(' / ')}`
  : hardwareStatus.reason || '未发现设备')

onMounted(async () => {
  const [loaded, detected] = await Promise.all([
    api.adminGetSettings().catch(() => ({})),
    api.adminTranscodeHardware().catch(e => ({ available:false, reason:e.message||'硬件检测失败' }))
  ])
  Object.assign(s, loaded)
  Object.assign(hardwareStatus, detected)
  if (s.transcode_hardware_acceleration && !hardwareStatus.available) s.transcode_hardware_acceleration = false
  const hardwareSupportsCodec = hardwareStatus.available && (hardwareStatus.supportedCodecs || []).includes(s.transcode_video_codec)
  if (hardwareSupportsCodec && !s.transcode_hardware_auto_configured) {
    try {
      const updated = await api.adminPutSettings({
        transcode_hardware_acceleration: true,
        transcode_hardware_auto_configured: true
      })
      Object.assign(s, updated)
    } catch {
      s.transcode_hardware_acceleration = false
    }
  }
  customSongIds.value = Array.isArray(loaded.standby_song_ids) ? loaded.standby_song_ids.join(', ') : ''
  Object.assign(content, await api.standbyContent().catch(() => ({})))
  wishes.value = await api.adminWishes().catch(() => [])
})

async function save() {
  try {
    s.standby_song_ids = customSongIds.value.split(/[,，\s]+/).map(Number).filter(Number.isFinite)
    await api.adminPutSettings({ ...s })
    saved.value = true
    setTimeout(() => { saved.value = false }, 1500)
  } catch (e) {
    if (['HARDWARE_ACCELERATOR_NOT_FOUND','HARDWARE_CODEC_UNAVAILABLE'].includes(e.code)) s.transcode_hardware_acceleration = false
    await alertDialog(e.message || '保存失败')
  }
}
async function toggleHardware(enabled) {
  s.transcode_hardware_auto_configured = true
  if (!enabled) { s.transcode_hardware_acceleration = false; return }
  const detected = await api.adminTranscodeHardware().catch(e => ({ available:false, reason:e.message||'硬件检测失败' }))
  Object.assign(hardwareStatus, detected)
  if (!detected.available || !(detected.supportedCodecs||[]).includes(s.transcode_video_codec)) {
    s.transcode_hardware_acceleration = false
    await alertDialog(detected.reason || `当前设备不支持 ${s.transcode_video_codec.toUpperCase()} 硬件编码`, { title: '硬件加速不可用' })
    return
  }
  s.transcode_hardware_acceleration = true
}
async function restoreTranscodeDefaults() {
  if (!await confirmDialog('仅恢复视频直拷和转码规则，其他系统设置不会变化。', { title: '恢复默认转码规则' })) return
  try { Object.assign(s, transcodeDefaults, await api.adminResetTranscodeDefaults()); saved.value = true; setTimeout(() => { saved.value = false }, 1500) }
  catch (e) { await alertDialog(e.message || '恢复默认值失败') }
}
async function uploadLogo(event) {
  const file = event.target.files?.[0]
  if (!file) return
  try { Object.assign(content, await api.adminUploadStandbyLogo(file)); saved.value = true; setTimeout(() => { saved.value = false }, 1500) }
  catch (error) { await alertDialog(error.message || 'Logo 上传失败') }
  event.target.value = ''
}
const logoStyle = computed(() => content.logoUrl ? { backgroundImage: `url(${content.logoUrl})` } : {})
</script>

<style scoped>
.page-head { margin-bottom:18px; }.page-head h1 { font-size:22px; color:#172033; }.page-head p { color:#64748b; font-size:13px; margin-top:6px; }
.grp { max-width:760px; font-size: 13px; font-weight:700; color:#334155; margin: 20px 0 8px; padding:12px 14px; background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px 8px 0 0; }
.grp:first-child { margin-top: 0; }
.row { display: flex; align-items: center; gap: 14px; min-height:54px; padding: 10px 14px; border:1px solid #e2e8f0; border-top:0; background:#fff; font-size: 13px; max-width:760px; }
.k { width: 170px; color:#334155; font-weight:600; flex: none; }
.v { color:#172033; }.v.dim { color:#64748b; }
.v-input { background:#fff; border:1px solid #cbd5e1; border-radius:6px; padding:8px 12px; color:#172033; font-size:13px; width:220px; }
.v-input.wide { width: 360px; }.v-input.short { width: 70px; }.hint { color:#94a3b8; font-size: 11px; }.logo-preview { width: 84px; height: 52px; border-radius:6px; background:#f8fafc; background-size: contain; background-position: center; background-repeat: no-repeat; display: grid; place-items: center; font-size: 24px; border:1px solid #e2e8f0; }.upload { color:#2563eb; cursor: pointer; font-size:12px; }.upload input { display: none; }
.sm { padding: 6px 14px; font-size: 12px; margin-left: auto; }
:deep(.switch) { width: 42px; height: 24px; border-radius: 12px; background:#cbd5e1; position: relative; cursor: pointer; transition: var(--transition); }
:deep(.switch::after) { content: ""; position: absolute; top: 3px; left: 3px; width: 18px; height: 18px; border-radius: 50%; background:#fff; transition: var(--transition); box-shadow:0 1px 3px rgba(15,23,42,.2); }
:deep(.switch.on) { background:#2563eb; }:deep(.switch.on::after) { left: 21px; background:#fff; }
.wishes { max-width:760px; padding:12px 14px; background:#fff; border:1px solid #e2e8f0; border-top:0; }.wish { font-size:13px; padding:6px 0; color:#64748b; }.dim { color:#94a3b8; font-size:12px; }
.save-bar { margin-top: 20px; max-width:760px; }.save-bar .btn { background:#2563eb; color:#fff; border-radius:6px; box-shadow:none; }
.transcode-head{display:flex;align-items:center;justify-content:space-between}.transcode-head .sm{margin-left:0}.column-row{align-items:flex-start}.checks{display:flex;flex-wrap:wrap;gap:8px 18px;padding:3px 0}.checks label{display:flex;align-items:center;gap:6px;color:#475569;min-width:110px}.checks input{accent-color:#2563eb}
.hardware-state{font-size:12px}.hardware-state.available{color:#15803d}
.rule-summary{display:flex;flex-direction:column;gap:4px;max-width:760px;padding:11px 14px;background:#eff6ff;border:1px solid #bfdbfe;border-top:0;color:#1e3a8a;font-size:12px}.rule-summary strong{font-size:13px}.rule-summary span{color:#475569}
</style>
