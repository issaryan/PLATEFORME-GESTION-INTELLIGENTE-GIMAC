import { defineConfig } from 'vitest/config'

// Définition de fini (D-61) : 80 % bloquants sur les composants ajoutés par les tranches (pied de
// page GIMAC) ; cliquet au niveau atteint sur le catalogue existant, qui ne peut que monter.
export default defineConfig({
  test: {
    environment: 'jsdom',
    include: ['test/**/*.test.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/index.ts', 'src/i18n/libelles.ts'],
      reporter: ['text-summary', 'html', 'lcov'],
      thresholds: {
        statements: 41, branches: 32, functions: 34, lines: 42,
        'src/enveloppe/Footer/**': { statements: 80, branches: 80, functions: 80, lines: 80 },
      },
    },
  },
})
