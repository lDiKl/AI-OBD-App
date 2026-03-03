import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    host: true,   // listen on 0.0.0.0 — required inside Docker
    port: 5173,
    proxy: {
      '/api': {
        // In Docker: API_URL=http://api:8000 is set by docker-compose
        // Locally:   falls back to http://localhost:8000
        target: process.env.API_URL ?? 'http://localhost:8000',
        changeOrigin: true,
        // Rewrite Location headers on 3xx redirects so the browser follows
        // them through the proxy (localhost:5173) instead of docker-internal host
        autoRewrite: true,
      },
    },
  },
})
