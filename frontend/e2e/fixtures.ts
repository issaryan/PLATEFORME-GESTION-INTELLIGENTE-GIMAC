import { test as base, expect, type Page } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

// Recueil de la couverture istanbul (window.__coverage__) dans e2e/.nyc_output : à la fin de chaque
// scénario et à chaque déchargement de page (navigation complète : connexion, déconnexion), par une
// liaison Playwright appelée sur « pagehide ». nyc fusionne les fichiers et impose les seuils.
const SORTIE = resolve(dirname(fileURLToPath(import.meta.url)), '.nyc_output')

export const ADRESSES = {
  console: 'http://localhost:5202',
  consoleHttp: 'http://localhost:5212',
  departement: 'http://localhost:5200',
  departementHttp: 'http://localhost:5210',
  portail: 'http://localhost:5201',
  portailHttp: 'http://localhost:5211',
}

function ecrire(couverture: string | null) {
  if (!couverture || couverture === 'null') return
  mkdirSync(SORTIE, { recursive: true })
  writeFileSync(join(SORTIE, `${randomUUID()}.json`), couverture)
}

export async function recueillirCouverture(page: Page) {
  const couverture = await page.evaluate(() => JSON.stringify((window as unknown as { __coverage__?: unknown }).__coverage__ ?? null)).catch(() => null)
  ecrire(couverture)
}

/** Profils de démonstration : console (D-62) et application du département (D-57, D-58). */
export type ProfilDemo = 'ROLE_SUPER_ADMIN' | 'ROLE_RSSI' | 'ROLE_CHEF_DDRM' | 'ROLE_CHARGE_DDRM' | 'ROLE_CHEF_DES' | 'ROLE_INGENIEUR_HOMOLOGATION' | 'ROLE_CONDUCTEUR_PROJET' | 'ROLE_SNA'
  | 'ROLE_PRODUCTION_MONETIQUE' | 'ROLE_POLE_RESEAU' | 'ROLE_POLE_SYSTEME' | 'ROLE_CONFORMITE' | 'ROLE_DG'

/** Démonstration en services simulés : profil (gm.profilDemo) et premier accès (gm.premierAccesDemo). */
export async function demo(page: Page, o: { profil?: ProfilDemo; premierAcces?: boolean; theme?: 'light' | 'dark' } = {}) {
  await page.addInitScript((x) => {
    if (x.profil) localStorage.setItem('gm.profilDemo', x.profil)
    if (x.premierAcces) localStorage.setItem('gm.premierAccesDemo', '1')
    if (x.theme) localStorage.setItem('gm.theme', x.theme)
  }, o)
}

export const test = base.extend({
  page: async ({ page }, use) => {
    await page.exposeFunction('__gmCouverture', (json: string) => ecrire(json))
    await page.addInitScript(() => {
      window.addEventListener('pagehide', () => {
        const w = window as unknown as { __coverage__?: unknown; __gmCouverture?: (j: string) => void }
        if (w.__coverage__) w.__gmCouverture?.(JSON.stringify(w.__coverage__))
      })
    })
    await use(page)
    await recueillirCouverture(page)
  },
})
export { expect }
