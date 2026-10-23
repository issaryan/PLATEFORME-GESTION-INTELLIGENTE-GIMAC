// Captures du lot homologation de l'épopée EXI (D-122) : application du DES servie par Vite sur les services simulés
// (VITE_SERVICES=simule npx vite --port 5441 dans frontend/apps/departement), Chromium sans tête, thèmes clair et sombre.
//   node frontend/e2e/captures/exi-homologation/capturer.mjs <dossier de sortie> [récit…]
// Chaque récit est une suite d'étapes numérotées clic par clic ; aucune donnée réelle.
import { chromium } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import { join } from 'node:path'

const BASE = process.env.GIMAC_CAPTURES_URL ?? 'http://localhost:5441'
const sortie = process.argv[2] ?? 'captures-exi-homologation'
const demandes = process.argv.slice(3)

/** Récits : profil de démonstration et étapes (nom de fichier, action avant la capture). */
export const RECITS = {
  // US-EXI-17 : onglet « Résultats de la sandbox » du dossier de liaison (DES-09), ingénieur certificateur.
  'exi-17': {
    profil: 'ROLE_SNA',
    etapes: [
      ['01-resultats-campagne', async (p) => {
        await p.goto(`${BASE}/des/parcours/LIA-2026-022?onglet=sandbox`)
        await p.getByText('EXE-000003', { exact: true }).waitFor()
      }],
      ['02-executions-page-suivante', async (p) => {
        const carte = p.locator('.gm-card').filter({ has: p.getByRole('heading', { name: 'Exécutions retenues' }) })
        await carte.getByRole('button', { name: 'Suivante' }).click()
        await p.getByText('EXE-000026', { exact: true }).waitFor()
        await carte.scrollIntoViewIfNeeded()
      }],
      ['03-scenarios-non-rapproches', async (p) => {
        const carte = p.locator('.gm-card').filter({ has: p.getByRole('heading', { name: 'Scénarios non rapprochés' }) })
        await carte.scrollIntoViewIfNeeded()
        await carte.getByText('SCN-INCONNU-2', { exact: true }).waitFor()
      }],
      ['04-sandbox-indisponible', async (p) => {
        await p.goto(`${BASE}/des/parcours/LIA-2026-027?onglet=sandbox`)
        await p.getByText('Dernière relève en échec', { exact: true }).waitFor()
      }],
      ['05-aucune-campagne', async (p) => {
        await p.goto(`${BASE}/des/parcours/LIA-2026-030?onglet=sandbox`)
        await p.getByText('Aucune campagne reprise pour cette liaison', { exact: true }).waitFor()
      }],
    ],
  },
}

const navigateur = await chromium.launch({ headless: true, executablePath: process.env.GIMAC_CHROMIUM })
try {
  for (const [nom, recit] of Object.entries(RECITS)) {
    if (demandes.length && !demandes.includes(nom)) continue
    const dossier = join(sortie, nom)
    mkdirSync(dossier, { recursive: true })
    for (const theme of ['clair', 'sombre']) {
      const contexte = await navigateur.newContext({ locale: 'fr-FR', timezoneId: 'Africa/Douala', viewport: { width: 1440, height: 900 } })
      await contexte.addInitScript((x) => {
        localStorage.setItem('gm.profilDemo', x.profil)
        localStorage.setItem('gm.theme', x.theme)
        if (!sessionStorage.getItem('gm.sessionDemo')) sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: '' }))
      }, { profil: recit.profil, theme: theme === 'clair' ? 'light' : 'dark' })
      const page = await contexte.newPage()
      page.setDefaultTimeout(60_000)
      for (const [fichier, action] of recit.etapes) {
        await action(page)
        await page.waitForTimeout(400)
        await page.screenshot({ path: join(dossier, `${fichier}-${theme}.png`), fullPage: true })
      }
      await contexte.close()
    }
    console.log(`${nom} : ${recit.etapes.length * 2} captures dans ${dossier}`)
  }
} finally {
  await navigateur.close()
}
