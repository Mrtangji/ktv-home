// REST API 封装（详设§11）。基址 /api，开发时由 Vite 代理到 :8080。
const BASE = '/api'

async function request(path, options = {}) {
  const isFormData = options.body instanceof FormData
  const res = await fetch(BASE + path, {
    headers: { ...(isFormData ? {} : { 'Content-Type': 'application/json' }), ...(options.headers || {}) },
    ...options
  })
  if (!res.ok) {
    let body = null
    try { body = await res.json() } catch { /* ignore */ }
    const err = new Error(body?.message || `HTTP ${res.status}`)
    err.code = body?.code
    err.status = res.status
    throw err
  }
  if (res.status === 204) return null
  const ct = res.headers.get('content-type') || ''
  return ct.includes('application/json') ? res.json() : res.text()
}

export const api = {
  health: () => request('/health'),

  // 搜索/曲库（P1.6/P1.7）
  searchSongs: (keyword, type = '', page = 0) =>
    request(`/songs?keyword=${encodeURIComponent(keyword)}&type=${type}&page=${page}`),
  songDetail: (id) => request(`/songs/${id}`),

  // 队列/控制（P1.9~P1.12）
  getQueue: () => request('/queue'),
  control: (action, params = {}, clientToken) =>
    request('/control', {
      method: 'POST',
      body: JSON.stringify({ action, params, client_token: clientToken })
    }),

  // 发现/历史/心愿（P3）
  ranking: (days = 30) => request(`/ranking?days=${days}`),
  newSongs: () => request('/songs/new'),
  history: () => request('/history'),
  recentHistory: (clientToken, mine = false) => request(`/history/recent?clientToken=${encodeURIComponent(clientToken || '')}&mine=${mine}`),
  repeatHistory: (historyId, clientToken, force = false) => request(`/history/${historyId}/repeat`, { method: 'POST', body: JSON.stringify({ clientToken, force }) }),
  favorites: (clientToken) => request(`/favorites?clientToken=${encodeURIComponent(clientToken || '')}`),
  favoriteIds: (clientToken) => request(`/favorites/ids?clientToken=${encodeURIComponent(clientToken || '')}`),
  addFavorite: (songId, clientToken) => request(`/favorites/${songId}`, { method: 'POST', body: JSON.stringify({ clientToken }) }),
  removeFavorite: (songId, clientToken) => request(`/favorites/${songId}?clientToken=${encodeURIComponent(clientToken || '')}`, { method: 'DELETE' }),
  playlists: () => request('/playlists'),
  playlistDetail: (id) => request(`/playlists/${id}`),
  orderPlaylist: (id, clientToken) => request(`/playlists/${id}/order`, { method: 'POST', body: JSON.stringify({ clientToken }) }),
  browseArtists: () => request('/browse/artists'),
  browseLanguages: () => request('/browse/languages'),
  browseTags: () => request('/browse/tags'),
  browseSongs: (params = {}) => request('/browse/songs?' + new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value != null)).toString()),
  addWish: (keyword, clientToken) =>
    request('/wishes', { method: 'POST', body: JSON.stringify({ keyword, client_token: clientToken }) }),
  registerUser: (clientToken, nickname) =>
    request('/user', { method: 'POST', body: JSON.stringify({ client_token: clientToken, nickname }) }),
  roomHostStatus: (clientToken) => request(`/room/host?clientToken=${encodeURIComponent(clientToken || '')}`),
  claimRoomHost: (clientToken) => request('/room/host/claim', { method: 'POST', body: JSON.stringify({ clientToken }) }),
  releaseRoomHost: (clientToken) => request('/room/host/release', { method: 'POST', body: JSON.stringify({ clientToken }) }),

  // 管理后台（P2）
  adminStatus: () => request('/admin/status'),
  adminSongs: (params = {}) => request('/admin/songs?' + new URLSearchParams(
    Object.entries(params).filter(([, value]) => value !== '' && value != null)
  ).toString()),
  adminEditSong: (id, body) => request(`/admin/songs/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  adminDeleteSong: (id) => request(`/admin/songs/${id}`, { method: 'DELETE' }),
  adminDeleteSongs: (ids) => request('/admin/songs', { method: 'DELETE', body: JSON.stringify({ ids }) }),
  adminTranscodeSong: (id) => request(`/admin/songs/${id}/transcode`, { method: 'POST' }),
  adminImports: (action = '', page = 0, size = 20) => request(`/admin/imports?action=${encodeURIComponent(action)}&page=${page}&size=${size}`),
  adminDeleteImportSource: (id) => request(`/admin/imports/${id}/source`, { method: 'DELETE' }),
  adminSourceLibrary: (params = {}) => request('/admin/source-library?' + new URLSearchParams(
    Object.entries(params).filter(([, value]) => value !== '' && value != null)
  ).toString()),
  adminStartSourceTranscode: (ids = [], all = false) => request('/admin/source-library/transcode', { method: 'POST', body: JSON.stringify({ ids, all }) }),
  adminPrioritizeSourceTranscode: (id) => request('/admin/source-library/transcode/priority', { method: 'POST', body: JSON.stringify({ id }) }),
  adminSourceTranscodeProgress: () => request('/admin/source-library/progress'),
  adminCleanupImportedSources: () => request('/admin/source-library/cleanup', { method: 'POST' }),
  adminDeleteSources: (ids = [], filters = {}) => request('/admin/source-library', {
    method: 'DELETE', body: JSON.stringify({ ids, ...filters })
  }),
  adminPreviewReparse: (songIds, rule) => request('/admin/songs/reparse/preview', { method: 'POST', body: JSON.stringify({ songIds, rule }) }),
  adminApplyReparse: (songIds, rule) => request('/admin/songs/reparse/apply', { method: 'POST', body: JSON.stringify({ songIds, rule }) }),
  adminScan: () => request('/admin/scan', { method: 'POST' }),
  adminGetSettings: () => request('/admin/settings'),
  adminTranscodeHardware: () => request('/admin/settings/transcode-hardware'),
  adminPutSettings: (body) => request('/admin/settings', { method: 'PUT', body: JSON.stringify(body) }),
  adminResetTranscodeDefaults: () => request('/admin/settings/transcode-defaults', { method: 'POST' }),
  standbyContent: () => request('/standby/content'),
  adminUploadStandbyLogo: (file) => {
    const body = new FormData()
    body.append('file', file)
    return request('/admin/standby/logo', { method: 'POST', body })
  },
  adminWishes: () => request('/wishes'),

  // ADM-04 AI 曲库与主题歌单
  adminAiTasks: () => request('/admin/ai/tasks'),
  adminAiCreateTask: (songId) => request('/admin/ai/tasks', { method: 'POST', body: JSON.stringify({ songId }) }),
  adminAiCreateUnclassified: (limit = 50) => request('/admin/ai/tasks/unclassified', { method: 'POST', body: JSON.stringify({ limit }) }),
  adminAiRetryTask: (id) => request(`/admin/ai/tasks/${id}/retry`, { method: 'POST' }),
  adminAiApplyTask: (id, result) => request(`/admin/ai/tasks/${id}/apply`, { method: 'POST', body: JSON.stringify(result) }),
  adminAiPlaylists: () => request('/admin/ai/playlists'),
  adminAiPlaylist: (id) => request(`/admin/ai/playlists/${id}`),
  adminAiCreatePlaylist: (body) => request('/admin/ai/playlists', { method: 'POST', body: JSON.stringify(body) }),
  adminAiUpdatePlaylist: (id, body) => request(`/admin/ai/playlists/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  adminAiDeletePlaylist: (id) => request(`/admin/ai/playlists/${id}`, { method: 'DELETE' }),
  adminAiGeneratePlaylist: (body) => request('/admin/ai/playlists/generate', { method: 'POST', body: JSON.stringify(body) }),
  adminAiAddPlaylistSong: (id, songId) => request(`/admin/ai/playlists/${id}/songs`, { method: 'POST', body: JSON.stringify({ songId }) }),
  adminAiRemovePlaylistSong: (id, songId) => request(`/admin/ai/playlists/${id}/songs/${songId}`, { method: 'DELETE' }),
  adminAiUploadPlaylistCover: (id, file) => {
    const body = new FormData()
    body.append('file', file)
    return request(`/admin/ai/playlists/${id}/cover`, { method: 'POST', body })
  },
  adminAiReorderPlaylistSongs: (id, songIds) => request(`/admin/ai/playlists/${id}/songs/order`, { method: 'PUT', body: JSON.stringify({ songIds }) })
}

// 控制指令便捷封装：自动带上当前用户 token（调用处传入）
export function makeControls(clientToken) {
  const c = (action, params) => api.control(action, params, clientToken)
  return {
    order: (songId, force = false) => c('order', { song_id: songId, force }),
    top: (queueId) => c('top', { queue_id: queueId }),
    cancel: (queueId) => c('cancel', { queue_id: queueId }),
    shuffle: () => c('shuffle', {}),
    play: () => c('play', {}),
    pause: () => c('pause', {}),
    next: () => c('next', {}),
    restart: () => c('restart', {}),
    setVolume: (volume) => c('set_volume', { volume }),
    mute: (muted) => c('mute', { muted }),
    setVocal: (mode) => c('set_vocal', { mode }),
    swapVocalTracks: () => c('swap_vocal_tracks', {}),
    effect: (effectId) => c('effect', { effect_id: effectId })
  }
}

export default api
