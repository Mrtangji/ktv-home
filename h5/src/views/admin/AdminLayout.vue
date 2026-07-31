<template>
  <div class="admin-shell">
    <aside class="side">
      <!-- 品牌标识区 / Brand identity area -->
      <div class="brand">
        <strong>家庭KTV</strong>
        <span>管理后台</span>
      </div>
      <!-- 侧边栏导航菜单 / Sidebar navigation menu -->
      <nav>
        <div class="nav-label">菜单导航</div>
        <router-link class="mi" :class="{ on: active === 'dashboard' }" :to="{ name: 'admin-dashboard' }"><span>▦</span>仪表盘</router-link>
        <router-link class="mi" :class="{ on: active === 'source' }" :to="{ name: 'admin-source-library' }"><span>▤</span>原始音乐管理</router-link>
        <router-link class="mi" :class="{ on: active === 'ktv' }" :to="{ name: 'admin-ktv-library' }"><span>♫</span>KTV曲库管理</router-link>
        <router-link class="mi" :class="{ on: active === 'ai' }" :to="{ name: 'admin-ai' }"><span>✦</span>AI曲库</router-link>
        <router-link class="mi" :class="{ on: active === 'settings' }" :to="{ name: 'admin-settings' }"><span>⚙</span>系统设置</router-link>
      </nav>
    </aside>
    <!-- 主内容区 / Main content area -->
    <main class="main"><slot /></main>
  </div>
</template>

<script setup>
/**
 * 管理后台布局组件 —— 左侧固定侧边栏 + 右侧内容区。
 *
 * Admin layout component — fixed left sidebar with right content area.
 */

/**
 * active 当前激活的菜单项标识（dashboard | source | ktv | settings）
 *
 * @type {String}
 * active — identifier of the currently active menu item.
 */
defineProps({ active: { type: String, default: '' } })
</script>

<style scoped>
.admin-shell { --admin-blue:#2563eb; --admin-border:#e2e8f0; --admin-muted:#64748b; display:flex; min-height:100vh; background:#f5f7fa; color:#172033; }
.side { width:220px; flex:none; background:#fff; border-right:1px solid var(--admin-border); }
.brand { height:76px; padding:18px 20px; border-bottom:1px solid var(--admin-border); display:flex; flex-direction:column; justify-content:center; }
.brand strong { font-size:18px; line-height:1; }.brand span { margin-top:7px; color:#94a3b8; font-size:11px; }
nav { padding:16px 12px; }.nav-label { padding:0 10px 8px; font-size:11px; color:#94a3b8; }
.mi { display:flex; align-items:center; gap:10px; min-height:42px; padding:0 12px; margin-bottom:4px; border-radius:6px; color:#475569; font-size:13px; border:1px solid transparent; }
.mi span { width:18px; text-align:center; font-size:16px; color:#64748b; }.mi:hover { background:#f8fafc; color:#172033; }
.mi.on { color:var(--admin-blue); background:#eff6ff; border-color:#bfdbfe; font-weight:600; box-shadow:inset 3px 0 0 var(--admin-blue); }.mi.on span { color:var(--admin-blue); }
.main { flex:1; min-width:0; padding:24px 28px 48px; overflow:auto; }
@media (max-width:760px) { .side { width:176px; }.main { padding:18px 14px 40px; }.brand { padding-left:16px; }.mi { padding:0 9px; font-size:12px; } }
</style>
