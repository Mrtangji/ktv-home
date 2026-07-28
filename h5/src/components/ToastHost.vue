<template>
  <transition name="toast">
    <div v-if="message" class="proto-toast">{{ message }}</div>
  </transition>
</template>

<script setup>
import { useToast } from '../composables/useToast'
const { message } = useToast()
</script>

<style scoped>
.proto-toast {
  position: fixed; left: 50%; bottom: 90px; transform: translateX(-50%);
  background: rgba(15,17,24,.92); backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,.08); color: #f0f2f7;
  padding: 12px 20px; border-radius: 12px; font-size: 13px; z-index: 999; white-space: nowrap;
  box-shadow: 0 12px 40px rgba(0,0,0,.5);
}
.toast-enter-active, .toast-leave-active { transition: opacity .25s; }
.toast-enter-from, .toast-leave-to { opacity: 0; }
</style>
