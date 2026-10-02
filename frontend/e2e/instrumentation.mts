import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import istanbul from 'vite-plugin-istanbul'
import type { PluginOption } from 'vite'

// Instrumentation istanbul des constructions de bout en bout (GIMAC_E2E=1). Code mesuré : console et
// administration (@gimac/administration, servie aussi dans le département, D-69),
// « Mon compte », réauthentification et déconnexion du socle (D-62), points d'entrée des trois couches, et
// l'application du département : ses profils, les écrans et compositions des étapes 1 à 3 du parcours
// (tableau de bord par acteur, registre et dossier des institutions, cadrage et liaisons), leurs
// descripteurs et l'exécuteur d'actions (D-57, D-58, D-61) ; le parcours de certification des étapes 3
// à 10 (tableau de bord « Mon travail » et pilotage, liste et dossier numérique des liaisons, décision et
// mise en service, D-56 à D-59), la messagerie (D-64) et le portail du participant. Les services (HTTP et
// simulés) sont couverts par les tests de contrat de Vitest. Les tests Playwright recueillent
// window.__coverage__ ; nyc impose 80 %.
const racine = resolve(dirname(fileURLToPath(import.meta.url)), '..')

export const PERIMETRE_E2E = [
  'apps/console/src/**',
  'packages/administration/src/**',
  'apps/departement/src/**',
  'packages/lot1/src/Fournisseurs.tsx',
  'packages/lot1/src/pages/des/TableauDeBordPage.tsx',
  'packages/lot1/src/pages/des/Institution*.tsx',
  'packages/lot1/src/pages/des/institutions/**',
  'packages/lot1/src/pages/des/Liaison*.tsx',
  'packages/lot1/src/pages/des/liaisons/**',
  'packages/lot1/src/ressources/actions.tsx',
  'packages/lot1/src/ressources/descripteur.ts',
  'packages/lot1/src/ressources/institution.tsx',
  'packages/lot1/src/ressources/liaison.tsx',
  'packages/lot1/src/pages/des/tableau/**',
  'packages/lot1/src/pages/des/parcours/**',
  'packages/lot1/src/pages/des/messagerie/**',
  'packages/lot1/src/pages/portail/**',
  'apps/portail/src/App.tsx',
  'packages/compte/src/**',
  'packages/socle/src/reauth/**',
  'packages/socle/src/session/**',
]
export const HORS_PERIMETRE_E2E = ['**/*.test.{ts,tsx}', '**/test/**', '**/i18n/**', '**/i18n.ts', '**/main.tsx', '**/services/simule/catalogue.ts']

export function pluginsE2E(): PluginOption[] {
  if (process.env.GIMAC_E2E !== '1') return []
  return [
    istanbul({
      cwd: racine,
      include: PERIMETRE_E2E,
      exclude: HORS_PERIMETRE_E2E,
      extension: ['.ts', '.tsx'],
      forceBuildInstrument: true,
      requireEnv: false,
    }),
  ]
}
