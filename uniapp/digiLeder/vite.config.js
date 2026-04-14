import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [
        uni(),
    ],
    server: {
        port: 5174,
        proxy: {
            // 将所有 /api 前缀的请求代理转发到后端
            '/api': {
                target: process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:18080',
                changeOrigin: true,
                // 如果后端接口本身没有 /api 前缀，取消注释下面这行
                // rewrite: (path) => path.replace(/^\/api/, '')
            }
        }
    }
})
