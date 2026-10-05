import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { capturePleinePage, naviguer, signer } from './outils'
import { Passerelle, PERMISSIONS_SUPER_ADMIN } from './passerelle'

// Fonctionnalité : exploitation de l'infrastructure (D-78), dans l'application interne et dans la console (D-79).
// Services simulés : l'administrateur système lit les charges, ouvre les journaux, redémarre et met à l'échelle avec
// sa clé de sécurité ; le RSSI lit sans geste. Passerelle simulée : exploitation non configurée (X-Orchestrateur:
// AUCUN), refus lisible d'un geste, opération poussée par le flux. Captures en clair et sombre.

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'infrastructure') : resolve(ICI, '..', 'captures', 'infrastructure')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.mouse.move(0, 0)
  await page.waitForTimeout(500)
  await capturePleinePage(page, join(CAPTURES, `${nom}.png`))
}
const suffixe = (theme: 'light' | 'dark') => (theme === 'light' ? 'clair' : 'sombre')
const ligne = (page: Page, nom: string) => page.getByRole('row').filter({ has: page.getByText(nom, { exact: true }) }).first()

for (const theme of ['light', 'dark'] as const) {
  test(`Scénario : l’administrateur système exploite l’infrastructure depuis l’application interne (thème ${theme})`, async ({ page }) => {
    await demo(page, { profil: 'ROLE_ADMIN_SYSTEME', theme })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await naviguer(page, 'Infrastructure')
    await expect(page).toHaveURL(/\/des\/plateforme\/infrastructure$/)
    await expect(page.getByRole('heading', { level: 1, name: 'Infrastructure' })).toBeVisible()
    await expect(ligne(page, 'pilotage').getByText('En erreur')).toBeVisible()
    await expect(ligne(page, 'gateway').getByRole('button', { name: 'Arrêter gateway' })).toHaveCount(0)
    await capturer(page, `infrastructure-admin-systeme-departement-${suffixe(theme)}`)
    if (theme === 'dark') return
    await test.step('Journaux de la charge, actualisés sur cinq lignes', async () => {
      await ligne(page, 'iam').getByRole('button', { name: 'Journaux de iam' }).click()
      const liste = page.getByRole('list', { name: 'Journaux de iam' })
      await expect(liste.getByRole('listitem')).toHaveCount(24)
      await page.getByLabel(/^Lignes/).fill('5')
      await page.getByLabel(/^Depuis/).selectOption('PT30M')
      await page.getByRole('button', { name: 'Actualiser' }).click()
      await expect(liste.getByRole('listitem')).toHaveCount(5)
      await page.getByRole('button', { name: 'Fermer les journaux' }).click()
      await expect(liste).toHaveCount(0)
    })
    await test.step('Redémarrage motivé et confirmé par la clé de sécurité, puis issue dans l’historique', async () => {
      await ligne(page, 'iam').getByRole('button', { name: 'Redémarrer iam' }).click()
      const d = await signer(page, 'Prise en compte de la nouvelle configuration')
      await expect(d.getByText('Signature confirmée')).toBeVisible()
      await d.getByRole('button', { name: 'Continuer' }).click()
      await expect(page.getByText(/Opération acceptée sur iam/)).toBeVisible()
      const historique = page.locator('section.gm-card').filter({ has: page.getByRole('heading', { name: 'Historique des opérations' }) })
      await expect(historique.getByText('Prise en compte de la nouvelle configuration', { exact: true })).toBeVisible()
      await expect(historique.getByRole('row').filter({ has: page.getByText('Prise en compte de la nouvelle configuration', { exact: true }) }).getByText('Réussie')).toBeVisible({ timeout: 10_000 })
    })
    await test.step('Mise à l’échelle : répliques hors bornes refusées, puis trois répliques', async () => {
      await ligne(page, 'certification').getByRole('button', { name: 'Mettre certification à l’échelle' }).click()
      const d = page.getByRole('dialog')
      await d.getByLabel(/Répliques voulues/).fill('9')
      await signer(page, 'Montée en charge de la campagne de certification')
      await expect(d.getByText('Le nombre de répliques doit être compris entre 0 et 5.')).toBeVisible()
      await d.getByLabel(/Répliques voulues/).fill('3')
      await d.getByRole('button', { name: /Réessayer|Utiliser ma clé de sécurité/ }).first().click()
      await expect(d.getByText('Signature confirmée')).toBeVisible()
      await d.getByRole('button', { name: 'Continuer' }).click()
      await expect(ligne(page, 'certification').getByText('3 / 3')).toBeVisible({ timeout: 10_000 })
    })
  })

  test(`Scénario : le RSSI consulte l’infrastructure en lecture seule (thème ${theme})`, async ({ page }) => {
    await demo(page, { profil: 'ROLE_RSSI', theme })
    await page.goto(`${ADRESSES.departement}/des/plateforme/infrastructure`)
    await expect(page.getByText(/Lecture seule : votre profil consulte/)).toBeVisible()
    await expect(ligne(page, 'iam').getByRole('button', { name: 'Journaux de iam' })).toBeVisible()
    await expect(ligne(page, 'iam').getByRole('button', { name: 'Redémarrer iam' })).toHaveCount(0)
    await capturer(page, `infrastructure-rssi-departement-${suffixe(theme)}`)
  })
}

