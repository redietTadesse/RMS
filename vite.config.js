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
    watch: {
      ignored: ignoredPaths,
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
})

