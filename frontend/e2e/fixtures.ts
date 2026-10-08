import { test as base, expect, type Page } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { poserCatalogue } from './specs/passerelle-catalogue'

// Recueil de la couverture istanbul (window.__coverage__) dans e2e/.nyc_output : à la fin de chaque
// scénario et à chaque déchargement de page (navigation complète : connexion, déconnexion), par une
// liaison Playwright appelée sur « pagehide ». nyc fusionne les fichiers et impose les seuils.
// GIMAC_E2E_NYC : autre dossier de recueil (exécutions parallèles) ; GIMAC_E2E_PORT : base des ports.
const SORTIE = process.env.GIMAC_E2E_NYC ? resolve(process.env.GIMAC_E2E_NYC) : resolve(dirname(fileURLToPath(import.meta.url)), '.nyc_output')
const BASE = Number(process.env.GIMAC_E2E_PORT ?? 5200)

export const ADRESSES = {
  console: `http://localhost:${BASE + 2}`,
  consoleHttp: `http://localhost:${BASE + 12}`,
  departement: `http://localhost:${BASE}`,
  departementHttp: `http://localhost:${BASE + 10}`,
  portail: `http://localhost:${BASE + 1}`,
  portailHttp: `http://localhost:${BASE + 11}`,
  /** Console telle que l'application du département la lie (VITE_URL_CONSOLE de la construction, construire.mjs). */
  consoleLiee: `http://localhost:${BASE + 2}`,
  /** Application du département ouverte par l'adresse IP (autre hôte que les adresses configurées, D-75). */
  departementIp: `http://127.0.0.1:${BASE}`,
  /** Console liée depuis une page ouverte par l'adresse IP : même hôte, port direct (VITE_PORT_CONSOLE). */
  consoleIp: `http://127.0.0.1:${BASE + 2}`,
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
  | 'ROLE_PRODUCTION_MONETIQUE' | 'ROLE_POLE_RESEAU' | 'ROLE_POLE_SYSTEME' | 'ROLE_CONFORMITE' | 'ROLE_DG' | 'ROLE_COLLABORATEUR' | 'ROLE_RESPONSABLE_SERVICE'
  | 'ROLE_ADMIN_SYSTEME'

/** Démonstration en services simulés : profil (gm.profilDemo) et premier accès (gm.premierAccesDemo). */
export async function demo(page: Page, o: { profil?: ProfilDemo; premierAcces?: boolean; theme?: 'light' | 'dark' } = {}) {
  await page.addInitScript((x) => {
    if (x.profil) localStorage.setItem('gm.profilDemo', x.profil)
    if (x.premierAcces) localStorage.setItem('gm.premierAccesDemo', '1')
    if (x.theme) localStorage.setItem('gm.theme', x.theme)
  }, o)
}

/**
 * `connecte` (vrai par défaut) : session de démonstration ouverte d'avance dans l'onglet (gm.sessionDemo,
 * services simulés, D-65), comme après la connexion simulée ; le profil reste celui de `demo`. Les
 * scénarios de connexion posent `test.use({ connecte: false })`.
 */
export const test = base.extend<{ connecte: boolean }>({
  connecte: [true, { option: true }],
  page: async ({ page, connecte }, use) => {
    if (connecte) {
      await page.addInitScript(() => {
        if (!sessionStorage.getItem('gm.sessionDemo')) sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: '' }))
      })
    }
    // Catalogue des fonctionnalités de MS-IAM (D-94) pour les constructions sur la passerelle.
    await poserCatalogue(page)
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
