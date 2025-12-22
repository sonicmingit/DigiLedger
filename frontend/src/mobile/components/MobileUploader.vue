<template>
  <div 
    ref="uploaderContainer"
    class="mobile-uploader"
    @paste="handlePaste"
    tabindex="0"
  >
    <div v-for="(item, index) in internalValue" :key="item.objectKey ?? item.url ?? index" class="mobile-uploader-item">
      <img :src="item.url" :alt="item.name || '附件'" />
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
const uploaderContainer = ref<HTMLDivElement | null>(null)

watch(
  () => props.modelValue,
  (val) => {
    internalValue.value = [...val]
  }
)

// 处理粘贴事件
const handlePaste = async (event: ClipboardEvent) => {
  const items = event.clipboardData?.items
  if (!items) return
  
  const imageItems = []
  for (let i = 0; i < items.length; i++) {
    if (items[i].type.indexOf('image') !== -1) {
      imageItems.push(items[i])
    }
  }
  
  if (imageItems.length > 0) {
    event.preventDefault()
    error.value = ''
    
    for (const item of imageItems) {
      try {
        const file = item.getAsFile()
        if (file) {
          const data = await uploadFile(file)
          const attachment: MobileAttachment = {
            name: file.name || `clipboard-image-${Date.now()}.png`,
            url: data.url,
            objectKey: data.objectKey
          }
          internalValue.value.push(attachment)
          emit('uploaded', attachment)
        }
      } catch (e) {
        error.value = '上传失败，请检查网络后重试'
      }
    }
    
    emit('update:modelValue', internalValue.value)
  }
}

const handleSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files
  if (!files || files.length === 0) return
  error.value = ''

  for (const file of Array.from(files)) {
    try {
      const data = await uploadFile(file)
      const attachment: MobileAttachment = {
        name: file.name,
        url: data.url,
        objectKey: data.objectKey
      }
      internalValue.value.push(attachment)
      emit('uploaded', attachment)
    } catch (e) {
      error.value = '上传失败，请检查网络后重试'
    }
  }

  emit('update:modelValue', internalValue.value)
  if (inputRef.value) inputRef.value.value = ''
}

const remove = (index: number) => {
  internalValue.value.splice(index, 1)
  emit('update:modelValue', internalValue.value)
}

// 组件挂载时添加事件监听器
import { onMounted, onBeforeUnmount } from 'vue'

onMounted(() => {
  if (uploaderContainer.value) {
    uploaderContainer.value.addEventListener('paste', handlePaste as EventListener)
  }
})

onBeforeUnmount(() => {
  if (uploaderContainer.value) {
    uploaderContainer.value.removeEventListener('paste', handlePaste as EventListener)
  }
})
</script>

<style scoped>
input[type='file'] {
  display: none;
}

.mobile-uploader {
  outline: none;
}
</style>