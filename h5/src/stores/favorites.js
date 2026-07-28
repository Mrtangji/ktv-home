import { defineStore } from 'pinia'
import api from '../api/client'

export const useFavoritesStore = defineStore('favorites', {
  state: () => ({ ids: [], loadedFor: '', loading: false }),
  getters: {
    has: (state) => (songId) => state.ids.includes(songId)
  },
  actions: {
    async load(clientToken, force = false) {
      if (!clientToken || this.loading || (!force && this.loadedFor === clientToken)) return
      this.loading = true
      try {
        this.ids = await api.favoriteIds(clientToken)
        this.loadedFor = clientToken
      } finally {
        this.loading = false
      }
    },
    async toggle(songId, clientToken) {
      if (this.has(songId)) {
        await api.removeFavorite(songId, clientToken)
        this.ids = this.ids.filter(id => id !== songId)
        return false
      }
      await api.addFavorite(songId, clientToken)
      if (!this.has(songId)) this.ids = [songId, ...this.ids]
      return true
    }
  }
})
