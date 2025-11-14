<template>
  <div class="mobile-empty">
    <img :src="illustration" alt="空状态" />
    <p>{{ description }}</p>
    <button v-if="actionLabel" type="button" @click="emit('action')">{{ actionLabel }}</button>
  </div>
</template>

<script setup lang="ts">
import { toRefs } from 'vue'

const props = withDefaults(
  defineProps<{
    description: string
    actionLabel?: string
    illustration?: string
  }>(),
  {
    illustration: 'https://cdn.jsdelivr.net/gh/itellboy/assets@main/empty-box.svg'
  }
)

const emit = defineEmits<{ (e: 'action'): void }>()

const { description, actionLabel, illustration } = toRefs(props)
</script>

<style scoped>
.mobile-empty {
  padding: 80px 20px;
  text-align: center;
  color: var(--mobile-muted);
}

.mobile-empty button {
  margin-top: 16px;
  border: none;
  border-radius: 999px;
  padding: 12px 28px;
  background: linear-gradient(130deg, var(--mobile-green-start), var(--mobile-green-end));
  color: #fff;
  font-size: 14px;
}
</style>
