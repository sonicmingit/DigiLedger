<template>
  <slot />
</template>

<script setup lang="ts">
import { provide, ref, watch } from 'vue'
import { ThemeSymbol, type ThemeName, type ThemeOption } from '@/composables/theme'

const STORAGE_KEY = 'digiledger-theme'

const themeVariables: Record<ThemeName, Record<string, string>> = {
  light: {
    'color-bg': '#f5f7fb',
    'color-bg-alt': '#eef2ff',
    'color-header-bg': '#ffffff',
    'color-sidebar-bg': '#ffffff',
    'color-sidebar-text': '#6b7280',
    'color-text': '#111827',
    'color-muted': '#8b95a5',
    'color-accent': '#7c3aed',
    'color-accent-soft': 'rgba(124, 58, 237, 0.12)',
    'color-card': '#ffffff',
    'color-border': '#e5e7eb',
    'color-surface-soft': 'rgba(124, 58, 237, 0.08)',
    'color-surface-ghost': 'rgba(255, 255, 255, 0.9)',
    'color-shadow': '0 18px 40px rgba(112, 99, 211, 0.14)'
  },
  dark: {
    'color-bg': '#0f172a',
    'color-bg-alt': '#0f172a',
    'color-header-bg': '#1f2937',
    'color-sidebar-bg': '#111827',
    'color-sidebar-text': '#cbd5f5',
    'color-text': '#e2e8f0',
    'color-muted': '#cbd5f5',
    'color-accent': '#38bdf8',
    'color-accent-soft': 'rgba(56, 189, 248, 0.2)',
    'color-card': '#1e293b',
    'color-border': '#1f2937',
    'color-surface-soft': 'rgba(56, 189, 248, 0.16)',
    'color-surface-ghost': 'rgba(30, 41, 59, 0.8)',
    'color-shadow': '0 18px 40px rgba(0, 0, 0, 0.45)'
  },
  neon: {
    'color-bg': '#0b0325',
    'color-bg-alt': '#0f042d',
    'color-header-bg': '#150537',
    'color-sidebar-bg': '#0f0329',
    'color-sidebar-text': '#c084fc',
    'color-text': '#f5f3ff',
    'color-muted': '#c084fc',
    'color-accent': '#f97316',
    'color-accent-soft': 'rgba(249, 115, 22, 0.24)',
    'color-card': '#150537',
    'color-border': '#2d0f5a',
    'color-surface-soft': 'rgba(249, 115, 22, 0.2)',
    'color-surface-ghost': 'rgba(32, 7, 73, 0.9)',
    'color-shadow': '0 18px 40px rgba(249, 115, 22, 0.25)'
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
