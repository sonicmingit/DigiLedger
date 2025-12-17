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
    'color-border': '#e5e7eb',
    'color-border-strong': '#d1d5db',
    'color-text': '#0f172a',
    'color-muted': '#6b7280',
    'color-accent': '#059669',
    'color-accent-soft': '#d1fae5',
    'color-card': '#ffffff',
    'dl-shadow-sm': '0 6px 18px rgba(15, 23, 42, 0.04)',
    'dl-shadow-md': '0 10px 30px rgba(15, 23, 42, 0.06)',
    'dl-shadow-lg': '0 16px 40px rgba(15, 23, 42, 0.08)',
    'el-color-primary': '#059669',
    'el-color-primary-light-3': '#34d399',
    'el-color-primary-light-5': '#6ee7b7',
    'el-color-primary-light-7': '#a7f3d0',
    'el-color-primary-light-8': '#d1fae5',
    'el-color-primary-light-9': '#ecfdf3',
    'el-color-primary-dark-2': '#047857',
    'el-bg-color': '#ffffff',
    'el-bg-color-page': '#f9fafb',
    'el-bg-color-overlay': '#ffffff',
    'el-fill-color-blank': '#ffffff',
    'el-fill-color-light': '#f3f4f6',
    'el-fill-color-lighter': '#f9fafb',
    'el-fill-color-extra-light': '#f9fafb',
    'el-text-color-primary': '#0f172a',
    'el-text-color-regular': '#111827',
    'el-text-color-secondary': '#6b7280',
    'el-text-color-placeholder': '#9ca3af',
    'el-text-color-disabled': '#9ca3af',
    'el-border-color': '#e5e7eb',
    'el-border-color-light': '#e5e7eb',
    'el-border-color-lighter': '#eef2f7',
    'el-border-color-extra-light': '#f3f4f6'
  },
  dark: {
    'color-bg': '#0b1220',
    'color-bg-alt': '#0f172a',
    'color-header-bg': '#0f172a',
    'color-sidebar-bg': '#0f172a',
    'color-sidebar-text': '#cbd5e1',
    'color-border': '#1f2937',
    'color-border-strong': '#334155',
    'color-text': '#e2e8f0',
    'color-muted': '#94a3b8',
    'color-accent': '#38bdf8',
    'color-accent-soft': 'rgba(56, 189, 248, 0.2)',
    'color-card': '#111827',
    'dl-shadow-sm': '0 10px 24px rgba(0, 0, 0, 0.28)',
    'dl-shadow-md': '0 14px 36px rgba(0, 0, 0, 0.34)',
    'dl-shadow-lg': '0 18px 46px rgba(0, 0, 0, 0.38)',
    'el-color-primary': '#38bdf8',
    'el-color-primary-light-3': '#7dd3fc',
    'el-color-primary-light-5': '#a5e2ff',
    'el-color-primary-light-7': '#c7efff',
    'el-color-primary-light-8': 'rgba(56, 189, 248, 0.2)',
    'el-color-primary-light-9': 'rgba(56, 189, 248, 0.12)',
    'el-color-primary-dark-2': '#0ea5e9',
    'el-bg-color': '#111827',
    'el-bg-color-page': '#0b1220',
    'el-bg-color-overlay': '#0f172a',
    'el-fill-color-blank': '#111827',
    'el-fill-color-light': '#0f172a',
    'el-fill-color-lighter': '#0b1220',
    'el-fill-color-extra-light': '#0b1220',
    'el-text-color-primary': '#e5e7eb',
    'el-text-color-regular': '#cbd5e1',
    'el-text-color-secondary': '#94a3b8',
    'el-text-color-placeholder': '#64748b',
    'el-text-color-disabled': '#64748b',
    'el-border-color': '#1f2937',
    'el-border-color-light': '#1f2937',
    'el-border-color-lighter': '#233046',
    'el-border-color-extra-light': '#26334a'
  },
  neon: {
    'color-bg': '#050114',
    'color-bg-alt': '#0a0224',
    'color-header-bg': '#150537',
    'color-sidebar-bg': '#0f0329',
    'color-sidebar-text': '#c084fc',
    'color-border': 'rgba(192, 132, 252, 0.22)',
    'color-border-strong': 'rgba(192, 132, 252, 0.35)',
    'color-text': '#f5f3ff',
    'color-muted': '#c084fc',
    'color-accent': '#f97316',
    'color-accent-soft': 'rgba(249, 115, 22, 0.24)',
    'color-card': '#12042f',
    'dl-shadow-sm': '0 10px 24px rgba(0, 0, 0, 0.3)',
    'dl-shadow-md': '0 16px 40px rgba(0, 0, 0, 0.38)',
    'dl-shadow-lg': '0 22px 60px rgba(0, 0, 0, 0.45)',
    'el-color-primary': '#f97316',
    'el-color-primary-light-3': '#fb923c',
    'el-color-primary-light-5': '#fdba74',
    'el-color-primary-light-7': '#fed7aa',
    'el-color-primary-light-8': 'rgba(249, 115, 22, 0.24)',
    'el-color-primary-light-9': 'rgba(249, 115, 22, 0.14)',
    'el-color-primary-dark-2': '#ea580c',
    'el-bg-color': '#12042f',
    'el-bg-color-page': '#050114',
    'el-bg-color-overlay': '#0a0224',
    'el-fill-color-blank': '#12042f',
    'el-fill-color-light': '#0a0224',
    'el-fill-color-lighter': '#050114',
    'el-fill-color-extra-light': '#050114',
    'el-text-color-primary': '#f5f3ff',
    'el-text-color-regular': '#e9d5ff',
    'el-text-color-secondary': '#c084fc',
    'el-text-color-placeholder': 'rgba(192, 132, 252, 0.7)',
    'el-text-color-disabled': 'rgba(192, 132, 252, 0.6)',
    'el-border-color': 'rgba(192, 132, 252, 0.22)',
    'el-border-color-light': 'rgba(192, 132, 252, 0.18)',
    'el-border-color-lighter': 'rgba(192, 132, 252, 0.14)',
    'el-border-color-extra-light': 'rgba(192, 132, 252, 0.12)'
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
