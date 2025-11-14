<template>
  <div class="upload-tester">
    <el-card shadow="never">
      <h3 class="title">对象存储上传测试</h3>
      <p class="description">
        通过该工具可以验证上传配置是否正确。点击下方按钮选择文件，系统会调用后端上传接口并返回存储路径。
      </p>
      <el-upload
        class="upload-block"
        :http-request="handleUpload"
        :show-file-list="false"
        accept="image/*"
        :disabled="uploading"
      >
        <el-button type="primary" :loading="uploading">选择文件上传</el-button>
      </el-upload>
      <el-alert v-if="error" type="error" :title="error" show-icon class="mt" />
      <div v-if="result" class="result mt">
        <el-alert type="success" title="上传成功" show-icon />
        <el-descriptions :column="1" border class="mt-sm">
          <el-descriptions-item label="ObjectKey">{{ result.objectKey }}</el-descriptions-item>
          <el-descriptions-item label="访问地址">
            <el-link :href="result.accessUrl" target="_blank" type="primary">{{ result.accessUrl }}</el-link>
          </el-descriptions-item>
        </el-descriptions>
        <img v-if="result.preview" :src="result.preview" alt="预览" class="preview" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { UploadRequestOptions } from 'element-plus'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/file'
import { buildOssUrl } from '@/utils/storage'

interface UploadResult {
  objectKey: string
  accessUrl: string
  preview: string | null
}

const uploading = ref(false)
const error = ref('')
const result = ref<UploadResult | null>(null)

const handleUpload = async (options: UploadRequestOptions) => {
  uploading.value = true
  error.value = ''
  try {
    const { objectKey, url } = await uploadFile(options.file)
    const accessUrl = buildOssUrl(url || objectKey)
    result.value = {
      objectKey,
      accessUrl,
      preview: accessUrl || null
    }
    options.onSuccess?.(objectKey)
    ElMessage.success('上传成功')
  } catch (err: any) {
    error.value = err?.message || '上传失败'
    result.value = null
    options.onError?.(err)
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-tester {
  padding: 12px 0;
}

.title {
  margin-bottom: 4px;
  font-size: 18px;
  font-weight: 600;
}

.description {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
}

.upload-block {
  margin-bottom: 12px;
}

.mt {
  margin-top: 16px;
}

.mt-sm {
  margin-top: 12px;
}

.preview {
  margin-top: 16px;
  width: 220px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.25);
}
</style>
