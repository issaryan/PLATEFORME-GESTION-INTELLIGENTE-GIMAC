import { defineConfig } from 'vitest/config'

// Définition de fini (REC-GIMAC-01, D-61) : 80 % bloquants sur le code des tranches livrées ;
// ailleurs, cliquet au niveau atteint (la couverture ne peut que monter jusqu'à 80 %).
const TRANCHE_DDRM = '{src/pages/des/Institution*.tsx,src/pages/des/TableauDeBordPage.tsx,src/pages/des/institutions/**,src/ressources/institution.tsx,src/services/http/participant.ts,src/services/simule/participant.ts,src/hooks/participant.ts}'

export default defineConfig({
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
    testTimeout: 20000,
    setupFiles: ['src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/test/**', 'src/i18n/**', 'src/**/index.ts'],
      reporter: ['text-summary', 'html', 'lcov'],
      thresholds: {
        statements: 75, branches: 66, functions: 67, lines: 75,
        [TRANCHE_DDRM]: { statements: 80, branches: 80, functions: 80, lines: 80 },
      },
    },
  },
})
