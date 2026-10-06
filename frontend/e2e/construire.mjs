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
// Adresses des trois applications servies par vite preview (playwright.config.ts) : aiguillage des comptes
// entre applications (D-79) et liens vers la console ; services simulés sur base..base+2, passerelle sur base+10..base+12.
const BASE = Number(process.env.GIMAC_E2E_PORT ?? 5200)
const adresses = (decalage) => ({
  VITE_URL_DES: `http://localhost:${BASE + decalage}`,
  VITE_URL_PORTAIL: `http://localhost:${BASE + decalage + 1}`,
  VITE_URL_CONSOLE: `http://localhost:${BASE + decalage + 2}`,
  // Ports de l'accès direct : liens calculés sur l'hôte de la page ouverte sous un autre nom (127.0.0.1).
  VITE_PORT_DES: String(BASE + decalage),
  VITE_PORT_PORTAIL: String(BASE + decalage + 1),
  VITE_PORT_CONSOLE: String(BASE + decalage + 2),
})
const simule = { VITE_SERVICES: 'simule', ...adresses(0) }
const passerelle = { VITE_SERVICES: 'http', ...adresses(10) }
const constructions = [
  { app: 'console', sortie: `dist-e2e${s}`, env: simule },
  { app: 'console', sortie: `dist-e2e-http${s}`, env: passerelle },
  { app: 'departement', sortie: `dist-e2e${s}`, env: simule },
  { app: 'departement', sortie: `dist-e2e-http${s}`, env: passerelle },
  { app: 'portail', sortie: `dist-e2e${s}`, env: simule },
  { app: 'portail', sortie: `dist-e2e-http${s}`, env: passerelle },
]
for (const c of constructions) {
  console.log(`Construction instrumentée de ${c.app} (${c.sortie})`)
  execSync(`npx vite build --outDir ${c.sortie} --emptyOutDir --logLevel warn`, {
    cwd: resolve(racine, 'apps', c.app),
    stdio: 'inherit',
    env: { ...process.env, GIMAC_E2E: '1', ...c.env },
  })
}
