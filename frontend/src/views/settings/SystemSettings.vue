<template>
  <div class="settings-page">
    <el-card shadow="never">
      <el-radio-group v-model="active" class="settings-tabs" size="large">
        <el-radio-button v-for="item in tabOptions" :key="item.value" :label="item.value">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>
      <div class="tab-panels">
        <div v-show="active === 'categories'"><category-manager /></div>
        <div v-show="active === 'platforms'"><platform-manager /></div>
        <div v-show="active === 'tags'"><tag-manager /></div>
        <div v-show="active === 'brands'"><brand-manager /></div>
        <div v-show="active === 'upload'"><upload-tester /></div>
        <div v-show="active === 'image-search'"><image-search-settings /></div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import CategoryManager from './components/CategoryManager.vue'
import PlatformManager from './components/PlatformManager.vue'
import TagManager from './components/TagManager.vue'
import BrandManager from './components/BrandManager.vue'
import UploadTester from './components/UploadTester.vue'
import ImageSearchSettings from './components/ImageSearchSettings.vue'
import { useDictionaries } from '@/composables/useDictionaries'

const active = ref('categories')
const tabOptions = [
  { label: '类别管理', value: 'categories' },
  { label: '平台管理', value: 'platforms' },
  { label: '标签管理', value: 'tags' },
  { label: '品牌管理', value: 'brands' },
  { label: '上传测试', value: 'upload' },
  { label: '智能找图', value: 'image-search' }
]
const { load } = useDictionaries()

onMounted(async () => {
  await load()
})
</script>

<style scoped>
.settings-page {
  padding: 8px 0;
}

.settings-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 6px 0 14px;
}

.tab-panels {
  padding-top: 6px;
}

:deep(.el-card) {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
}
</style>
