<template><button class="pill-button primary-button" :disabled="disabled || loading" @click="$emit('click')"><img v-if="icon" :src="icon" alt="" /><span>{{ loading ? '处理中' : label }}</span></button></template>
<script setup lang="ts">
/** Figma-faithful action button with keyboard, loading and disabled states. */
defineProps<{ label: string; icon?: string; loading?: boolean; disabled?: boolean }>()
defineEmits<{ click: [] }>()
</script>
