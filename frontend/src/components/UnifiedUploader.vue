<template>
  <div
    ref="pasteRef"
    class="unified-uploader-wrapper"
    tabindex="0"
    @paste="handlePaste"
    @click="activatePasteTarget"
    @focus="activatePasteTarget"
  >
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
    >
      <div class="upload-drag-content">
        <p class="hint">将文件拖到此处，点击选择，或 Ctrl+V 粘贴剪贴板图片/文件</p>
        <el-button type="primary" @click.stop.prevent="triggerSelect">选择文件</el-button>
      </div>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'

type PasteTarget = {
  handlePaste: (event: ClipboardEvent) => void
  isDisabled: () => boolean
  isVisible: () => boolean
}

const pasteTargets = new Set<PasteTarget>()
let activeTarget: PasteTarget | null = null
let listenerAttached = false
let pendingRestoreFocus: HTMLElement | null = null
let pasteCaptureInput: HTMLTextAreaElement | null = null
const PASTE_HANDLED_KEY = '__dlPasteHandled'
const shouldShowPasteHints = true
let lastPasteEventAt = 0
let pasteHintTimer: number | null = null

const ensurePasteCaptureInput = () => {
  if (pasteCaptureInput || typeof document === 'undefined') return
  const input = document.createElement('textarea')
  input.setAttribute('aria-hidden', 'true')
  input.tabIndex = -1
  input.style.position = 'fixed'
  input.style.opacity = '0'
  input.style.pointerEvents = 'none'
  input.style.width = '1px'
  input.style.height = '1px'
  input.style.left = '-9999px'
  input.style.top = '0'
  document.body.appendChild(input)
  pasteCaptureInput = input
}

const restorePendingFocus = () => {
  if (pendingRestoreFocus && typeof pendingRestoreFocus.focus === 'function') {
    pendingRestoreFocus.focus()
  }
  pendingRestoreFocus = null
}

const clearPasteHintTimer = () => {
  if (pasteHintTimer !== null) {
    window.clearTimeout(pasteHintTimer)
    pasteHintTimer = null
  }
}

const getClipboardFiles = (event: ClipboardEvent) => {
  const clipboard = event.clipboardData
  if (!clipboard) return []
  const files: File[] = []
  if (clipboard.files && clipboard.files.length) {
    files.push(...Array.from(clipboard.files))
  } else if (clipboard.items && clipboard.items.length) {
    for (const item of Array.from(clipboard.items)) {
      if (item.kind === 'file') {
        const file = item.getAsFile()
        if (file) files.push(file)
      }
    }
  }
  return files
}

const resolvePasteTarget = () => {
  const candidates = Array.from(pasteTargets).filter((target) => !target.isDisabled() && target.isVisible())
  if (!candidates.length) return null
  if (activeTarget && candidates.includes(activeTarget)) {
    return activeTarget
  }
  return candidates[candidates.length - 1]
}

const handleGlobalPaste = (event: ClipboardEvent) => {
  if (!pasteTargets.size) return
  clearPasteHintTimer()
  if ((event as any)[PASTE_HANDLED_KEY]) {
    restorePendingFocus()
    return
  }
  if (!getClipboardFiles(event).length) {
    restorePendingFocus()
    return
  }
  const target = resolvePasteTarget()
  if (!target) {
    restorePendingFocus()
    return
  }
  target.handlePaste(event)
  restorePendingFocus()
}

const isEditableElement = (el: Element | null) => {
  if (!el || !(el instanceof HTMLElement)) return false
  const tag = el.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA') {
    const input = el as HTMLInputElement
    return !input.readOnly && !input.disabled
  }
  if (el.isContentEditable) return true
  return !!el.closest('[contenteditable="true"]')
}

const isPasteShortcut = (event: KeyboardEvent) => {
  if (event.altKey) return false
  if (!(event.ctrlKey || event.metaKey)) return false
  return event.key.toLowerCase() === 'v'
}

