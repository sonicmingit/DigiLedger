<template>
  <slot />
</template>

<script setup lang="ts">
import { provide, ref, watch } from 'vue'
import { ThemeSymbol, type ThemeName, type ThemeOption } from '@/composables/theme'

const STORAGE_KEY = 'digiledger-theme'

const themeVariables: Record<ThemeName, Record<string, string>> = {
  light: {
    'color-bg': '#f8fafc',
    'color-bg-alt': '#e2e8f0',
    'color-header-bg': '#f1f5f9',
    'color-sidebar-bg': '#ffffff',
    'color-sidebar-text': '#475569',
    'color-text': '#0f172a',
    'color-muted': '#64748b',
    'color-accent': '#2563eb',
    'color-accent-soft': 'rgba(37, 99, 235, 0.12)',
    'color-card': '#ffffff'
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
    'color-card': '#1e293b'
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
    'color-card': '#150537'
  }
}

const options: ThemeOption[] = [
  { label: '浅色', value: 'light' },
  { label: '深色', value: 'dark' },
  { label: '霓虹', value: 'neon' }
]

const theme = ref<ThemeName>('dark')

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
