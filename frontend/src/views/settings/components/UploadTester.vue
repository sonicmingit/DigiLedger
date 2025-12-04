<template>
  <div class="upload-tester">
    <el-card shadow="never">
      <h3 class="title">对象存储上传测试</h3>
      <p class="description">
        通过该工具可以验证上传配置是否正确。点击下方按钮选择文件，系统会调用后端上传接口并返回存储路径。
      </p>
      <UnifiedUploader
        class="upload-block"
        :http-request="handleUpload"
        accept="image/*"
        :disabled="uploading"
        :show-file-list="false"
        @success="() => {}"
      />
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
    
    <!-- 手动清理功能 -->
    <el-card shadow="never" class="cleanup-section">
      <h3 class="title">手动清理未使用文件</h3>
      <p class="description">
        此功能可以帮助您查找并删除存储中不再被引用的文件，释放存储空间。
      </p>
      
      <el-button 
        type="warning" 
        :loading="scanning"
        @click="scanUnusedFiles"
      >
        {{ scanning ? '扫描中...' : '扫描未使用文件' }}
      </el-button>
      
      <div v-if="unusedFiles !== null" class="scan-result mt">
        <el-alert 
          :type="unusedFiles.length > 0 ? 'warning' : 'success'" 
          :title="`找到 ${unusedFiles.length} 个未使用的文件`" 
          show-icon 
          class="mb-sm"
        />
        
        <div v-if="unusedFiles.length > 0">
          <el-table 
            :data="unusedFiles" 
            max-height="360" 
            class="file-table"
            row-key="objectKey"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column label="预览" width="120">
              <template #default="{ row }">
                <el-image
                  v-if="row.preview"
                  :src="row.preview"
                  fit="cover"
                  :preview-src-list="[row.preview]"
                  style="width: 80px; height: 48px; border-radius: 6px"
                />
                <span v-else class="no-preview">无预览</span>
              </template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件名" />
            <el-table-column prop="objectKey" label="对象键" show-overflow-tooltip />
          </el-table>
          
          <div class="actions mt">
            <el-popconfirm
              :title="confirmTitle"
              confirm-button-text="确认删除"
              cancel-button-text="取消"
              @confirm="performCleanup"
            >
              <template #reference>
                <el-button 
                  type="danger" 
                  :loading="cleaning"
                  :disabled="cleaning || selectedFiles.length === 0"
                >
                  {{ cleaning ? '清理中...' : `确认删除 (${selectedFiles.length || 0})` }}
                </el-button>
              </template>
            </el-popconfirm>
            
            <el-button @click="resetScan">重新扫描</el-button>
            <div class="summary">
              已选 {{ selectedFiles.length }} / 共 {{ unusedFiles.length }} 个
            </div>
          </div>
        </div>
        
        <div v-else class="no-files">
          <el-result icon="success" title="无需清理" subTitle="没有发现未使用的文件" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import UnifiedUploader from '@/components/UnifiedUploader.vue'
import type { UploadRequestOptions } from 'element-plus'
import { ElMessage } from 'element-plus'
import { uploadFile, getUnusedAttachments, cleanupUnusedAttachments, type UnusedAttachment } from '@/api/file'
import { buildOssUrl } from '@/utils/storage'

interface UploadResult {
  objectKey: string
  accessUrl: string
  preview: string | null
}

interface UnusedFile {
  objectKey: string
  fileName: string
  preview: string | null
}

const uploading = ref(false)
const error = ref('')
const result = ref<UploadResult | null>(null)

// 新增的状态
const scanning = ref(false)
const cleaning = ref(false)
const unusedFiles = ref<UnusedFile[] | null>(null)
const selectedFiles = ref<UnusedFile[]>([])

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

// 新增的方法
const scanUnusedFiles = async () => {
  scanning.value = true
  try {
    const files = (await getUnusedAttachments()) || []
    unusedFiles.value = files.map((item: UnusedAttachment) => ({
      objectKey: item.objectKey,
      fileName: extractFileName(item.objectKey),
      preview: item.url || buildOssUrl(item.objectKey) || null
    }))
    selectedFiles.value = []
    ElMessage.success(`扫描完成，找到 ${files.length} 个未使用的文件`)
  } catch (err: any) {
    ElMessage.error(err?.message || '扫描失败')
  } finally {
    scanning.value = false
  }
}

const performCleanup = async () => {
  if (!selectedFiles.value.length) {
    ElMessage.warning('请至少选择一个文件进行清理')
    return
  }

  cleaning.value = true
  try {
    const objectKeys = selectedFiles.value.map((item) => item.objectKey)
    await cleanupUnusedAttachments(objectKeys)
    ElMessage.success('清理完成')
    // 清理完成后从列表中移除已删除项
    if (unusedFiles.value) {
      const deletedSet = new Set(objectKeys)
      unusedFiles.value = unusedFiles.value.filter((item) => !deletedSet.has(item.objectKey))
    }
    selectedFiles.value = []
  } catch (err: any) {
    ElMessage.error(err?.message || '清理失败')
  } finally {
    cleaning.value = false
  }
}

const resetScan = () => {
  unusedFiles.value = null
  selectedFiles.value = []
}

const extractFileName = (objectKey: string) => {
  const parts = objectKey.split('/')
  return parts[parts.length - 1]
}

const confirmTitle = computed(() => {
  const count = selectedFiles.value.length
  return count ? `确定删除选中的 ${count} 个文件吗？此操作不可撤销` : '请选择要删除的文件'
})

const handleSelectionChange = (rows: UnusedFile[]) => {
  selectedFiles.value = rows
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

.mb-sm {
  margin-bottom: 12px;
}

.preview {
  margin-top: 16px;
  width: 220px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.cleanup-section {
  margin-top: 20px;
}

.file-table {
  margin-top: 12px;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.summary {
  margin-left: auto;
  color: var(--el-text-color-secondary);
  align-self: center;
}

.no-preview {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.no-files {
  margin-top: 16px;
}
</style>
