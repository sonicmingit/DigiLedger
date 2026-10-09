<template>
  <div class="mobile-uploader" @paste.stop.prevent="handlePaste">
    <div v-for="(item, index) in internalValue" :key="item.objectKey ?? item.url ?? index" class="mobile-uploader-item">
      <img v-if="isImage(item)" :src="buildOssUrl(item.url)" :alt="item.name || '图片附件'" />
      <span v-else class="mobile-file-name">{{ item.name || '文件附件' }}</span>
      <button type="button" class="mobile-uploader-remove" @click="remove(index)">×</button>
    </div>
    <label class="mobile-uploader-add">
      <input
        ref="inputRef"
        type="file"
        :accept="accept"
        :multiple="multiple"
        @change="handleSelect"
      />
      +
    </label>
    <div v-if="error" class="mobile-toast">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { uploadFile } from '@/api/file'
import { buildOssUrl } from '@/utils/storage'

export interface MobileAttachment {
  name?: string
  url: string
  objectKey?: string
}

const props = withDefaults(
  defineProps<{
    modelValue: MobileAttachment[]
    multiple?: boolean
    accept?: string
  }>(),
  {
    modelValue: () => [],
    multiple: true,
    accept: 'image/*,.pdf,.txt,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip'
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: MobileAttachment[]): void
  (e: 'uploaded', value: MobileAttachment): void
}>()

const internalValue = ref<MobileAttachment[]>([...props.modelValue])
const inputRef = ref<HTMLInputElement | null>(null)
const error = ref('')
const isImage = (item: MobileAttachment) => /\.(png|jpe?g|webp|gif)(?:[?#]|$)/i.test(item.name || item.url)

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

watch(
  () => props.modelValue,
  (val) => {
    internalValue.value = [...val]
  }
)

const handleSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files || files.length === 0) return
  error.value = ''

  for (const file of Array.from(files)) {
    if (!isAcceptedFile(file)) continue
    await processFile(file)
  }

  emit('update:modelValue', internalValue.value)
  if (inputRef.value) inputRef.value.value = ''
}

const handlePaste = async (event: ClipboardEvent) => {
  const items = event.clipboardData?.items
  if (!items?.length) return
  error.value = ''

  const files = Array.from(items)
    .filter((item) => item.kind === 'file')
    .map((item) => item.getAsFile())
    .filter((file): file is File => !!file && isAcceptedFile(file))

  if (files.length === 0) return

  for (const file of props.multiple ? files : files.slice(0, 1)) {
    await processFile(file)
  }

  emit('update:modelValue', internalValue.value)
}

const processFile = async (file: File) => {
    try {
      const data = await uploadFile(file)
      const attachment: MobileAttachment = {
        name: file.name,
        url: buildOssUrl(data.url || data.objectKey),
        objectKey: data.objectKey
      }
      if (props.multiple) internalValue.value.push(attachment)
      else internalValue.value = [attachment]
      emit('uploaded', attachment)
    } catch (e) {
      error.value = (e as Error).message || '上传失败，请检查网络后重试'
    }
  }

const remove = (index: number) => {
  internalValue.value.splice(index, 1)
  emit('update:modelValue', internalValue.value)
}
</script>

<style scoped>
input[type='file'] {
  display: none;
}
.mobile-file-name {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 8px;
  color: #3e5137;
  font-size: 11px;
  text-align: center;
  overflow-wrap: anywhere;
}
</style>
