import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
    { path: '/assets', name: 'assets', component: () => import('@/views/AssetsView.vue') },
    { path: '/assets/:id', name: 'asset-detail', component: () => import('@/views/AssetDetailView.vue') },
    { path: '/wishlist', name: 'wishlist', component: () => import('@/views/WishlistView.vue') },
    { path: '/upgrade-routes', name: 'upgrade-routes', component: () => import('@/views/UpgradeRoutesView.vue') },
    { path: '/settings', name: 'settings', component: () => import('@/views/SettingsView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior: () => ({ top: 0 })
})

export default router
