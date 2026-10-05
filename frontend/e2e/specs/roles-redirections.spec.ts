import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, expect, recueillirCouverture, test } from '../fixtures'
import { capturePleinePage } from './outils'

// Fonctionnalité : aiguillage des comptes et tableaux de bord par permissions (D-79), services simulés. Pour chaque
// profil interne et le correspondant d'une institution, connexion sur chacune des trois applications : application
// d'arrivée (compte d'institution : portail ; compte interne : application interne, ou console s'il porte une
// permission de console), barre latérale (messagerie et Mon activité pour tout interne), tableau de bord jamais
// vide et composé des widgets que ses permissions ouvrent. Preuves : capture plein écran du tableau de bord
// d'arrivée en thème clair et sombre, <profil>-<application de connexion>-<clair|sombre>.png, dans
// e2e/captures/roles (ou $GIMAC_CAPTURES/roles).
test.use({ connecte: false })

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'roles') : resolve(ICI, '..', 'captures', 'roles')
const DEMO = 'Gimac-Demo-2026!'

const MJ = 'Ma journée'
const ADM = 'Gestion des utilisateurs'
const SUP = 'Supervision de la plateforme'
const INF = 'Charges en erreur'
const DDRM = 'Relations avec les membres (DDRM)'
const CK = 'Pilotage des projets de certification'
const MT = 'Mon travail'
const ACC = 'Mes accès'

type Profil = { role: string; identifiant: string; widgets: string[]; console?: string[] }
// Widgets attendus du tableau de bord de l'application interne (registre des fonctionnalités, D-79), et de celui
// de la console pour les profils qui l'ouvrent.
const PROFILS: Profil[] = [
  { role: 'ROLE_INGENIEUR_HOMOLOGATION', identifiant: 'j.ekotto', widgets: [MJ, MT] },
  { role: 'ROLE_CHEF_DES', identifiant: 'p.mbarga', widgets: [MJ, CK, MT] },
  { role: 'ROLE_SUPER_ADMIN', identifiant: 'super.admin', widgets: [MJ, ADM, SUP, INF], console: [ADM, INF, SUP] },
  { role: 'ROLE_RSSI', identifiant: 'h.kamga', widgets: [MJ, ADM, SUP, INF, MT], console: [ADM, INF, SUP] },
  { role: 'ROLE_CONFORMITE', identifiant: 'm.obame', widgets: [MJ, MT] },
  { role: 'ROLE_DG', identifiant: 'j.ella', widgets: [MJ, CK, MT] },
  { role: 'ROLE_CHARGE_DDRM', identifiant: 'f.abakar', widgets: [MJ, DDRM, MT] },
  { role: 'ROLE_CHEF_DDRM', identifiant: 'b.nguema', widgets: [MJ, DDRM, CK, MT] },
  { role: 'ROLE_SNA', identifiant: 'r.biyong', widgets: [MJ, MT] },
  { role: 'ROLE_PRODUCTION_MONETIQUE', identifiant: 's.ondoua', widgets: [MJ, MT] },
  { role: 'ROLE_POLE_RESEAU', identifiant: 'i.mahamat', widgets: [MJ, MT] },
  { role: 'ROLE_POLE_SYSTEME', identifiant: 'c.ngo', widgets: [MJ, MT] },
  { role: 'ROLE_CONDUCTEUR_PROJET', identifiant: 'a.mahamat', widgets: [MJ, MT] },
  { role: 'ROLE_COLLABORATEUR', identifiant: 'n.mvondo', widgets: [MJ, ACC] },
  { role: 'ROLE_RESPONSABLE_SERVICE', identifiant: 'g.ndzana', widgets: [MJ, ACC] },
  { role: 'ROLE_ADMIN_SYSTEME', identifiant: 'admin.systeme', widgets: [MJ, SUP, INF], console: [INF, SUP] },
]
const TOUS = [MJ, ADM, SUP, INF, DDRM, CK, MT, ACC]
const APPLICATIONS = ['departement', 'portail', 'console'] as const
type Application = (typeof APPLICATIONS)[number]
const nomFichier = (role: string) => role.replace(/^ROLE_/, '').toLowerCase().replace(/_/g, '-')

/** Identifiant et mot de passe, puis code ; `refuse` : la couche refuse le compte, aucun code n'est demandé. */
async function seConnecter(page: Page, identifiant: string, refuse = false) {
  await page.locator('input[name="identifiant"]').fill(identifiant)
  await page.locator('input[name="motDePasse"]').fill(DEMO)
  await page.getByRole('button', { name: 'Se connecter' }).click()
  if (refuse) return
  await page.locator('input[name="code"]').fill('000000')
  await page.getByRole('button', { name: 'Vérifier le code' }).click()
}

/** Capture plein écran en clair puis en sombre (préférence de thème, page rechargée : la session de l'onglet demeure). */
async function capturer(page: Page, nom: string, attendre: () => Promise<void>) {
  mkdirSync(CAPTURES, { recursive: true })
  for (const theme of ['light', 'dark'] as const) {
    if (theme === 'dark') {
      await recueillirCouverture(page)
      await page.evaluate(() => localStorage.setItem('gm.theme', 'dark'))
      await page.reload()
    }
    await attendre()
    await expect(page.locator('html, body').first()).toBeVisible()
    // Pointeur écarté : aucun survol ne colore une entrée de la barre latérale sur la capture.
    await page.mouse.move(0, 0)
    await page.waitForTimeout(600)
    await capturePleinePage(page, join(CAPTURES, `${nom}-${theme === 'light' ? 'clair' : 'sombre'}.png`))
  }
}

