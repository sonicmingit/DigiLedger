<template>
  <div class="settings-page">
    <el-card shadow="never">
      <el-tabs v-model="active">
        <el-tab-pane label="类别管理" name="categories">
          <category-manager />
        </el-tab-pane>
        <el-tab-pane label="平台管理" name="platforms">
          <platform-manager />
        </el-tab-pane>
        <el-tab-pane label="标签管理" name="tags">
          <tag-manager />
        </el-tab-pane>
        <el-tab-pane label="品牌管理" name="brands">
          <brand-manager />
        </el-tab-pane>
        <el-tab-pane label="上传测试" name="upload">
          <upload-tester />
        </el-tab-pane>
        <el-tab-pane label="智能找图" name="image-search">
          <image-search-settings />
        </el-tab-pane>
      </el-tabs>
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
const { load } = useDictionaries()

onMounted(async () => {
  await load()
})
</script>

<style scoped>
.settings-page {
  padding: 8px 0;
}

:deep(.el-card) {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  box-shadow: var(--color-shadow);
}
</style>
