import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import istanbul from 'vite-plugin-istanbul'
import type { PluginOption } from 'vite'

// Instrumentation istanbul des constructions de bout en bout (GIMAC_E2E=1) : seul le code livré par la
// décision D-62 est mesuré (console, « Mon compte », réauthentification et déconnexion du socle, points
// d'entrée des trois couches). Les tests Playwright recueillent window.__coverage__ ; nyc impose 80 %.
const racine = resolve(dirname(fileURLToPath(import.meta.url)), '..')

export const PERIMETRE_E2E = [
  'apps/console/src/**',
  'apps/departement/src/App.tsx',
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
