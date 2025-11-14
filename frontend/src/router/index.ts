import { createRouter, createWebHistory } from 'vue-router'
import AssetOverview from '@/views/AssetOverview.vue'
import AssetList from '@/views/assets/AssetList.vue'
import AssetDetail from '@/views/assets/AssetDetail.vue'
import WishlistList from '@/views/wishlist/WishlistList.vue'
import SystemSettings from '@/views/settings/SystemSettings.vue'

const MobileRoot = () => import('@/mobile/layouts/MobileRoot.vue')
const MobileHome = () => import('@/mobile/pages/Home.vue')
const MobileSearch = () => import('@/mobile/pages/Search.vue')
const MobileWishlist = () => import('@/mobile/pages/Wishlist.vue')
const MobileSettings = () => import('@/mobile/pages/Settings.vue')
const MobileStats = () => import('@/mobile/pages/Stats.vue')
const MobileEditor = () => import('@/mobile/pages/Editor.vue')
const MobileCategories = () => import('@/mobile/pages/CategoryManager.vue')

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
    },
    {
      path: '/mobile',
      component: MobileRoot,
      meta: { layout: 'mobile' },
      children: [
        { path: '', redirect: { name: 'mobileHome' } },
        {
          path: 'index',
          name: 'mobileHome',
          component: MobileHome,
          meta: { layout: 'mobile' }
        },
        {
          path: 'search',
          name: 'mobileSearch',
          component: MobileSearch,
          meta: { layout: 'mobile' }
        },
        {
          path: 'wishlist',
          name: 'mobileWishlist',
          component: MobileWishlist,
          meta: { layout: 'mobile' }
        },
        {
          path: 'settings',
          name: 'mobileSettings',
          component: MobileSettings,
          meta: { layout: 'mobile' }
        },
        {
          path: 'settings/categories',
          name: 'mobileCategories',
          component: MobileCategories,
          meta: { layout: 'mobile' }
        },
        {
          path: 'stats',
          name: 'mobileStats',
          component: MobileStats,
          meta: { layout: 'mobile' }
        },
        {
          path: 'edit',
          name: 'mobileEditor',
          component: MobileEditor,
          meta: { layout: 'mobile' }
        }
      ]
    }
  ]
})

export default router
