<template>
  <AdminLayout active="settings">
    <header class="page-head"><div><h1>系统设置</h1><p>按类别管理服务、AI 模型、入库转码与 TV 展示</p></div></header>
    <div class="settings-shell">
      <aside class="settings-nav">
        <input v-model="search" class="search" placeholder="搜索设置名称、说明或关键词" aria-label="搜索设置" />
        <div v-if="search" class="search-results">
          <button v-for="item in searchResults" :key="item.key" @click="jump(item)">{{ item.label }}<small>{{ item.category }}</small></button>
          <span v-if="!searchResults.length" class="empty">没有匹配设置</span>
        </div>
        <nav v-else>
          <button v-for="item in categories" :key="item.key" :class="{active: section === item.key}" @click="selectSection(item.key)">{{ item.label }}<small>{{ item.description }}</small></button>
        </nav>
      </aside>

      <main class="settings-content">
        <section v-show="section === 'basic'" class="section" id="section-basic">
          <div class="section-head"><h2>基础配置</h2><p>扫描路径和局域网访问相关选项</p></div>
          <SettingRow id="library_watch_enabled" label="源目录自动扫描" hint="关闭后只在仪表盘手动扫描"><Toggle v-model="form.library_watch_enabled" /></SettingRow>
          <SettingRow label="扫描源目录"><span class="readonly">容器内 /source-music</span></SettingRow>
          <SettingRow label="KTV 曲库目录"><span class="readonly">容器内 /music</span></SettingRow>
          <SettingRow id="qr_address" label="二维码展示地址" hint="留空时使用当前访问地址"><input v-model="form.qr_address" class="input" placeholder="192.168.1.10:8080" /></SettingRow>
        </section>

        <section v-show="section === 'ai'" class="section" id="section-ai">
          <div class="section-head"><div><h2>AI 模型</h2><p>支持任意 OpenAI-compatible 服务，未配置时继续使用本地解析</p></div><span class="source-pill" :class="{ok: ai.apiKeyConfigured && ai.enabled}">{{ ai.enabled && ai.apiKeyConfigured ? '服务可用' : '未配置' }}</span></div>
          <div class="ai-status"><span>配置来源：{{ sourceLabel(ai.sources?.bulk_model) }}</span><span>最近测试：{{ ai.lastTestAt ? formatTime(ai.lastTestAt) : '尚未测试' }}</span><span>JSON：{{ ai.jsonMode || 'AUTO' }}</span></div>
          <SettingRow id="ai_enabled" label="启用 AI" hint="扫描、转码和入库不会因 AI 故障中断"><Toggle v-model="aiForm.enabled" /></SettingRow>
          <SettingRow id="ai_base_url" label="API Base URL" hint="完整 API 前缀，例如 https://api.example.com/v1"><input v-model="aiForm.baseUrl" class="input wide" placeholder="https://api.example.com/v1" /></SettingRow>
          <SettingRow id="ai_api_key" label="API Key" hint="只显示配置状态和尾号，留空表示保留现有 Key"><div class="key-control"><input v-model="aiForm.apiKey" class="input wide" type="password" placeholder="已配置时留空即可保留" /><span v-if="ai.apiKeyConfigured" class="key-tail">已配置 · ****{{ ai.apiKeySuffix }}</span><button v-if="ai.apiKeyConfigured" class="text-btn danger" @click="clearKey = !clearKey">{{ clearKey ? '取消清除' : '清除 Key' }}</button></div></SettingRow>
          <SettingRow label="服务预设"><div class="presets"><button v-for="preset in presets" :key="preset.name" class="preset" @click="applyPreset(preset)">{{ preset.name }}</button></div></SettingRow>
          <SettingRow id="ai_bulk_model" label="批量模型 ID" hint="可填写任意兼容服务提供的模型名"><div class="model-control"><input v-model="aiForm.bulkModel" class="input wide" placeholder="例如 deepseek-chat / gpt-4o-mini" /><button class="btn ghost small" @click="loadModels" :disabled="testing">获取模型列表</button></div></SettingRow>
          <div v-if="models.length" class="model-list"><button v-for="model in models" :key="model" @click="aiForm.bulkModel = model">{{ model }}</button></div>
          <SettingRow id="ai_reasoning_model" label="增强模型 ID" hint="留空时所有任务使用批量模型"><input v-model="aiForm.reasoningModel" class="input wide" placeholder="可选" /></SettingRow>
          <SettingRow id="ai_json_mode" label="JSON 模式"><select v-model="aiForm.jsonMode" class="input"><option value="AUTO">自动检测</option><option value="FORCE">强制 response_format</option><option value="PROMPT_ONLY">仅提示词约束</option></select></SettingRow>
          <SettingRow id="ai_timeout" label="请求超时（秒）"><input v-model.number="aiForm.timeoutSeconds" class="input short" type="number" min="5" max="600" /></SettingRow>
          <SettingRow id="ai_threshold" label="自动应用阈值" hint="身份默认 0.97，语种和演唱形式默认 0.92"><div class="thresholds"><label>身份 <input v-model.number="aiForm.identityThreshold" class="input short" type="number" min="0" max="1" step="0.01" /></label><label>分类 <input v-model.number="aiForm.classificationThreshold" class="input short" type="number" min="0" max="1" step="0.01" /></label></div></SettingRow>
          <SettingRow label="并发数"><div class="thresholds"><label>批量 <input v-model.number="aiForm.bulkConcurrency" class="input short" type="number" min="1" max="10" /></label><label>增强 <input v-model.number="aiForm.reasoningConcurrency" class="input short" type="number" min="1" max="5" /></label></div></SettingRow>
          <div class="ai-actions"><button class="btn" @click="testAi" :disabled="testing">{{ testing ? '测试中…' : '测试连接与模型' }}</button><span v-if="testMessage" class="test-message">{{ testMessage }}</span></div>
        </section>

        <section v-show="section === 'transcode'" class="section" id="section-transcode">
          <div class="section-head"><h2>入库与转码</h2><p>自动删源默认关闭，仅作用于管理员手动启动的批量转码</p></div>
          <SettingRow id="delete_source_after_transcode" label="转码成功后删除源文件" hint="启动批量转码时冻结开关，并在确认后逐首校验删除"><Toggle v-model="form.delete_source_after_transcode" /></SettingRow>
          <div class="rule-title"><strong>视频直拷条件</strong><button class="text-btn" @click="restoreTranscodeDefaults">恢复默认</button></div>
          <SettingRow label="容器白名单"><Checks v-model="form.direct_copy_containers" :options="containerOptions" /></SettingRow>
          <SettingRow label="视频编码白名单"><Checks v-model="form.direct_copy_video_codecs" :options="videoOptions" /></SettingRow>
          <SettingRow label="音频编码白名单"><Checks v-model="form.direct_copy_audio_codecs" :options="audioOptions" /></SettingRow>
          <SettingRow label="纯音频文件转码"><Toggle v-model="form.transcode_audio_only" /></SettingRow>
          <SettingRow label="输出容器"><select v-model="form.transcode_output_container" class="input"><option value="mkv">MKV</option><option value="mp4">MP4</option></select></SettingRow>
          <SettingRow label="输出视频编码"><select v-model="form.transcode_video_codec" class="input"><option value="h264">H.264</option><option value="hevc">H.265 / HEVC</option></select></SettingRow>
          <SettingRow label="输出音频编码"><select v-model="form.transcode_audio_codec" class="input"><option value="aac">AAC</option><option value="mp3">MP3</option><option value="opus">Opus</option></select></SettingRow>
          <SettingRow label="硬件加速" :hint="hardwareStatusText"><Toggle v-model="form.transcode_hardware_acceleration" /></SettingRow>
        </section>

        <section v-show="section === 'tv'" class="section" id="section-tv">
          <div class="section-head"><h2>TV 显示</h2><p>待机页、画面比例与防烧屏设置</p></div>
          <SettingRow id="tv_video_scale_mode" label="视频画面模式"><select v-model="form.tv_video_scale_mode" class="input"><option value="zoom">铺满（等比裁切）</option><option value="fit">原画（完整显示）</option><option value="fill">拉伸（充满屏幕）</option></select></SettingRow>
          <SettingRow label="待机热门轮播"><Toggle v-model="form.standby_carousel" /></SettingRow>
          <SettingRow id="standby_source" label="待机内容来源"><select v-model="form.standby_source" class="input"><option value="mixed">热门与新歌混合</option><option value="hot">热门歌曲</option><option value="new">最近入库</option><option value="custom">指定歌曲</option></select></SettingRow>
          <SettingRow v-if="form.standby_source === 'custom'" id="standby_song_ids" label="指定歌曲 ID" hint="用逗号分隔多个歌曲 ID"><input :value="(form.standby_song_ids || []).join(',')" class="input wide" @input="setStandbySongIds($event.target.value)" /></SettingRow>
          <SettingRow label="防烧屏微移"><Toggle v-model="form.anti_burn" /></SettingRow>
          <SettingRow label="播放页迷你二维码"><Toggle v-model="form.mini_qr" /></SettingRow>
          <SettingRow label="待机欢迎语"><input v-model="form.standby_welcome" class="input wide" /></SettingRow>
          <SettingRow label="欢迎语副标题"><textarea v-model="form.standby_subtitle" class="input wide" rows="2"></textarea></SettingRow>
          <SettingRow label="轮播间隔"><input v-model.number="form.standby_interval_sec" class="input short" type="number" min="3" max="60" /></SettingRow>
          <SettingRow id="standby_logo" label="待机 Logo" :hint="form.standby_logo_path ? '已配置自定义 Logo' : '使用默认 Logo'"><label class="upload-btn">上传图片<input type="file" accept="image/png,image/jpeg,image/webp" @change="uploadStandbyLogo" /></label></SettingRow>
        </section>

        <section v-show="section === 'maintenance'" class="section" id="section-maintenance">
          <div class="section-head"><h2>数据维护</h2><p>存量曲库修复、心愿和其他维护入口</p></div>
          <SettingRow label="存量曲库修复" hint="覆盖全部歌曲的歌名、歌手、语种、演唱形式、年代和主题"><router-link class="btn ghost small" :to="{name:'admin-ai'}">前往 AI 曲库</router-link></SettingRow>
          <SettingRow label="未处理心愿"><span class="readonly">{{ wishes.length }} 条</span></SettingRow>
          <SettingRow label="关于"><span class="readonly">home-ktv v0.1.0 · 家庭局域网自用</span></SettingRow>
        </section>
      </main>
    </div>
    <div v-if="dirty" class="save-bar"><span>有未保存的修改</span><button class="btn ghost" @click="resetChanges">放弃修改</button><button class="btn" @click="saveAll">保存设置</button></div>
  </AdminLayout>
