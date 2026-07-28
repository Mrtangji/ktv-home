import { ref } from 'vue'

// 全局轻量 toast（详设：点歌反馈「已加入队列，前面还有 N 首」）
const message = ref('')
let timer = null

export function useToast() {
  function toast(msg, ms = 1800) {
    message.value = msg
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => { message.value = '' }, ms)
  }
  return { message, toast }
}
