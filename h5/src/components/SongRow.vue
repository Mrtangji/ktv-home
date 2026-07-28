<template>
  <div class="songrow">
    <span v-if="rank" class="rank" :class="{ top: rank <= 3 }">{{ rank }}</span>
    <div class="grow info">
      <div class="t">
        <span v-html="highlightedTitle"></span>
        <span class="tag" :class="tagClass">{{ tagText }}</span>
      </div>
      <div class="s">{{ song.artist }}<span v-if="extra"> · {{ extra }}</span></div>
    </div>
    <button class="favorite-btn" :class="{ on: favorites.has(song.id) }" :disabled="favoriteBusy"
            :aria-label="favorites.has(song.id) ? '取消收藏' : '收藏'" @click="toggleFavorite">
      {{ favorites.has(song.id) ? '♥' : '♡' }}
    </button>
    <button v-if="ordered" class="order-btn done" disabled>已点 ✓</button>
    <button v-else class="order-btn" @click="$emit('order', song)">点歌</button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useFavoritesStore } from '../stores/favorites'
import { useUserStore } from '../stores/user'
import { useToast } from '../composables/useToast'

const props = defineProps({
  song: { type: Object, required: true },
  rank: { type: Number, default: 0 },
  keyword: { type: String, default: '' },
  extra: { type: String, default: '' },
  ordered: { type: Boolean, default: false }
})
defineEmits(['order'])
const favorites = useFavoritesStore()
const user = useUserStore()
const { toast } = useToast()
const favoriteBusy = ref(false)

async function toggleFavorite() {
  favoriteBusy.value = true
  try {
    const added = await favorites.toggle(props.song.id, user.clientToken)
    toast(added ? '已加入收藏' : '已取消收藏')
  } catch (error) {
    toast(error.message || '收藏操作失败')
  } finally {
    favoriteBusy.value = false
  }
}

const tagText = computed(() => ({
  KTV_VIDEO: 'KTV版', MV: 'MV版', AUDIO: '音频版'
}[props.song.mediaType] || ''))

const tagClass = computed(() => ({
  KTV_VIDEO: 'tag-ktv', MV: 'tag-mv', AUDIO: 'tag-audio'
}[props.song.mediaType] || 'tag-audio'))

// 关键词高亮（详设 H5-03）
const highlightedTitle = computed(() => {
  const title = props.song.title || ''
  const kw = props.keyword?.trim()
  if (!kw) return escapeHtml(title)
  const idx = title.toLowerCase().indexOf(kw.toLowerCase())
  if (idx < 0) return escapeHtml(title)
  return escapeHtml(title.slice(0, idx))
    + '<span class="hl">' + escapeHtml(title.slice(idx, idx + kw.length)) + '</span>'
    + escapeHtml(title.slice(idx + kw.length))
})

function escapeHtml(s) {
  return s.replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]))
}
</script>

<style scoped>
.songrow {
  display: flex; align-items: center; gap: 10px;
  padding: 13px 0; border-bottom: 1px solid var(--line);
}
.rank { width: 22px; text-align: center; font-weight: 800; color: var(--dim2); font-size: 14px; }
.rank.top { color: var(--gold); }
.info { min-width: 0; }
.t { font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.t :deep(.hl) { color: var(--gold); }
.s { font-size: 12px; color: var(--dim); margin-top: 3px; }
.order-btn {
  background: linear-gradient(135deg, var(--gold), #dba70e); color: #1a1400; font-weight: 700;
  font-size: 13px; border-radius: 8px; padding: 7px 14px; flex: none;
  box-shadow: 0 2px 10px rgba(240,199,66,.2); transition: var(--transition);
}
.order-btn:active { transform: scale(.95); }
.order-btn.done { background: var(--panel2); color: var(--dim); box-shadow: none; }
.favorite-btn { color: var(--dim2); font-size: 25px; line-height: 1; padding: 5px; flex: none; transition: var(--transition); }
.favorite-btn.on { color: #f87171; }
.favorite-btn:active { transform: scale(.88); }
.favorite-btn:disabled { opacity: .45; }
</style>
