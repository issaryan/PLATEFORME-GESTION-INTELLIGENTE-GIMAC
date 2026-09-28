import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Catalogue vivant du système de conception (DS-GIMAC-01 §12.2, écran /catalogue hors production).
export default defineConfig({
  plugins: [react()],
  server: { port: 4203, strictPort: true },
})