</template>

<script setup>
import { computed, h, onBeforeUnmount, onMounted, reactive, ref, watch, nextTick } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import api from '../../api/client'
import AdminLayout from './AdminLayout.vue'
import { alertDialog, confirmDialog } from '../../composables/useDialog'

const route = useRoute(); const router = useRouter()
const categories = [
  { key: 'basic', label: '基础配置', description: '路径与访问' },
  { key: 'ai', label: 'AI 模型', description: '服务与能力' },
  { key: 'transcode', label: '入库与转码', description: '格式与源文件' },
  { key: 'tv', label: 'TV 显示', description: '待机与播放' },
  { key: 'maintenance', label: '数据维护', description: '修复与清理' }
]
const search = ref(''); const section = ref(route.query.section && categories.some(x => x.key === route.query.section) ? route.query.section : 'basic')
const form = reactive({ library_watch_enabled:false, qr_address:'', delete_source_after_transcode:false, tv_video_scale_mode:'zoom', standby_carousel:true, standby_source:'mixed', standby_song_ids:[], standby_logo_path:'', anti_burn:true, mini_qr:true, standby_welcome:'今晚开唱', standby_subtitle:'手机点歌，电视欢唱\n一家人的客厅 KTV', standby_interval_sec:8, direct_copy_containers:['mp4','m4v','mkv'], direct_copy_video_codecs:['h264','hevc'], direct_copy_audio_codecs:['aac','mp3'], transcode_audio_only:false, transcode_output_container:'mkv', transcode_video_codec:'h264', transcode_audio_codec:'aac', transcode_hardware_acceleration:false })
const ai = reactive({ enabled:false, apiKeyConfigured:false, apiKeySuffix:null, sources:{}, capabilities:{}, lastTestAt:null })
const aiForm = reactive({ enabled:false, baseUrl:'', apiKey:'', bulkModel:'', reasoningModel:'', timeoutSeconds:60, identityThreshold:.97, classificationThreshold:.92, jsonMode:'AUTO', bulkConcurrency:2, reasoningConcurrency:1 })
const original = ref(''); const aiOriginal = ref(''); const dirty = ref(false); const loading = ref(true); const testing = ref(false); const testMessage = ref(''); const models = ref([]); const wishes = ref([]); const clearKey = ref(false)
const hardware = reactive({ available:false, reason:'尚未检测' })
const presets = [{name:'DeepSeek',baseUrl:'https://api.deepseek.com/v1'},{name:'OpenAI',baseUrl:'https://api.openai.com/v1'},{name:'Ollama',baseUrl:'http://localhost:11434/v1'},{name:'自定义',baseUrl:''}]
const containerOptions=['mp4','m4v','mkv','mov','ts','m2ts','mts','mpg','mpeg','vob','avi','webm','wmv','asf','flv','f4v','3gp','3g2','rm','rmvb'].map(value=>({value,label:value.toUpperCase()}))
const videoOptions=['h264','hevc','mpeg2video','mpeg4','vp8','vp9','av1','vc1','wmv3','wmv2','theora','prores','dnxhd','mjpeg','dvvideo','h263','rawvideo'].map(value=>({value,label:value.toUpperCase()}))
const audioOptions=['aac','mp3','mp2','ac3','eac3','dts','truehd','flac','alac','opus','vorbis','ape','wmav1','wmav2','pcm_s16le','pcm_s24le','pcm_s32le','pcm_f32le'].map(value=>({value,label:value.toUpperCase()}))
const SettingRow = { props:['id','label','hint'], setup(props,{slots}) { return () => h('div',{id:props.id,class:'setting-row'},[h('div',{class:'setting-label'},[h('strong',props.label),props.hint?h('small',props.hint):null]),h('div',{class:'setting-value'},slots.default?.())]) } }
const Toggle = { props:['modelValue'], emits:['update:modelValue'], setup(props,{emit}) { return () => h('button',{type:'button',class:['switch',props.modelValue?'on':''],onClick:()=>emit('update:modelValue',!props.modelValue),'aria-label':props.modelValue?'已开启':'已关闭'},[h('span')]) } }
const Checks = { props:['modelValue','options'], emits:['update:modelValue'], setup(props,{emit}) { return () => h('div',{class:'checks'},props.options.map(item=>h('label',{key:item.value},[h('input',{type:'checkbox',checked:props.modelValue.includes(item.value),onChange:e=>emit('update:modelValue',e.target.checked?[...props.modelValue,item.value]:props.modelValue.filter(v=>v!==item.value))}),item.label]))) } }
const searchItems = computed(() => categories.flatMap(c => ({basic:['library_watch_enabled','qr_address'],ai:['ai_enabled','ai_base_url','ai_api_key','ai_bulk_model','ai_reasoning_model','ai_json_mode','ai_timeout','ai_threshold'],transcode:['delete_source_after_transcode'],tv:['tv_video_scale_mode','standby_source','standby_song_ids','standby_logo'],maintenance:['repair']}[c.key]||[]).map(key=>({key,label:key==='repair'?'存量曲库修复':key.replaceAll('_',' '),category:c.label,description:c.description}))))
const searchResults = computed(() => { const q=search.value.trim().toLowerCase(); return q?searchItems.value.filter(x=>(x.label+' '+x.description+' '+x.key).toLowerCase().includes(q)):[] })
const hardwareStatusText = computed(() => hardware.available ? '硬件编码可用' : (hardware.reason || '未检测到硬件编码器'))
function snapshot(v){ return JSON.stringify(v) }
function selectSection(value){ section.value=value; router.replace({query:{...route.query,section:value}}) }
function jump(item){ selectSection(item.category==='AI 模型'?'ai':item.category==='入库与转码'?'transcode':item.category==='TV 显示'?'tv':item.category==='数据维护'?'maintenance':'basic'); nextTick(()=>document.getElementById(item.key)?.scrollIntoView({behavior:'smooth',block:'center'})) }
function sourceLabel(value){ return value==='DATABASE'?'管理后台':value==='ENVIRONMENT'?'环境变量':value==='NONE'?'未配置':'默认值' }
function formatTime(value){ return value ? new Date(value).toLocaleString('zh-CN',{hour12:false}) : '' }
function applyPreset(p){ if(p.baseUrl) aiForm.baseUrl=p.baseUrl }
async function load(){ loading.value=true; const [settings,config,hw,w] = await Promise.all([api.adminGetSettings().catch(()=>({})),api.adminAiConfig().catch(()=>({})),api.adminTranscodeHardware().catch(e=>({reason:e.message})),api.adminWishes().catch(()=>[])]); Object.assign(form,settings); Object.assign(ai,config); Object.assign(aiForm,{enabled:config.enabled,baseUrl:config.baseUrl,bulkModel:config.bulkModel,reasoningModel:config.reasoningModel,timeoutSeconds:config.timeoutSeconds,identityThreshold:config.identityThreshold,classificationThreshold:config.classificationThreshold,jsonMode:config.jsonMode||'AUTO',bulkConcurrency:config.bulkConcurrency||2,reasoningConcurrency:config.reasoningConcurrency||1}); Object.assign(hardware,hw); wishes.value=w; original.value=snapshot(form); aiOriginal.value=snapshot(aiForm); dirty.value=false; loading.value=false }
watch([form,aiForm],()=>{ if(!loading.value) dirty.value=snapshot(form)!==original.value||snapshot(aiForm)!==aiOriginal.value },{deep:true})
async function saveAll(){ try { const updated=await api.adminPutSettings({...form}); Object.assign(form,updated); const config=await api.adminAiPutConfig({...aiForm,apiKey:aiForm.apiKey||null,clearApiKey:clearKey.value}); Object.assign(ai,config); aiForm.apiKey=''; clearKey.value=false; original.value=snapshot(form); aiOriginal.value=snapshot(aiForm); dirty.value=false } catch(e){ await alertDialog(e.message||'保存失败') } }
function resetChanges(){ Object.assign(form,JSON.parse(original.value)); Object.assign(aiForm,JSON.parse(aiOriginal.value)); dirty.value=false }
async function loadModels(){ try { models.value=(await api.adminAiModels()).models||[] } catch(e){ await alertDialog(e.message||'模型列表获取失败') } }
async function testAi(){ testing.value=true; try { const result=await api.adminAiTestConfig(); testMessage.value=`连接成功，已检测 ${Object.keys(result.testedModels||{}).length} 个模型`; Object.assign(ai,await api.adminAiConfig()) } catch(e){ testMessage.value=e.message||'连接测试失败' } finally { testing.value=false } }
async function restoreTranscodeDefaults(){ if(!await confirmDialog('恢复转码默认规则？',{title:'恢复默认'}))return; const result=await api.adminResetTranscodeDefaults(); Object.assign(form,result) }
function setStandbySongIds(value){ form.standby_song_ids=[...new Set(value.split(/[,，\s]+/).map(Number).filter(id=>Number.isInteger(id)&&id>0))] }
async function uploadStandbyLogo(event){ const file=event.target.files?.[0]; if(!file)return; try{await api.adminUploadStandbyLogo(file); Object.assign(form,await api.adminGetSettings()); original.value=snapshot(form)}catch(e){await alertDialog(e.message||'Logo 上传失败')}finally{event.target.value=''} }
onMounted(load)
function beforeUnload(e){ if(dirty.value){e.preventDefault();e.returnValue=''} }
onMounted(()=>window.addEventListener('beforeunload',beforeUnload)); onBeforeUnmount(()=>window.removeEventListener('beforeunload',beforeUnload))
onBeforeRouteLeave(async()=>{ if(!dirty.value)return true; return await confirmDialog('有未保存的设置，确定离开吗？',{title:'未保存修改',tone:'warning'}) })
</script>

