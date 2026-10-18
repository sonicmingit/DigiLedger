<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <img class="brand-logo" src="@/assets/digiledger-logo.png" alt="DigiLedger logo" />
        <div class="brand-copy"><strong>DIGI LEDGER</strong><span>我的数字仓库</span></div>
      </div>
      <nav aria-label="主导航">
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to" :class="{ active: isActive(item.to) }">
          <img :src="item.icon" alt="" /><span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="profile"><div class="avatar">DL</div><div><strong>私人物品库</strong><button class="sign-out" :disabled="loggingOut" @click="signOut">{{ loggingOut ? '正在退出…' : '退出登录' }}</button></div></div>
    </aside>
    <main class="main-content"><RouterView /></main>
    <AssetEditorDrawer />
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { logout, authError } from '@/auth'
import AssetEditorDrawer from './AssetEditorDrawer.vue'
import dashboardIcon from '@/assets/icons/dashboard.svg'
import assetsIcon from '@/assets/icons/assets.svg'
import wishlistIcon from '@/assets/icons/wishlist.svg'
import upgradeIcon from '@/assets/icons/upgrade.svg'
import settingsIcon from '@/assets/icons/settings.svg'

const route = useRoute()
const loggingOut = ref(false)
async function signOut() {
  loggingOut.value = true
  try { await logout() } catch (error) { ElMessage.error(authError(error)) }
  finally { loggingOut.value = false }
}
const nav = [
  { to: '/', label: '总览', icon: dashboardIcon }, { to: '/assets', label: '物品中心', icon: assetsIcon },
  { to: '/wishlist', label: '心愿单', icon: wishlistIcon }, { to: '/upgrade-routes', label: '升级路线', icon: upgradeIcon },
  { to: '/settings', label: '系统设置', icon: settingsIcon }
]
const isActive = (path: string) => path === '/' ? route.path === '/' : route.path.startsWith(path)
</script>

<style scoped>
.sign-out{border:0;padding:0;background:transparent;color:#b7ff3c;font-size:11px;cursor:pointer;text-align:left}
</style>
