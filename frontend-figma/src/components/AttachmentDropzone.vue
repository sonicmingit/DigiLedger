<template>
  <div
    class="attachment-dropzone"
    :class="{ compact, panel, disabled, dragging }"
    tabindex="0"
    role="button"
    :aria-label="label"
    @click="chooseFiles"
    @keydown.enter.prevent="chooseFiles"
    @keydown.space.prevent="chooseFiles"
    @dragenter.prevent="dragging = true"
    @dragover.prevent="dragging = true"
    @dragleave.prevent="dragging = false"
    @drop.prevent="handleDrop"
    @paste="handlePaste"
  >
    <input ref="fileInput" class="file-input" type="file" :accept="accept" :multiple="multiple" @change="handleChange" />
    <span class="dropzone-icon">＋</span>
    <span class="dropzone-copy"><strong>{{ label }}</strong><small>{{ hint }}</small></span>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(defineProps<{
  label?: string
  hint?: string
  accept?: string
  multiple?: boolean
  compact?: boolean
  panel?: boolean
  disabled?: boolean
}>(), {
  label: '上传附件',
  hint: '拖拽文件、点击选择，或直接粘贴截图',
  accept: '',
  multiple: true,
  compact: false,
  panel: false,
  disabled: false
})
const emit = defineEmits<{ files: [files: File[]] }>()
const fileInput = ref<HTMLInputElement>()
const dragging = ref(false)

function emitFiles(files: File[]) {
  const selected = props.multiple ? files : files.slice(0, 1)
  if (selected.length && !props.disabled) emit('files', selected)
}
function chooseFiles() { if (!props.disabled) fileInput.value?.click() }
function handleChange(event: Event) {
  emitFiles(Array.from((event.target as HTMLInputElement).files || []))
  ;(event.target as HTMLInputElement).value = ''
}
function handleDrop(event: DragEvent) {
  dragging.value = false
  emitFiles(Array.from(event.dataTransfer?.files || []))
}
function handlePaste(event: ClipboardEvent) {
  const files = Array.from(event.clipboardData?.files || [])
  if (files.length) {
    event.preventDefault()
    emitFiles(files)
  }
}
</script>

<style scoped>
.attachment-dropzone{display:flex;align-items:center;gap:10px;min-height:72px;width:100%;box-sizing:border-box;padding:12px 14px;border:1px dashed #cdd4c6;border-radius:16px;background:#fafbf8;color:var(--dl-text-secondary);cursor:pointer;outline:none;transition:.18s ease}.attachment-dropzone:hover,.attachment-dropzone:focus-visible,.attachment-dropzone.dragging{border-color:#8acb26;background:var(--dl-accent-soft);color:var(--dl-text)}.attachment-dropzone.disabled{pointer-events:none;opacity:.55}.attachment-dropzone.compact{min-height:58px}.attachment-dropzone.panel{min-height:164px;justify-content:center;padding:24px;border-color:#9fdccf;background:#fff}.attachment-dropzone.panel .dropzone-icon{width:40px;height:40px;font-size:26px}.attachment-dropzone.panel .dropzone-copy{align-items:center;text-align:center}.attachment-dropzone.panel .dropzone-copy strong{font-size:14px}.attachment-dropzone.panel .dropzone-copy small{white-space:normal;font-size:11px}.file-input{display:none}.dropzone-icon{display:grid;place-items:center;width:28px;height:28px;border-radius:50%;background:#edf1e7;color:#547417;font-size:19px;font-weight:400}.dropzone-copy{display:flex;flex-direction:column;gap:3px;min-width:0}.dropzone-copy strong{font-size:12px}.dropzone-copy small{overflow:hidden;color:var(--dl-muted);font-size:10px;text-overflow:ellipsis;white-space:nowrap}
</style>
