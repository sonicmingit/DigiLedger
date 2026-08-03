<template>
  <div class="mobile-uploader" @paste.stop.prevent="handlePaste">
    <div v-for="(item, index) in internalValue" :key="item.objectKey ?? item.url ?? index" class="mobile-uploader-item">
      <img :src="buildOssUrl(item.url)" :alt="item.name || '附件'" />
      <button type="button" class="mobile-uploader-remove" @click="remove(index)">×</button>
    </div>
    <label class="mobile-uploader-add">
      <input
        ref="inputRef"
        type="file"
        :accept="accept"
        :multiple="multiple"
        capture="environment"
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
    accept: 'image/*'
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: MobileAttachment[]): void
  (e: 'uploaded', value: MobileAttachment): void
}>()

const internalValue = ref<MobileAttachment[]>([...props.modelValue])
const inputRef = ref<HTMLInputElement | null>(null)
const error = ref('')

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
    if (!file.type.startsWith('image/') || !isAcceptedFile(file)) continue
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
    .filter((file): file is File => !!file && file.type.startsWith('image/') && isAcceptedFile(file))

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
      internalValue.value.push(attachment)
      emit('uploaded', attachment)
    } catch (e) {
      error.value = '上传失败，请检查网络后重试'
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
</style>
