import { defineStore } from 'pinia'
import type { AssetDetail } from '@/types'

// Cross-route workspace state lets dashboard and list actions reuse the same editor drawer.
export const useWorkspaceStore = defineStore('workspace', {
  state: () => ({ assetEditorOpen: false, editingAsset: null as AssetDetail | null, refreshKey: 0 }),
  actions: {
    openNewAsset() { this.editingAsset = null; this.assetEditorOpen = true },
    openAssetEditor(asset: AssetDetail) { this.editingAsset = asset; this.assetEditorOpen = true },
    closeAssetEditor(changed = false) {
      this.assetEditorOpen = false
      this.editingAsset = null
      if (changed) this.refreshKey += 1
    }
  }
})
