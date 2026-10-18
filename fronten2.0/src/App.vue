<template>
  <RouterView v-if="route.name === 'login'" />
  <AppShell v-else />
</template>

<script setup lang="ts">
import AppShell from '@/components/AppShell.vue'
import { RouterView, useRoute } from 'vue-router'
const route = useRoute()
</script>
