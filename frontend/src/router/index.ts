import { createRouter, createWebHistory } from 'vue-router'
import AssetOverview from '@/views/AssetOverview.vue'
import AssetList from '@/views/assets/AssetList.vue'
import AssetDetail from '@/views/assets/AssetDetail.vue'
import WishlistList from '@/views/wishlist/WishlistList.vue'
import SystemSettings from '@/views/settings/SystemSettings.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'assetOverview',
      component: AssetOverview
    },
    {
      path: '/assets',
      name: 'assets',
      component: AssetList
    },
    {
      path: '/assets/:id',
      name: 'assetDetail',
      component: AssetDetail,
      props: true
    },
    {
      path: '/wishlist',
      name: 'wishlist',
      component: WishlistList
    },
    {
      path: '/settings',
      name: 'settings',
      component: SystemSettings
    }
  ]
})

export default router
