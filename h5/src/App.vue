<template>
  <router-view v-slot="{ Component }">
    <component :is="Component" />
  </router-view>
  <ToastHost />
  <DialogHost />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { usePlayerStore } from './stores/player'
import { useUserStore } from './stores/user'
import { useFavoritesStore } from './stores/favorites'
import ToastHost from './components/ToastHost.vue'
import DialogHost from './components/DialogHost.vue'

// 已注册用户进入即连 WebSocket；未注册的在进入页提交后由路由跳转触发
const player = usePlayerStore()
const user = useUserStore()
const favorites = useFavoritesStore()

onMounted(() => {
  if (user.isRegistered) {
    player.connect()
    favorites.load(user.clientToken).catch(() => {})
  }
})
onUnmounted(() => player.disconnect())
</script>
