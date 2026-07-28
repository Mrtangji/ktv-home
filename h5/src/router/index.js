import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// H5 页面地图（详设§3.2）
const routes = [
  { path: '/', name: 'entry', component: () => import('../views/EntryView.vue'), meta: { public: true } },        // H5-01
  { path: '/home', name: 'home', component: () => import('../views/HomeView.vue') },                              // H5-02
  { path: '/search', name: 'search', component: () => import('../views/SearchView.vue') },                        // H5-03
  { path: '/artist/:name', name: 'artist', component: () => import('../views/ArtistView.vue') },                  // H5-04
  { path: '/browse', name: 'browse', component: () => import('../views/CategoryBrowseView.vue') },
  { path: '/playlists', name: 'playlists', component: () => import('../views/PlaylistListView.vue') },
  { path: '/playlists/:id', name: 'playlist-detail', component: () => import('../views/PlaylistDetailView.vue') },
  { path: '/recent', name: 'recent-history', component: () => import('../views/RecentHistoryView.vue') },
  { path: '/favorites', name: 'favorites', component: () => import('../views/FavoritesView.vue') },
  { path: '/queue', name: 'queue', component: () => import('../views/QueueView.vue') },                           // H5-05
  { path: '/remote', name: 'remote', component: () => import('../views/RemoteView.vue') },                        // H5-06
  { path: '/lyric', name: 'lyric', component: () => import('../views/LyricView.vue') },                           // H5-07

  // 管理后台（免登录，PC/手机浏览器）
  { path: '/admin', name: 'admin-dashboard', component: () => import('../views/admin/DashboardView.vue'), meta: { public: true, admin: true } },
  { path: '/admin/source-library', name: 'admin-source-library', component: () => import('../views/admin/SourceLibraryView.vue'), meta: { public: true, admin: true } },
  { path: '/admin/ktv-library', name: 'admin-ktv-library', component: () => import('../views/admin/KtvLibraryView.vue'), meta: { public: true, admin: true } },
  { path: '/admin/songs', redirect: { name: 'admin-ktv-library' } },
  { path: '/admin/ai', name: 'admin-ai', component: () => import('../views/admin/AiLibraryView.vue'), meta: { public: true, admin: true } },
  { path: '/admin/settings', name: 'admin-settings', component: () => import('../views/admin/SettingsView.vue'), meta: { public: true, admin: true } }
]

const router = createRouter({
  // 部署在 /m 下
  history: createWebHistory('/m/'),
  routes
})

// 未注册（无昵称）时强制回进入页（详设 H5-01）
router.beforeEach((to) => {
  const user = useUserStore()
  if (!to.meta.public && !user.isRegistered) {
    return { name: 'entry' }
  }
})

export default router
