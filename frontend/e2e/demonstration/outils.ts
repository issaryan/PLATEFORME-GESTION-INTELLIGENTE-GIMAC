import { expect, type Page } from '@playwright/test'
import { createHmac } from 'node:crypto'
import { existsSync, mkdirSync, readFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

// Outils des preuves de recette sur services réels, repris du parcours DEMO-SBX du dépôt de la Sandbox GIMAC (04/11/2026) :
// adresses du déploiement local, connexion par l'écran de la plateforme, capture de chaque clic en thème clair puis
// sombre sans recharger la page (l'état de l'écran, dialogue ouvert compris, demeure).

const ici = dirname(fileURLToPath(import.meta.url))
const RACINE = resolve(ici, '..', '..', '..')

/**
 * Secrets de la recette locale (fichier .env de la recette du dépôt de la Sandbox GIMAC, jamais versionné, écrit par
 * comptes-demo.py ; GIMAC_RECETTE_ENV le désigne) : mots de passe et secrets TOTP des comptes de démonstration. Une
 * variable déjà présente dans l'environnement l'emporte.
 */
export function chargerEnvRecette(fichier = process.env.GIMAC_RECETTE_ENV ?? join(RACINE, 'infra', 'recette', '.env')) {
  if (!existsSync(fichier)) return
  for (const ligne of readFileSync(fichier, 'utf8').split('\n')) {
    const i = ligne.indexOf('=')
    if (i <= 0 || ligne.startsWith('#')) continue
    const cle = ligne.slice(0, i).trim()
    if (/^GIMAC_DEMO_/.test(cle) && process.env[cle] === undefined) process.env[cle] = ligne.slice(i + 1)
  }
}
chargerEnvRecette()

/** Suffixe des variables d'un compte : « demo.admin.gimac » donne « DEMO_ADMIN_GIMAC » (comme comptes-demo.py). */
const suffixe = (identifiant: string) => identifiant.replace(/[^A-Za-z0-9]/g, '_').toUpperCase()

export const ADRESSES = {
  departement: process.env.GIMAC_URL_DES ?? 'http://localhost:4200',
  portail: process.env.GIMAC_URL_PORTAIL_SBX ?? 'http://localhost:4301',
  console: process.env.GIMAC_URL_CONSOLE_SBX ?? 'http://localhost:4302',
}

/** Mot de passe d'un compte de démonstration, lu dans l'environnement (jamais écrit dans le dépôt). */
export function motDePasse(identifiant: string): string {
  const cle = `GIMAC_DEMO_MDP_${suffixe(identifiant)}`
  const valeur = process.env[cle] ?? process.env.GIMAC_DEMO_MDP
  if (!valeur) throw new Error(`mot de passe de démonstration absent : ${cle} ou GIMAC_DEMO_MDP`)
  return valeur
}

/** Décodage base 32 (RFC 4648, sans remplissage), alphabet du secret TOTP enregistré par gimac-totp. */
export function base32(texte: string): Buffer {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
  let bits = 0
  let valeur = 0
  const octets: number[] = []
  for (const c of texte.replace(/=+$/, '').toUpperCase()) {
    const i = alphabet.indexOf(c)
    if (i < 0) throw new Error('secret TOTP invalide (base 32 attendue)')
    valeur = (valeur << 5) | i
    bits += 5
    if (bits >= 8) {
      octets.push((valeur >>> (bits - 8)) & 0xff)
      bits -= 8
    }
  }
  return Buffer.from(octets)
}

/** Code TOTP (RFC 6238 : HMAC-SHA1, 6 chiffres, pas de 30 s, politique OTP du domaine gimac) au pas donné. */
export function codeTotp(secret: string, pas = Math.floor(Date.now() / 30_000)): string {
  const compteur = Buffer.alloc(8)
  compteur.writeBigUInt64BE(BigInt(pas))
  const empreinte = createHmac('sha1', base32(secret)).update(compteur).digest()
  const decalage = empreinte[empreinte.length - 1] & 0x0f
  return String((empreinte.readUInt32BE(decalage) & 0x7fffffff) % 1_000_000).padStart(6, '0')
}

// Keycloak refuse un code déjà employé (otpPolicyCodeReusable faux) : un même compte ne se reconnecte qu'au pas suivant.
const derniersPas = new Map<string, number>()

/**
 * Code du second facteur d'un compte : calculé depuis GIMAC_DEMO_TOTP_<COMPTE> (recette locale), en attendant au
 * besoin un pas de 30 s pas encore employé par ce compte ; à défaut, GIMAC_DEMO_CODE.
 */
export async function codeSecondFacteur(identifiant: string): Promise<string> {
  const secret = process.env[`GIMAC_DEMO_TOTP_${suffixe(identifiant)}`]
  if (!secret) return process.env.GIMAC_DEMO_CODE ?? '000000'
  let pas = Math.floor(Date.now() / 30_000)
  while (pas <= (derniersPas.get(identifiant) ?? -1)) {
    await new Promise((r) => setTimeout(r, 30_000 - (Date.now() % 30_000) + 500))
    pas = Math.floor(Date.now() / 30_000)
  }
  derniersPas.set(identifiant, pas)
  return codeTotp(secret, pas)
}

/**
 * Connexion par l'écran de la plateforme (D-65) : identifiant et mot de passe, puis code TOTP calculé. Un code refusé
 * (déjà employé par une autre connexion du même compte dans ce pas) est ressaisi une fois, au pas suivant.
 */
export async function seConnecter(page: Page, adresse: string, identifiant: string) {
  await page.goto(adresse)
  await page.locator('input[name="identifiant"]').fill(identifiant)
  await page.locator('input[name="motDePasse"]').fill(motDePasse(identifiant))
  await page.getByRole('button', { name: 'Se connecter' }).click()
  const code = page.locator('input[name="code"]')
  // Refus de la passerelle (compte inactif, verrou de force brute de Keycloak) : l'alerte est signalée tout de suite,
  // sinon le parcours attendrait en silence un écran qui ne viendra jamais.
  const alerte = page.locator('[role="alert"]').first()
  const vu = await Promise.race([
    code.waitFor({ state: 'visible', timeout: 15_000 }).then(() => 'code', () => 'rien'),
    alerte.waitFor({ state: 'visible', timeout: 15_000 }).then(() => 'alerte', () => 'rien'),
  ])
  if (vu === 'alerte') {
    throw new Error(`connexion refusée pour ${identifiant} : ${(await alerte.innerText()).replace(/\s+/g, ' ').trim()}`)
  }
  for (let essai = 0; essai < 2; essai++) {
    // isVisible n'attend pas : le champ du code apparaît après la réponse CODE_REQUIS de la passerelle.
    if (!(await code.waitFor({ state: 'visible', timeout: 10_000 }).then(() => true, () => false))) return
    await code.fill(await codeSecondFacteur(identifiant))
    await page.getByRole('button', { name: 'Vérifier le code' }).click()
    // Code admis : l'application quitte l'écran de connexion ; refusé : le champ reste affiché, vidé.
    if (await expect(code).toBeHidden({ timeout: 10_000 }).then(() => true, () => false)) return
  }
  throw new Error(`connexion refusée pour ${identifiant}`)
}

/**
 * Bascule de thème sans recharger : l'application suit les changements de gm.theme faits dans un autre onglet
 * (événement storage, socle) ; l'état de l'écran, dialogue ouvert compris, est conservé et le logo suit le thème.
 */
export async function appliquerTheme(page: Page, theme: 'light' | 'dark') {
  await page.evaluate((t) => {
    localStorage.setItem('gm.theme', t)
    window.dispatchEvent(new StorageEvent('storage', { key: 'gm.theme', newValue: t }))
  }, theme)
  await expect(page.locator('html')).toHaveAttribute('data-theme', theme)
  await page.waitForTimeout(300)
}

/** Journal du parcours : une capture numérotée par clic, dans docs/recette/demonstration/<etape>/. */
export class Journal {
  private rang: number
  private readonly dossier: string

  /** `depart` : rang de la dernière capture déjà faite pour l'étape (un second test de l'étape la continue). */
  constructor(etape: string, depart = 0) {
    this.rang = depart
    this.dossier = join(RACINE, 'docs', 'recette', 'demonstration', etape)
    mkdirSync(this.dossier, { recursive: true })
  }

  async capturer(page: Page, nom: string) {
    this.rang++
    const prefixe = `${String(this.rang).padStart(2, '0')}-${nom}`
    await page.mouse.move(0, 0)
    for (const theme of ['light', 'dark'] as const) {
      await appliquerTheme(page, theme)
      await page.screenshot({ path: join(this.dossier, `${prefixe}-${theme === 'light' ? 'clair' : 'sombre'}.png`), fullPage: true })
    }
    await appliquerTheme(page, 'light')
  }

  /** Clique un bouton par son nom accessible, attend l'effet, puis capture. */
  async cliquer(page: Page, nom: string | RegExp, capture: string, effet?: () => Promise<void>) {
    await page.getByRole('button', { name: nom }).first().click()
    if (effet) await effet()
    await this.capturer(page, capture)
  }
}

/**
 * Appel de la passerelle depuis la page connectée, par le fetch du navigateur (même témoin de session, Secure sur
 * http://localhost, que l'écran) ; jeton anti-CSRF du témoin XSRF-TOKEN repris en en-tête ; rend statut et corps JSON.
 */
export async function appelerPasserelle(page: Page, methode: 'GET' | 'POST', chemin: string, corps?: unknown, entetes: Record<string, string> = {}) {
  return page.evaluate(async ({ methode, chemin, corps, entetes }) => {
    const xsrf = decodeURIComponent(document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='))?.slice(11) ?? '')
    const r = await fetch(chemin, {
      method: methode, credentials: 'include', body: corps === undefined ? undefined : JSON.stringify(corps),
      headers: { 'X-XSRF-TOKEN': xsrf, 'Accept-Language': 'fr', ...(corps === undefined ? {} : { 'Content-Type': 'application/json' }), ...entetes },
    })
    const texte = await r.text()
    return { statut: r.status, corps: texte ? JSON.parse(texte) : null }
  }, { methode, chemin, corps, entetes })
}

export { expect }
