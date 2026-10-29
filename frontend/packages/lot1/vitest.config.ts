import { defineConfig } from 'vitest/config'

// Définition de fini (REC-GIMAC-01, D-61) : 80 % bloquants sur tout le paquet (instructions, branches,
// fonctions, lignes), et en particulier sur les tranches DDRM et cadrage (étapes 1 à 3 du parcours).
const TRANCHES = '{src/pages/des/Institution*.tsx,src/pages/des/TableauDeBordPage.tsx,src/pages/des/institutions/**,src/pages/des/liaisons/cadrage.tsx,src/ressources/institution.tsx,src/services/http/participant.ts,src/services/simule/participant.ts,src/hooks/participant.ts}'
const PROCESSUS_PDF = '{src/services/http/processus-pdf.ts,src/pages/des/processus/pdf.ts,src/services/simule/changement-modele.ts,src/services/simule/reference-processus.ts}'

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
        statements: 80, branches: 80, functions: 80, lines: 80,
        [TRANCHES]: { statements: 80, branches: 80, functions: 80, lines: 80 },
        [PROCESSUS_PDF]: { statements: 98, branches: 98, functions: 98, lines: 98 },
      },
    },
  },
})
