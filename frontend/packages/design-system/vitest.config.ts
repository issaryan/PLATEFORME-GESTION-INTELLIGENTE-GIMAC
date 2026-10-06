import { defineConfig } from 'vitest/config'

// Définition de fini (D-61) : 80 % bloquants sur tout le système de conception (instructions, branches,
// fonctions, lignes), les entrées du catalogue vivant rendues comprises (test/catalogue.test.tsx), et sur
// chaque composite ajouté par les tranches (pied de page, D-62, D-63).
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
        statements: 80, branches: 80, functions: 80, lines: 80,
        'src/enveloppe/Footer/**': { statements: 80, branches: 80, functions: 80, lines: 80 },
        // Composites et extensions de la console et de « Mon compte » (D-62).
        '{src/affichage/OneTimeSecret/**,src/saisie/PermissionPicker/**,src/donnees/LiveStatGroup/**,src/saisie/Checkbox/**,src/saisie/CheckboxGroup/**,src/saisie/TextField/**}': { statements: 80, branches: 80, functions: 80, lines: 80 },
        // Composites de la tranche DDRM et du cadrage (D-63).
        // Modales de saisie et coquille applicative (D-63, demande du 30/09/2026).
        '{src/affichage/QrCode/**,src/conteneurs/CrudDialog/**,src/conteneurs/ConfirmDialog/**,src/conteneurs/SignDialog/**,src/conteneurs/Dialog/**,src/enveloppe/SideNav/**,src/affichage/StatCard/**}': { statements: 80, branches: 80, functions: 80, lines: 80 },
        '{src/enveloppe/PageHeader/**,src/conteneurs/PageSection/**,src/conteneurs/ToastRegion/**,src/donnees/StatGrid/**,src/navigation/LinkList/**,src/actions/ActionBar/**}': { statements: 80, branches: 80, functions: 80, lines: 80 },
        // Photo de profil, recadrage et menu utilisateur (docs/lot1/mon-compte.md).
        // Graphiques de métriques : supervision et tableaux de bord (demande du 06/10/2026).
        '{src/affichage/Sparkline/**,src/donnees/SerieTemporelle/**,src/donnees/BarresHorizontales/**,src/lib/graphiques.ts}': { statements: 80, branches: 80, functions: 80, lines: 80 },
        '{src/affichage/Avatar/**,src/saisie/AvatarCropper/**,src/enveloppe/TopBar/**,src/domaine/Messagerie/**}': { statements: 80, branches: 80, functions: 80, lines: 80 },
      },
    },
  },
})
