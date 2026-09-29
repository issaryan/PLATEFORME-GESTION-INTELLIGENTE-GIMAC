import { defineConfig, devices } from '@playwright/test'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

// Tests de bout en bout (D-61, D-62) : scénarios par acteur sur les constructions instrumentées
// (npm run e2e:construire), servies par vite preview. Chromium préinstallé (/opt/pw-browsers).
const ici = dirname(fileURLToPath(import.meta.url))
const apercu = (app: string, sortie: string, port: number) => ({
  command: `npx vite preview --outDir ${sortie} --port ${port} --strictPort`,
  cwd: resolve(ici, '..', 'apps', app),
  url: `http://localhost:${port}/`,
  reuseExistingServer: !process.env.CI,
  timeout: 60_000,
})

export default defineConfig({
  testDir: './specs',
  outputDir: './resultats',
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
    apercu('console', 'dist-e2e', 5202),
    apercu('console', 'dist-e2e-http', 5212),
    apercu('departement', 'dist-e2e', 5200),
    apercu('departement', 'dist-e2e-http', 5210),
    apercu('portail', 'dist-e2e', 5201),
  ],
})
