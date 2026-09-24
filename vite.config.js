import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Files that churn constantly (agent tooling, backend logs) must be excluded
// from Vite's watcher AND Tailwind's scanner, otherwise the dev server
// pushes HMR/reloads to the browser every few seconds.
const ignoredPaths = ['**/.autoclaw/**', '**/logs/**', '**/node_modules/**', '**/.git/**']

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: true,        // listen on all interfaces (0.0.0.0)
    port: 5173,
    strictPort: true,
    watch: {
      ignored: ignoredPaths,
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
})