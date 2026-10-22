// Captures du lot IAM de l'épopée EXI (D-119) : console servie par Vite sur les services simulés
// (VITE_SERVICES=simule npx vite --port 5431 dans frontend/apps/console), Chromium sans tête, thèmes clair et sombre.
//   node frontend/e2e/captures/exi-iam/capturer.mjs <dossier de sortie> [récit…]
// Chaque récit est une suite d'étapes numérotées clic par clic ; aucune donnée réelle.
import { chromium } from '@playwright/test'
import { mkdirSync } from 'node:fs'
import { join } from 'node:path'

const BASE = process.env.GIMAC_CAPTURES_URL ?? 'http://localhost:5431'
const sortie = process.argv[2] ?? 'captures-exi-iam'
const demandes = process.argv.slice(3)

/** Récits : profil de démonstration et étapes (nom de fichier, action avant la capture). */
export const RECITS = {
  'exi-06': {
    profil: 'ROLE_SUPER_ADMIN',
    etapes: [
      ['01-perimetres-suspendus', async (p) => { await p.goto(`${BASE}/console/sandbox`); await p.getByRole('cell', { name: '99001' }).waitFor() }],
      ['02-comptes-proteges', async (p) => { await p.getByRole('tab', { name: 'Comptes protégés' }).click(); await p.getByText('Awa Ndiaye').waitFor() }],
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
