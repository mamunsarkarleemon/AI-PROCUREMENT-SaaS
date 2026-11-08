import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Backend address. Change with:  $env:VITE_BACKEND_URL="http://localhost:8091"; npm run dev
const backend = process.env.VITE_BACKEND_URL ?? 'http://localhost:8081'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    // The browser calls /api/... and /uploads/... on the Vite server, which forwards them to Spring Boot.
    proxy: {
      '/api': { target: backend, changeOrigin: true },
      '/uploads': { target: backend, changeOrigin: true },
    },
  },
})
