import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Définition de fini (D-61) : 80 % bloquants sur l'administration (D-62, D-69).
export default defineConfig({
  plugins: [react()],
  // Le test de concordance du catalogue simulé lit le domaine Keycloak généré (infra/keycloak/gimac-realm.json, ADM-01).
  server: { fs: { allow: ['../../..'] } },
  test: {
    environment: 'jsdom',
    // Les tests d'écran tournent sur les services simulés, demandés explicitement comme en démonstration.
    env: { VITE_SERVICES: 'simule' },
    include: ['src/**/*.test.{ts,tsx}'],
    testTimeout: 30000,
    setupFiles: ['src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/i18n/**', 'src/index.ts', 'src/services/simule/catalogue.ts'],
      reporter: ['text-summary', 'html', 'lcov'],
      thresholds: { statements: 80, branches: 80, functions: 80, lines: 80 },
    },
  },
})