const nav = (page: Page) => page.getByRole('navigation', { name: 'Navigation principale' })

/** Tableau de bord de l'application interne : barre latérale et widgets attendus, rien d'autre. */
async function verifierInterne(page: Page, p: Profil) {
  await expect(page).toHaveURL(`${ADRESSES.departement}/des/tableau-de-bord`)
  await expect(page.getByRole('heading', { level: 1, name: 'Tableau de bord' })).toBeVisible()
  await expect(nav(page).getByRole('link', { name: /^Messagerie/ })).toBeVisible()
  await expect(nav(page).getByRole('link', { name: 'Ma journée', exact: true })).toBeVisible()
  for (const w of p.widgets) await expect(page.getByRole('region', { name: w, exact: true }).first(), `${p.role} : ${w}`).toBeVisible()
  for (const w of TOUS.filter((x) => !p.widgets.includes(x))) await expect(page.getByRole('region', { name: w, exact: true }), `${p.role} sans ${w}`).toHaveCount(0)
  // Jamais vide.
  await expect(page.getByText('Aucun tableau de bord pour votre profil')).toHaveCount(0)
}

async function verifierConsole(page: Page, p: Profil) {
  await expect(page).toHaveURL(`${ADRESSES.console}/console/tableau-de-bord`)
  await expect(nav(page).getByRole('link', { name: /^Messagerie/ })).toBeVisible()
  await expect(nav(page).getByRole('link', { name: 'Mon activité', exact: true })).toHaveAttribute('href', `${ADRESSES.departement}/des/activite`)
  for (const w of p.console!) await expect(page.getByRole('region', { name: w, exact: true }).first(), `${p.role} console : ${w}`).toBeVisible()
  await expect(page.getByText('Aucun tableau de bord pour votre profil')).toHaveCount(0)
}

const depart: Record<Application, string> = { departement: ADRESSES.departement, portail: ADRESSES.portail, console: ADRESSES.console }

test.describe('Fonctionnalité : chaque profil arrive dans son application, sur un tableau de bord composé par ses permissions', () => {
  for (const p of PROFILS) {
    for (const application of APPLICATIONS) {
      test(`Scénario : ${p.role} se connecte sur ${application}`, async ({ page }) => {
        await page.addInitScript(() => {
          if (!localStorage.getItem('gm.theme')) localStorage.setItem('gm.theme', 'light')
        })
        await page.goto(`${depart[application]}/connexion`)
        await seConnecter(page, p.identifiant, application === 'portail')
        if (application === 'portail') {
          // Compte interne refusé par le portail : conduit vers la connexion de l'application interne, identifiant prérempli.
          await expect(page).toHaveURL(new RegExp(`^${ADRESSES.departement}/connexion\\?motif=aiguillage&depuis=portail`))
          await expect(page.getByText(/Le portail des participants vous a conduit ici/)).toBeVisible()
          await expect(page.locator('input[name="identifiant"]')).toHaveValue(p.identifiant)
          await seConnecter(page, p.identifiant)
        }
        const surConsole = application === 'console' && !!p.console
        if (application === 'console' && !p.console) {
          // Compte interne sans permission de console : message bref, puis application interne.
          await expect(page.getByText(/n'ouvre pas la console d'administration/)).toBeVisible()
        }
        const verifier = () => (surConsole ? verifierConsole(page, p) : verifierInterne(page, p))
        await verifier()
        await capturer(page, `${nomFichier(p.role)}-${application}`, verifier)
      })
    }
  }

  for (const application of APPLICATIONS) {
    test(`Scénario : le correspondant d'une institution se connecte sur ${application}`, async ({ page }) => {
      await page.addInitScript(() => {
        if (!localStorage.getItem('gm.theme')) localStorage.setItem('gm.theme', 'light')
      })
      await page.goto(`${depart[application]}/connexion`)
      await seConnecter(page, 'correspondant-a', application !== 'portail')
      if (application !== 'portail') {
        // Compte d'institution refusé : conduit vers la connexion du portail.
        await expect(page).toHaveURL(new RegExp(`^${ADRESSES.portail}/connexion\\?motif=aiguillage&depuis=${application === 'departement' ? 'des' : 'console'}`))
        await seConnecter(page, 'correspondant-a')
      }
      const verifier = async () => {
        await expect(page).toHaveURL(`${ADRESSES.portail}/portail/tableau-de-bord`)
        await expect(page.getByRole('heading', { level: 1, name: 'Mon espace de certification' })).toBeVisible()
        await expect(nav(page).getByRole('link', { name: /^Messagerie/ })).toHaveCount(0)
      }
      await verifier()
      await capturer(page, `correspondant-${application}`, verifier)
    })
  }
})

test.describe('Fonctionnalité : ouverture d’une application avec une session existante', () => {
  test('Scénario : un collaborateur ouvre la console avec sa session : message bref, puis application interne', async ({ page }) => {
    await page.addInitScript(() => sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: 'n.mvondo', role: 'ROLE_COLLABORATEUR' })))
    await page.goto(`${ADRESSES.console}/console/comptes`)
    await expect(page.getByText(/n'ouvre pas la console d'administration/)).toBeVisible()
    await page.getByRole('button', { name: "Ouvrir l'application interne" }).click()
    await expect(page).toHaveURL(`${ADRESSES.departement}/des/tableau-de-bord`)
    await expect(page.getByRole('region', { name: 'Ma journée' })).toBeVisible()
  })
})