const handleGlobalKeydown = (event: KeyboardEvent) => {
  if (!isPasteShortcut(event)) return
  if (!pasteTargets.size) return
  if (isEditableElement(document.activeElement)) return
  const target = resolvePasteTarget()
  if (!target) return
  ensurePasteCaptureInput()
  if (!pasteCaptureInput) return
  pendingRestoreFocus = document.activeElement as HTMLElement | null
  activeTarget = target
  pasteCaptureInput.value = ''
  pasteCaptureInput.focus()
  if (shouldShowPasteHints) {
    const mark = Date.now()
    clearPasteHintTimer()
    pasteHintTimer = window.setTimeout(() => {
      if (lastPasteEventAt < mark) {
        ElMessage.warning('未捕获到粘贴事件，请确认浏览器允许粘贴或剪贴板是否为文件')
      }
    }, 300)
  }
}

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
const pasteRef = ref<HTMLElement | null>(null)

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

const activatePasteTarget = () => {
  activeTarget = targetRef
  pasteRef.value?.focus()
}

const handlePaste = (event: ClipboardEvent) => {
  if ((event as any)[PASTE_HANDLED_KEY]) return
  ;(event as any)[PASTE_HANDLED_KEY] = true
  if (props.disabled) return
  lastPasteEventAt = Date.now()
  clearPasteHintTimer()
  const files = getClipboardFiles(event)

  if (!files.length) {
    // 检查是否有图片数据在剪贴板中
    const clipboardData = event.clipboardData
    if (clipboardData && clipboardData.items.length > 0) {
      for (let i = 0; i < clipboardData.items.length; i++) {
        const item = clipboardData.items[i]
        if (item.type.indexOf('image') !== -1) {
          const file = item.getAsFile()
          if (file) {
            files.push(file)
          }
        }
      }
    }
    
    if (!files.length) {
      if (shouldShowPasteHints) {
        ElMessage.warning('剪贴板未检测到文件')
      }
      return
    }
  }
  
  event.preventDefault()
  const picked = props.multiple ? files : files.slice(0, 1)
  if (shouldShowPasteHints) {
    ElMessage.success(`已检测到 ${picked.length} 个文件，开始上传`)
  }
  picked.forEach((file, index) => {
    const rawFile = file as File & { uid?: string }
    rawFile.uid = rawFile.uid || `${Date.now()}_${index}`
    uploadRef.value?.handleStart?.(rawFile)
  })
  uploadRef.value?.submit?.()
}

const targetRef: PasteTarget = {
  handlePaste,
  isDisabled: () => props.disabled,
  isVisible: () => {
    const el = pasteRef.value
    if (!el) return false
    const rect = el.getBoundingClientRect()
    return rect.width > 0 || rect.height > 0
  }
}

onMounted(() => {
  pasteTargets.add(targetRef)
  if (!activeTarget) {
    activeTarget = targetRef
  }
  if (!listenerAttached) {
    document.addEventListener('paste', handleGlobalPaste, true)
    document.addEventListener('keydown', handleGlobalKeydown, true)
    listenerAttached = true
  }
  
  // 添加粘贴事件监听器到组件本身
  if (pasteRef.value) {
    pasteRef.value.addEventListener('paste', handlePaste)
  }
})

onBeforeUnmount(() => {
  pasteTargets.delete(targetRef)
  if (activeTarget === targetRef) {
    activeTarget = null
  }
  if (!pasteTargets.size && listenerAttached) {
    document.removeEventListener('paste', handleGlobalPaste, true)
    document.removeEventListener('keydown', handleGlobalKeydown, true)
    listenerAttached = false
    clearPasteHintTimer()
    restorePendingFocus()
    if (pasteCaptureInput?.parentNode) {
      pasteCaptureInput.parentNode.removeChild(pasteCaptureInput)
    }
    pasteCaptureInput = null
  }
  
  // 移除粘贴事件监听器
  if (pasteRef.value) {
    pasteRef.value.removeEventListener('paste', handlePaste)
  }
})

const onSuccess = (res: any, file: any) => {
  emit('success', res, file)
}

const onError = (err: any, file: any) => {
  emit('error', err, file)
}

const onProgress = (e: any) => {
  emit('progress', e)
}
</script>

<style scoped>
.unified-uploader {
  display: block;
}
.unified-uploader-wrapper {
  display: block;
  outline: none;
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