<template>
  <div class="settings-page">
    <PageHeader title="系统设置" subtitle="维护类别、平台、标签与品牌等基础字典">
      <template #actions>
        <SegmentedTabs v-model="active" :items="tabOptions" size="large" />
      </template>
    </PageHeader>
    <el-card shadow="never" class="settings-content">
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
import PageHeader from '@/components/PageHeader.vue'
import SegmentedTabs from '@/components/SegmentedTabs.vue'
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
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tab-panels {
  padding-top: 6px;
}

.settings-content {
  border-radius: var(--dl-radius-md);
  border: 1px solid var(--el-border-color-light);
}
</style>
