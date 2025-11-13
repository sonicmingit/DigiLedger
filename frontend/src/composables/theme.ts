import type { Ref } from 'vue'
import { inject } from 'vue'

export type ThemeName = 'light' | 'dark' | 'neon'

export interface ThemeOption {
  label: string
  value: ThemeName
}

export interface ThemeContext {
  theme: Ref<ThemeName>
  setTheme: (theme: ThemeName) => void
  options: ThemeOption[]
}

export const ThemeSymbol = Symbol('ThemeProvider')

export const useTheme = () => {
  const context = inject<ThemeContext>(ThemeSymbol)
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return context
}
