<template>
  <ThemeProvider>
    <RouterView v-if="isMobileRoute" />
    <AppLayout v-else />
  </ThemeProvider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import ThemeProvider from '@/components/ThemeProvider.vue'
import AppLayout from '@/components/AppLayout.vue'

const route = useRoute()
const isMobileRoute = computed(() => route.matched.some((record) => record.meta.layout === 'mobile'))
</script>
