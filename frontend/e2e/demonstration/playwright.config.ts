import { defineConfig, devices } from '@playwright/test'
import { existsSync, readdirSync } from 'node:fs'
import { homedir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

// Preuves de recette sur services réels (docs/recette/demonstration/), reprises du parcours DEMO-SBX : jouées contre le
// déploiement local, sans serveur de prévisualisation. Adresses lues dans l'environnement ; un seul ouvrier.
const ici = dirname(fileURLToPath(import.meta.url))

/**
 * Chromium : GIMAC_CHROMIUM, sinon le plus récent des navigateurs installés (PLAYWRIGHT_BROWSERS_PATH, par exemple
 * /opt/pw-browsers, puis le cache de Playwright du poste), sinon celui de Playwright. Aucun téléchargement.
 */
function chromium(): string | undefined {
  if (process.env.GIMAC_CHROMIUM) return process.env.GIMAC_CHROMIUM
  for (const cache of [process.env.PLAYWRIGHT_BROWSERS_PATH, '/opt/pw-browsers', join(homedir(), '.cache', 'ms-playwright')]) {
    if (!cache || !existsSync(cache)) continue
    const versions = readdirSync(cache).filter((d) => /^chromium-\d+$/.test(d)).sort((a, b) => Number(b.slice(9)) - Number(a.slice(9)))
    const trouve = versions.flatMap((v) => ['chrome-linux64', 'chrome-linux'].map((d) => join(cache, v, d, 'chrome'))).find((c) => existsSync(c))
    if (trouve) return trouve
  }
  return undefined
}
const NAVIGATEUR = chromium()

export default defineConfig({
  testDir: resolve(ici, 'etapes'),
  outputDir: resolve(ici, 'resultats'),
  timeout: 180_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    locale: 'fr-FR',
    timezoneId: 'Africa/Douala',
    viewport: { width: 1440, height: 900 },
    trace: 'retain-on-failure',
    launchOptions: NAVIGATEUR ? { executablePath: NAVIGATEUR } : {},
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } }],
})
