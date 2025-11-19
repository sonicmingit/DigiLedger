<template>
  <span class="icon-renderer" :title="icon">
    <template v-if="isMdi">
      <img
        v-if="!fallback"
        :src="svgSrc"
        class="tag-icon-svg"
        alt="icon"
        @error="onError"
      />
      <i v-else :class="fontClass" class="tag-icon" />
    </template>
    <template v-else>
      <i v-if="icon" :class="icon" class="tag-icon" />
    </template>
  </span>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
const props = defineProps<{ icon?: string | null }>()
const icon = props.icon || ''
const fallback = ref(false)

// support both `mdi:account` and `mdi-account` formats
const isMdi = computed(() => String(icon).startsWith('mdi:') || String(icon).startsWith('mdi-'))
const name = computed(() => {
  const s = String(icon)
  if (s.startsWith('mdi:')) return s.split(':')[1]
  if (s.startsWith('mdi-')) return s.slice('mdi-'.length)
  return s
})
const svgSrc = computed(() => `/mdi/mdi-${name.value}.svg`)
const fontClass = computed(() => `mdi mdi-${name.value}`)

const onError = () => {
  fallback.value = true
}
</script>

<style scoped>
.tag-icon-svg {
  width: 16px;
  height: 16px;
  vertical-align: text-bottom;
  margin-right: 6px;
}
.tag-icon {
  margin-right: 4px;
}
.icon-renderer {
  display: inline-flex;
  align-items: center;
}
</style>