<style scoped>
.page-head{margin-bottom:18px}.page-head h1{font-size:22px;color:#172033}.page-head p{margin-top:6px;color:#64748b;font-size:13px}.settings-shell{display:grid;grid-template-columns:220px minmax(0,820px);gap:18px;align-items:start}.settings-nav{position:sticky;top:18px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:10px}.search{width:100%;border:1px solid #cbd5e1;border-radius:6px;padding:9px 10px;font-size:12px}.settings-nav nav{display:flex;flex-direction:column;gap:3px;margin-top:10px}.settings-nav nav button,.search-results button{display:flex;flex-direction:column;align-items:flex-start;text-align:left;padding:11px 10px;border-radius:6px;color:#475569;font-size:13px}.settings-nav button small{margin-top:4px;color:#94a3b8;font-size:11px}.settings-nav button.active{background:#eff6ff;color:#1d4ed8;font-weight:600}.search-results{margin-top:10px;display:flex;flex-direction:column;gap:2px}.settings-nav button:hover{background:#f8fafc}.empty{padding:12px;color:#94a3b8;font-size:12px}.settings-content{min-width:0}.section{background:#fff;border:1px solid #e2e8f0;border-radius:8px;padding:20px;margin-bottom:18px}.section-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:12px}.section-head h2{font-size:17px;color:#172033}.section-head p{margin-top:5px;color:#64748b;font-size:12px}.source-pill{padding:4px 9px;border-radius:12px;background:#f1f5f9;color:#64748b;font-size:11px}.source-pill.ok{background:#dcfce7;color:#166534}.ai-status{display:flex;gap:18px;flex-wrap:wrap;padding:10px 12px;margin-bottom:8px;background:#f8fafc;border:1px solid #e2e8f0;color:#64748b;font-size:11px}.setting-row{display:flex;align-items:center;justify-content:space-between;gap:18px;min-height:58px;padding:10px 0;border-top:1px solid #eef2f7}.setting-label{min-width:190px}.setting-label strong,.setting-label small{display:block}.setting-label strong{font-size:13px;color:#334155}.setting-label small{margin-top:4px;color:#94a3b8;font-size:11px;line-height:1.4}.setting-value{display:flex;justify-content:flex-end;align-items:center;gap:10px;min-width:0;flex:1}.input{width:220px;padding:8px 10px;border:1px solid #cbd5e1;border-radius:6px;background:#fff;color:#172033;font-size:13px}.input.wide{width:min(420px,100%)}.input.short{width:82px}.readonly{color:#64748b;font-size:12px}.switch{position:relative;width:42px;height:24px;border-radius:12px;background:#cbd5e1;flex:none}.switch span{position:absolute;top:3px;left:3px;width:18px;height:18px;border-radius:50%;background:#fff;transition:.2s}.switch.on{background:#2563eb}.switch.on span{left:21px}.key-control,.model-control,.presets,.thresholds{display:flex;align-items:center;gap:9px;flex-wrap:wrap}.key-tail{font-size:11px;color:#15803d}.btn.small{padding:7px 11px;font-size:11px}.text-btn{color:#2563eb;font-size:12px}.text-btn.danger{color:#b91c1c}.preset,.model-list button{padding:6px 10px;border:1px solid #cbd5e1;border-radius:5px;color:#475569;font-size:11px;background:#fff}.preset:hover,.model-list button:hover{border-color:#2563eb;color:#2563eb}.model-list{display:flex;gap:6px;flex-wrap:wrap;padding:0 0 10px 208px}.checks{display:flex;flex-wrap:wrap;gap:9px 16px}.checks label{display:flex;gap:5px;align-items:center;color:#475569;font-size:11px}.rule-title{display:flex;justify-content:space-between;align-items:center;margin-top:12px;padding:10px 0;border-top:1px solid #eef2f7;color:#334155;font-size:13px}.ai-actions{display:flex;align-items:center;gap:12px;margin-top:14px}.test-message{font-size:12px;color:#15803d}.upload-btn{display:inline-flex;align-items:center;padding:7px 11px;border:1px solid #cbd5e1;border-radius:6px;color:#2563eb;font-size:12px;cursor:pointer}.upload-btn input{display:none}.save-bar{position:sticky;bottom:12px;z-index:5;display:flex;align-items:center;justify-content:flex-end;gap:12px;padding:11px 14px;background:#172033;color:#fff;border-radius:8px;box-shadow:0 8px 28px rgba(15,23,42,.25)}.save-bar span{margin-right:auto;font-size:12px}.save-bar .btn{background:#2563eb;color:#fff}.save-bar .btn.ghost{background:transparent;border:1px solid #64748b}@media(max-width:800px){.settings-shell{grid-template-columns:1fr}.settings-nav{position:static}.settings-nav nav{display:grid;grid-template-columns:repeat(2,1fr)}.setting-row{align-items:flex-start;flex-direction:column;gap:8px}.setting-value{width:100%;justify-content:flex-start}.setting-label{min-width:0}.model-list{padding-left:0}.section{padding:15px}}
</style>
