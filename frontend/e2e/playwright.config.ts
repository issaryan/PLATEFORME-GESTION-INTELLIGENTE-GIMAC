import { defineConfig, devices } from '@playwright/test'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

// Tests de bout en bout (D-61, D-62) : scénarios par acteur sur les constructions instrumentées
// (npm run e2e:construire), servies par vite preview. Chromium préinstallé (/opt/pw-browsers).
const ici = dirname(fileURLToPath(import.meta.url))
// Exécutions parallèles sur un même poste (plusieurs agents ou branches) : GIMAC_E2E_PORT déplace les six
// ports de prévisualisation (base, base+1, base+2 et base+10 à base+12), GIMAC_E2E_SORTIE suffixe les
// dossiers de construction (dist-e2e<suffixe>) et de résultats. Par défaut : 5200 et aucun suffixe.
const BASE = Number(process.env.GIMAC_E2E_PORT ?? 5200)
const SUFFIXE = process.env.GIMAC_E2E_SORTIE ?? ''
const apercu = (app: string, sortie: string, port: number) => ({
  command: `npx vite preview --outDir ${sortie} --port ${port} --strictPort`,
  cwd: resolve(ici, '..', 'apps', app),
  url: `http://localhost:${port}/`,
  reuseExistingServer: !process.env.CI,
  timeout: 60_000,
})

export default defineConfig({
  testDir: './specs',
  outputDir: `./resultats${SUFFIXE}`,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  workers: process.env.CI ? 2 : 4,
  retries: 0,
  reporter: [['list']],
  use: {
    locale: 'fr-FR',
    timezoneId: 'Africa/Douala',
    viewport: { width: 1440, height: 900 },
    trace: 'retain-on-failure',
    launchOptions: { executablePath: process.env.GIMAC_CHROMIUM ?? '/opt/pw-browsers/chromium' },
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } }],
  webServer: [
    apercu('console', `dist-e2e${SUFFIXE}`, BASE + 2),
    apercu('console', `dist-e2e-http${SUFFIXE}`, BASE + 12),
    apercu('departement', `dist-e2e${SUFFIXE}`, BASE),
    apercu('departement', `dist-e2e-http${SUFFIXE}`, BASE + 10),
    apercu('portail', `dist-e2e${SUFFIXE}`, BASE + 1),
    apercu('portail', `dist-e2e-http${SUFFIXE}`, BASE + 11),
  ],
})
