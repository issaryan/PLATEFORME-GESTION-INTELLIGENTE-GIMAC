import { defineConfig } from 'vitest/config'

// Définition de fini (D-61) : 80 % bloquants sur le socle d'interface.
export default defineConfig({
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
    testTimeout: 20000,
    coverage: {
      provider: 'v8',
      include: ['src/http/**', 'src/session/**', 'src/reauth/**', 'src/connexion/**', 'src/avatar/**'],
      exclude: ['src/**/*.test.{ts,tsx}'],
      reporter: ['text-summary', 'html', 'lcov'],
      thresholds: { statements: 80, branches: 80, functions: 80, lines: 80 },
    },
  },
})
