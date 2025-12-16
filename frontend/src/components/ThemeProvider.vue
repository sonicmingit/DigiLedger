<template>
  <slot />
</template>

<script setup lang="ts">
import { provide, ref, watch } from 'vue'
import { ThemeSymbol, type ThemeName, type ThemeOption } from '@/composables/theme'

const STORAGE_KEY = 'digiledger-theme'

const themeVariables: Record<ThemeName, Record<string, string>> = {
  light: {
    'color-bg': '#f9fafb',
    'color-bg-alt': '#f3f4f6',
    'color-header-bg': '#ffffff',
    'color-sidebar-bg': '#ffffff',
    'color-sidebar-text': '#1f2937',
    'color-text': '#111827',
    'color-muted': '#6b7280',
    'color-accent': '#059669',
    'color-accent-soft': '#d1fae5',
    'color-card': '#ffffff',
    'el-color-primary': '#059669',
    'el-color-primary-light-3': '#34d399',
    'el-color-primary-light-5': '#6ee7b7',
    'el-color-primary-light-7': '#a7f3d0',
    'el-color-primary-light-8': '#d1fae5',
    'el-color-primary-light-9': '#ecfdf3',
    'el-color-primary-dark-2': '#047857'
  },
  dark: {
    'color-bg': '#0f172a',
    'color-bg-alt': '#0f172a',
    'color-header-bg': '#1e293b',
    'color-sidebar-bg': '#111827',
    'color-sidebar-text': '#94a3b8',
    'color-text': '#e2e8f0',
    'color-muted': '#cbd5f5',
    'color-accent': '#38bdf8',
    'color-accent-soft': 'rgba(56, 189, 248, 0.2)',
    'color-card': '#1e293b',
    'el-color-primary': '#38bdf8',
    'el-color-primary-light-3': '#7dd3fc',
    'el-color-primary-light-5': '#a5e2ff',
    'el-color-primary-light-7': '#c7efff',
    'el-color-primary-light-8': 'rgba(56, 189, 248, 0.2)',
    'el-color-primary-light-9': 'rgba(56, 189, 248, 0.12)',
    'el-color-primary-dark-2': '#0ea5e9'
  },
  neon: {
    'color-bg': '#050114',
    'color-bg-alt': '#0a0224',
    'color-header-bg': '#150537',
    'color-sidebar-bg': '#0f0329',
    'color-sidebar-text': '#c084fc',
    'color-text': '#f5f3ff',
    'color-muted': '#c084fc',
    'color-accent': '#f97316',
    'color-accent-soft': 'rgba(249, 115, 22, 0.24)',
    'color-card': '#150537',
    'el-color-primary': '#f97316',
    'el-color-primary-light-3': '#fb923c',
    'el-color-primary-light-5': '#fdba74',
    'el-color-primary-light-7': '#fed7aa',
    'el-color-primary-light-8': 'rgba(249, 115, 22, 0.24)',
    'el-color-primary-light-9': 'rgba(249, 115, 22, 0.14)',
    'el-color-primary-dark-2': '#ea580c'
  }
}

const options: ThemeOption[] = [
  { label: '浅色', value: 'light' },
  { label: '深色', value: 'dark' },
  { label: '霓虹', value: 'neon' }
]

const theme = ref<ThemeName>('light')

if (typeof window !== 'undefined') {
  const stored = localStorage.getItem(STORAGE_KEY) as ThemeName | null
  if (stored && stored in themeVariables) {
    theme.value = stored
  }
}

const applyTheme = (name: ThemeName) => {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  root.setAttribute('data-theme', name)
  const variables = themeVariables[name]
  Object.entries(variables).forEach(([key, value]) => {
    root.style.setProperty(`--${key}`, value)
  })
}

const setTheme = (value: ThemeName) => {
  theme.value = value
}

provide(ThemeSymbol, { theme, setTheme, options })

watch(
  theme,
  (value) => {
    applyTheme(value)
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, value)
    }
  },
  { immediate: true }
)
</script>
