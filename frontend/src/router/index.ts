import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'
import AssetList from '@/views/assets/AssetList.vue'
import AssetDetail from '@/views/assets/AssetDetail.vue'
import WishlistList from '@/views/wishlist/WishlistList.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: Dashboard
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
    }
  ]
})

export default router
