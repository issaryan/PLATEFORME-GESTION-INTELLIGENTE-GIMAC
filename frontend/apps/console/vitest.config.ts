import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Définition de fini (D-61) : 80 % bloquants sur la console (D-62).
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    // Services réels par défaut (passerelle) ; les tests d'écran tournent sur les services simulés, demandés
    // explicitement comme en démonstration. Les tests « services réels » posent VITE_SERVICES=http ou le retirent.
    env: { VITE_SERVICES: 'simule' },
    include: ['src/**/*.test.{ts,tsx}'],
    testTimeout: 30000,
    setupFiles: ['src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/main.tsx'],
      reporter: ['text-summary', 'html', 'lcov'],
      thresholds: { statements: 80, branches: 80, functions: 80, lines: 80 },
    },
  },
})
