import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    // In development the browser calls /api/... on the Vite server, which forwards to Spring Boot.
    // Same-origin requests mean no CORS preflights while developing.
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
})
