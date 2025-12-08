<template>
  <el-upload
    ref="uploadRef"
    class="unified-uploader"
    drag
    v-bind="$attrs"
    :http-request="httpRequest"
    :show-file-list="showFileList"
    :accept="accept"
    :multiple="multiple"
    :disabled="disabled"
    :capture="capture"
    @success="onSuccess"
    @error="onError"
    @progress="onProgress"
    @paste.stop="onPaste"
  >
    <div class="upload-drag-content">
      <p class="hint">将文件拖到此处，或</p>
      <el-button type="primary" @click.stop.prevent="triggerSelect">选择文件</el-button>
    </div>
  </el-upload>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits } from 'vue'
import type { UploadRequestOptions } from 'element-plus'

const props = defineProps({
  httpRequest: { type: Function as unknown as () => (options: UploadRequestOptions) => void, required: true },
  accept: { type: String, default: '' },
  multiple: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  showFileList: { type: Boolean, default: false },
  capture: { type: String, default: undefined }
})

const emit = defineEmits(['success', 'error', 'progress'])

const uploadRef = ref<any>(null)

const isAcceptedFile = (file: File) => {
  if (!props.accept) return true
  const acceptList = props.accept
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

  if (acceptList.length === 0) return true

  return acceptList.some((accept) => {
    if (accept === '*/*') return true
    if (accept.endsWith('/*')) return file.type.startsWith(accept.slice(0, -1))
    if (accept.startsWith('.')) return file.name.toLowerCase().endsWith(accept.toLowerCase())
    return file.type === accept
  })
}

const triggerSelect = () => {
  // Try to trigger el-upload's internal file input. Prefer direct input click for robustness.
  try {
    const comp = uploadRef.value
    const root = comp?.$el ?? comp
    const input = root?.querySelector?.('input[type=file]')
    if (input) {
      input.click()
      return
    }
    // Fallback to exposed method if available
    comp?.handleClick?.()
  } catch (e) {
    // ignore
  }
}

const onSuccess = (res: any, file: any) => {
  emit('success', res, file)
}

const onError = (err: any, file: any) => {
  emit('error', err, file)
}

const onProgress = (e: any) => {
  emit('progress', e)
}

const onPaste = (event: ClipboardEvent) => {
  if (props.disabled) return

  const items = event.clipboardData?.items
  if (!items?.length) return

  const imageFiles = Array.from(items)
    .filter((item) => item.kind === 'file')
    .map((item) => item.getAsFile())
    .filter((file): file is File => !!file && file.type.startsWith('image/') && isAcceptedFile(file))

  if (imageFiles.length === 0) return

  event.preventDefault()

  const filesToUpload = props.multiple ? imageFiles : imageFiles.slice(0, 1)
  filesToUpload.forEach((file) => uploadRef.value?.handleStart?.(file))
  uploadRef.value?.submit?.()
}
</script>

<style scoped>
.unified-uploader {
  display: block;
}
.upload-drag-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px;
  justify-content: center;
}
.upload-drag-content .hint {
  margin: 0;
  color: var(--el-text-color-secondary);
}
</style>
