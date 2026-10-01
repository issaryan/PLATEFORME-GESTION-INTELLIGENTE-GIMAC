// Constructions instrumentées (istanbul) des couches pour les tests de bout en bout :
// services simulés pour les trois couches (VITE_SERVICES=simule, demandé explicitement : les applications utilisent
// la passerelle par défaut), et les trois couches sur la passerelle (VITE_SERVICES=http)
// dont Playwright simule les réponses. Sorties dist-e2e et dist-e2e-http, jamais versionnées.
import { execSync } from 'node:child_process'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const racine = resolve(dirname(fileURLToPath(import.meta.url)), '..')
// GIMAC_E2E_SORTIE suffixe les dossiers de sortie (exécutions parallèles, voir playwright.config.ts).
const s = process.env.GIMAC_E2E_SORTIE ?? ''
const constructions = [
  { app: 'console', sortie: `dist-e2e${s}`, env: { VITE_SERVICES: 'simule' } },
  { app: 'console', sortie: `dist-e2e-http${s}`, env: { VITE_SERVICES: 'http' } },
  { app: 'departement', sortie: `dist-e2e${s}`, env: { VITE_SERVICES: 'simule' } },
  { app: 'departement', sortie: `dist-e2e-http${s}`, env: { VITE_SERVICES: 'http' } },
  { app: 'portail', sortie: `dist-e2e${s}`, env: { VITE_SERVICES: 'simule' } },
  { app: 'portail', sortie: `dist-e2e-http${s}`, env: { VITE_SERVICES: 'http' } },
]
for (const c of constructions) {
  console.log(`Construction instrumentée de ${c.app} (${c.sortie})`)
  execSync(`npx vite build --outDir ${c.sortie} --emptyOutDir --logLevel warn`, {
    cwd: resolve(racine, 'apps', c.app),
    stdio: 'inherit',
    env: { ...process.env, GIMAC_E2E: '1', ...c.env },
  })
}
