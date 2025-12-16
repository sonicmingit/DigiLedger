<template>
  <component :is="asCard ? ElCard : 'section'" :class="rootClass" shadow="never">
    <div class="dl-page-header__inner">
      <div class="dl-page-header__meta">
        <component :is="titleTag" class="dl-page-header__title">{{ title }}</component>
        <p v-if="subtitle" class="dl-page-header__subtitle">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.actions" class="dl-page-header__actions">
        <slot name="actions" />
      </div>
    </div>
    <div v-if="$slots.default" class="dl-page-header__below">
      <slot />
    </div>
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElCard } from 'element-plus'

type Variant = 'standard' | 'hero'

const props = withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    variant?: Variant
    asCard?: boolean
    titleTag?: 'h1' | 'h2'
  }>(),
  {
    variant: 'standard',
    asCard: true,
    titleTag: 'h1'
  }
)

const rootClass = computed(() => ({
  'dl-page-header': true,
  'dl-page-header--hero': props.variant === 'hero'
}))
</script>

<style scoped>
.dl-page-header {
  border-radius: var(--dl-radius-lg);
  border: 1px solid var(--el-border-color-lighter);
  background: var(--dl-card);
}

.dl-page-header--hero {
  background: linear-gradient(140deg, var(--dl-card), var(--el-color-primary-light-9));
  box-shadow: var(--dl-shadow-lg);
}

.dl-page-header__inner {
  display: flex;
  gap: var(--dl-space-4);
  align-items: flex-start;
  justify-content: space-between;
  padding: var(--dl-space-6);
}

.dl-page-header--hero .dl-page-header__inner {
  padding: 28px;
}

.dl-page-header__meta {
  min-width: 0;
}

.dl-page-header__title {
  margin: 0;
  font-size: var(--dl-font-h1);
  font-weight: 700;
  color: var(--dl-text);
}

.dl-page-header--hero .dl-page-header__title {
  color: var(--el-color-primary-dark-2);
}

.dl-page-header__subtitle {
  margin: var(--dl-space-2) 0 0;
  color: var(--dl-muted);
  font-size: var(--dl-font-body);
}

.dl-page-header__actions {
  display: flex;
  gap: var(--dl-space-3);
  align-items: center;
  flex-wrap: wrap;
}

.dl-page-header__below {
  padding: 0 var(--dl-space-6) var(--dl-space-5);
}
</style>

