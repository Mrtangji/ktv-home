<template>
  <nav class="tabbar">
    <router-link class="tab" :class="{ on: active === 'home' }" :to="{ name: 'home' }">
      <span class="ic">🎵</span>点歌
    </router-link>
    <router-link class="tab" :class="{ on: active === 'queue' }" :to="{ name: 'queue' }">
      <span class="ic">📋</span>已点
      <span v-if="player.queueCount" class="badge">{{ player.queueCount }}</span>
    </router-link>
    <router-link class="tab" :class="{ on: active === 'remote' }" :to="{ name: 'remote' }">
      <span class="ic">🎮</span>遥控
    </router-link>
  </nav>
</template>

<script setup>
import { usePlayerStore } from '../stores/player'
defineProps({ active: { type: String, default: '' } })
const player = usePlayerStore()
</script>

<style scoped>
.tabbar {
  position: fixed; bottom: 0; left: 0; right: 0;
  height: calc(62px + var(--safe-bottom)); padding-bottom: var(--safe-bottom);
  background: rgba(10,12,18,.92); backdrop-filter: blur(16px);
  border-top: 1px solid var(--line); display: flex; z-index: 40;
}
.tab {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 3px; font-size: 11px; color: var(--dim2); transition: var(--transition); position: relative;
}
.tab.on { color: var(--gold); }
.tab .ic { font-size: 20px; }
.badge {
  position: absolute; top: 8px; margin-left: 34px;
  background: var(--gold); color: #1a1400; font-size: 10px; font-weight: 700;
  min-width: 16px; height: 16px; border-radius: 8px; padding: 0 4px;
  display: inline-flex; align-items: center; justify-content: center;
}
</style>
