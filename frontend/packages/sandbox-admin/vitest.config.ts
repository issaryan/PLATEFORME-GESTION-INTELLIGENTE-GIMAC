import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Code de la Sandbox GIMAC (D-115, D-122) : 98 % bloquants sur tout le paquet, fichier par fichier.
export default defineConfig({
  plugins: [react()],
  server: { fs: { allow: ['../../..'] } },
  test: {
    environment: 'jsdom',
    env: { VITE_SERVICES: 'simule' },
    include: ['src/**/*.test.{ts,tsx}'],
    testTimeout: 30000,
    setupFiles: ['src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/i18n/fr.ts', 'src/i18n/en.ts', 'src/i18n/es.ts', 'src/modele.ts', 'src/services/ports.ts'],
      reporter: ['text', 'text-summary', 'html', 'lcov'],
      thresholds: { 'src/**/*.{ts,tsx}': { statements: 98, branches: 98, functions: 98, lines: 98 } },
    },
  },
})