test('Scénario : la console sert l’infrastructure et les écrans de plateforme sont dans l’application interne', async ({ page }) => {
  await demo(page, { profil: 'ROLE_ADMIN_SYSTEME' })
  await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)
  await expect(page.getByRole('heading', { level: 1, name: 'Tableau de bord de l’exploitation' })).toBeVisible()
  await page.getByRole('region', { name: 'Charges en erreur' }).getByRole('link', { name: 'pilotage' }).click()
  await expect(page).toHaveURL(/\/console\/plateforme\/infrastructure\?charge=pilotage$/)
  await expect(page.getByRole('list', { name: 'Journaux de pilotage' })).toBeVisible()
  await capturer(page, 'infrastructure-admin-systeme-console-clair')
  // Application interne : supervision, configuration et journalisation servies sous /des/plateforme.
  await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
  await page.getByRole('region', { name: 'Supervision de la plateforme' }).getByRole('link', { name: 'Ouvrir la supervision' }).click()
  await expect(page.getByRole('region', { name: 'État en temps réel' })).toBeVisible()
  await naviguer(page, 'Configuration')
  await expect(page).toHaveURL(/\/des\/plateforme\/configuration$/)
  await naviguer(page, 'Journalisation')
  await expect(page).toHaveURL(/\/des\/plateforme\/journalisation$/)
  await page.goto(`${ADRESSES.departement}/des/plateforme/courriels`)
  await expect(page.getByText('Accès refusé')).toBeVisible()
})

test.describe('Fonctionnalité : infrastructure sur la passerelle', () => {
  const PERMISSIONS = [...PERMISSIONS_SUPER_ADMIN, 'PLATFORM_OPS_READ', 'PLATFORM_OPS_EXECUTE']

  test('Scénario : exploitation non configurée (X-Orchestrateur: AUCUN)', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS }, orchestrateur: 'AUCUN' })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/plateforme/infrastructure`)
    await expect(page.getByText('Exploitation non configurée')).toBeVisible()
    await page.goto(`${ADRESSES.consoleHttp}/console/tableau-de-bord`)
    await expect(page.getByRole('region', { name: 'Charges en erreur' }).getByText('Exploitation non configurée')).toBeVisible()
  })

  test('Scénario : geste refusé par la passerelle, message lisible ; journaux et opération poussée', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS }, orchestrateur: 'DOCKER', refusGeste: 'OPS_OPERATION_EN_COURS' })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/plateforme/infrastructure`)
    await expect(page.getByText('Charges de travail (Docker)')).toBeVisible()
    await ligne(page, 'iam').getByRole('button', { name: 'Journaux de iam' }).click()
    await expect(page.getByRole('list', { name: 'Journaux de iam' }).getByText('Jeton Bearer **** renouvelé')).toBeVisible()
    await ligne(page, 'iam').getByRole('button', { name: 'Arrêter iam' }).click()
    const d = await signer(page, 'Maintenance programmée de la nuit')
    await expect(d.getByText(/Une opération est déjà en cours sur cette charge/)).toBeVisible()
    const geste = gw.appels.find((a) => a.chemin === '/api/plateforme/infrastructure/charges/iam/arret')!
    expect(geste.corps).toEqual({ motif: 'Maintenance programmée de la nuit' })
    expect(geste.entetes['x-reauth-assertion']).toBeTruthy()
    await expect.poll(() => gw.appels.filter((a) => a.chemin === '/api/plateforme/infrastructure/charges').length, { timeout: 15_000 }).toBeGreaterThan(1)
  })
})
