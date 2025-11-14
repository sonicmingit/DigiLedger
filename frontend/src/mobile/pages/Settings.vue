<template>
  <div class="mobile-scroll">
    <header class="mobile-topbar">
      <div class="mobile-topbar-header">
        <h1>设置</h1>
      </div>
    </header>

    <section class="mobile-card-section">
      <article class="mobile-settings-card" v-if="profile">
        <div class="user-card">
          <img :src="profile.avatar || fallbackAvatar" alt="头像" />
          <div>
            <h3>{{ profile.nickname }}</h3>
            <p>{{ profile.level || '普通会员' }}</p>
          </div>
        </div>
      </article>

      <article class="mobile-settings-card">
        <h4>偏好设置</h4>
        <div class="mobile-settings-item">
          <span>视图模式</span>
          <select v-model="preferences.viewMode" @change="save">
            <option value="card">卡片模式</option>
            <option value="list">列表模式</option>
          </select>
        </div>
        <div class="mobile-settings-item">
          <span>金额单位</span>
          <select v-model="preferences.currency" @change="save">
            <option value="CNY">人民币</option>
            <option value="USD">美元</option>
            <option value="EUR">欧元</option>
          </select>
        </div>
        <div class="mobile-settings-item">
          <span>金额千分位</span>
          <div class="mobile-switch" :class="{ on: preferences.thousandSeparator }" @click="toggle('thousandSeparator')"></div>
        </div>
        <div class="mobile-settings-item">
          <span>保留小数位数</span>
          <input type="number" min="0" max="4" v-model.number="preferences.decimalPlaces" @change="save" />
        </div>
        <div class="mobile-settings-item">
          <span>统计包含退役资产</span>
          <div class="mobile-switch" :class="{ on: preferences.includeRetired }" @click="toggle('includeRetired')"></div>
        </div>
        <div class="mobile-settings-item">
          <span>统计包含已卖出资产</span>
          <div class="mobile-switch" :class="{ on: preferences.includeSold }" @click="toggle('includeSold')"></div>
        </div>
      </article>

      <article class="mobile-settings-card">
        <div class="mobile-settings-item">
          <span>分类管理</span>
          <button type="button" @click="goCategories">进入</button>
        </div>
      </article>
    </section>

    <div v-if="toast" class="mobile-toast">{{ toast }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchMobilePreferences,
  fetchMobileProfile,
  updateMobilePreferences,
  type MobilePreferencePayload,
  type MobileUserProfile
} from '@/mobile/api/settings'

const router = useRouter()
const fallbackAvatar = 'https://cdn.jsdelivr.net/gh/itellboy/assets@main/avatar-default.png'
const profile = ref<MobileUserProfile | null>(null)
const toast = ref('')
const preferences = reactive<MobilePreferencePayload>({
  viewMode: 'card',
  currency: 'CNY',
  thousandSeparator: true,
  decimalPlaces: 2,
  includeRetired: true,
  includeSold: false
})

const load = async () => {
  try {
    const [prefRes, profileRes] = await Promise.all([fetchMobilePreferences(), fetchMobileProfile()])
    Object.assign(preferences, prefRes)
    profile.value = profileRes
  } catch (error) {
    toast.value = '加载偏好设置失败'
    setTimeout(() => (toast.value = ''), 1800)
  }
}

const save = async () => {
  try {
    await updateMobilePreferences({ ...preferences })
    toast.value = '已保存偏好设置'
  } catch (error) {
    toast.value = '保存失败，请稍后重试'
  } finally {
    setTimeout(() => (toast.value = ''), 1800)
  }
}

const toggle = async (key: keyof MobilePreferencePayload) => {
  ;(preferences[key] as boolean) = !(preferences[key] as boolean)
  await save()
}

const goCategories = () => router.push({ name: 'mobileCategories' })

onMounted(load)
</script>

<style scoped>
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-card img {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  object-fit: cover;
}

h4 {
  margin: 0 0 12px;
}

select {
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  padding: 6px 12px;
}

input[type='number'] {
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  padding: 6px 12px;
  width: 72px;
}

button {
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: transparent;
  padding: 6px 12px;
}
</style>
