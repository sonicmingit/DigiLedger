<template>
  <div class="mobile-uploader">
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
</script>

<style scoped>
input[type='file'] {
  display: none;
}
</style>
