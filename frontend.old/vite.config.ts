import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Proxy is not needed because the frontend calls the gateway at localhost:8080 directly.
    // The gateway has CORS configured to allow http://localhost:5173.
  },
})
