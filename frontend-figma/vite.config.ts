import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// The proxy keeps local development aligned with the production same-origin `/api` contract.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
    server: {
      port: 5174,
      proxy: { '/api': { target: env.VITE_PROXY_TARGET || 'http://localhost:18080', changeOrigin: true } }
    }
  }
})
