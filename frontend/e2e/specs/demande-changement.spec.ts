import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { joindre } from './outils'

// Fiche RFC du processus d'unité : demandeur, responsable, décision SSI signée et PDF. Clair et sombre.
const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'demande-changement') : resolve(ICI, '..', 'captures', 'demande-changement')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
async function devenir(page: Page, profil: 'ROLE_COLLABORATEUR' | 'ROLE_RSSI' | 'ROLE_PRODUCTION_MONETIQUE', theme: 'light' | 'dark', chemin: string) {
  await demo(page, { profil, theme })
  await page.goto(`${ADRESSES.departement}${chemin}`)
}
async function terminer(page: Page, libelle: string, attendu = 'Terminée') {
  const codes: Record<string, string> = { 'Identification du changement': 'IDENTIFICATION', 'Raison du changement': 'RAISON', 'Impact du changement': 'IMPACT', 'Détail d’implémentation': 'IMPLEMENTATION', 'Décision de la Sécurité': 'SSI' }
  const ouvrir = page.getByRole('button', { name: `Ouvrir ${codes[libelle]}`, exact: true })
  if (await ouvrir.count()) await ouvrir.first().click()
  const panneau = page.getByRole('complementary', { name: new RegExp(libelle) })
  await panneau.getByRole('button', { name: 'Terminer l’étape' }).click()
  await page.getByRole('alertdialog', { name: 'Terminer l’étape' }).getByRole('button', { name: 'Terminer l’étape' }).click()
  await expect(panneau.getByText(attendu, { exact: true }).first()).toBeVisible()
}

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : demande de changement (${suffixe})`, () => {
    test('Scénario : le demandeur renseigne les sections 1 à 4, la SSI signe et le PDF se télécharge', async ({ page }) => {
      test.setTimeout(120_000)
      let instance = ''
      await test.step('Le demandeur lance la fiche et obtient une référence datée automatique', async () => {
        await devenir(page, 'ROLE_COLLABORATEUR', theme, '/des/demandes/lancer')
        await page.getByRole('button', { name: 'Lancer « Demande de changement »' }).click()
        const d = page.getByRole('dialog', { name: 'Nouvelle demande' })
        await d.getByLabel(/^Objet de la demande/).fill('Accès en lecture à la table AMA')
        await d.getByLabel(/^Unité du demandeur/).selectOption('SUPPORT_MONETIQUE')
        await d.getByLabel(/^Élément de configuration/).fill('AMA')
        await d.getByLabel(/^Identifiant EC/).fill('EC-AMA-001')
        await d.getByLabel(/^Priorité/).selectOption('URGENT')
        await d.getByLabel(/^Déclencheur/).selectOption('CLIENT')
        await d.getByLabel(/^Contact du demandeur/).fill('david@gimac.test')
        await d.getByLabel(/^Raison détaillée/).fill('Donner un accès en lecture aux transactions.')
        await d.getByLabel(/^EC impactés/).fill('AMA, BD')
        await d.getByLabel(/^Description de l’impact/).fill('Création d’un compte en lecture seule.')
        await d.getByLabel(/^Contact du responsable de mise en œuvre/).fill('alice@gimac.test')
        await d.getByLabel(/^Description du plan de test/).fill('Vérifier les lectures et refuser les écritures.')
        await d.getByLabel(/^Description du plan de retour/).fill('Supprimer le compte créé.')
        await capturer(page, `formulaire-${suffixe}`)
        await d.getByRole('button', { name: 'Envoyer la demande' }).click()
        await expect(page.getByRole('heading', { level: 1, name: 'Accès en lecture à la table AMA' })).toBeVisible()
        await expect(page.getByText(/CHG-\d{4}-\d{2}-\d{2}-\d{6}/)).toBeVisible()
        instance = new URL(page.url()).pathname.split('/').pop()!
      })
      await test.step('Les étapes 1 à 3 appartiennent au demandeur', async () => {
        for (const etape of ['Identification du changement', 'Raison du changement', 'Impact du changement']) await terminer(page, etape)
        await capturer(page, `etapes-demandeur-${suffixe}`)
      })
      await test.step('Le responsable dépose le dossier de mise en œuvre', async () => {
        await devenir(page, 'ROLE_PRODUCTION_MONETIQUE', theme, `/des/processus/instances/${instance}?etape=IMPLEMENTATION`)
        const panneau = page.getByRole('complementary', { name: /Détail d’implémentation/ })
        await joindre(panneau, 'dossier-changement.pdf')
        await panneau.getByRole('button', { name: 'Envoyer DOSSIER_CHANGEMENT' }).click()
        await expect(page.getByRole('status').filter({ hasText: /dossier-changement.pdf déposé/ })).toBeVisible()
        await terminer(page, 'Détail d’implémentation')
      })
      await test.step('La SSI renseigne planification et évaluation, puis signe à second facteur', async () => {
        await devenir(page, 'ROLE_RSSI', theme, `/des/processus/instances/${instance}?etape=SSI`)
        const panneau = page.getByRole('complementary', { name: /Décision de la Sécurité/ })
        await panneau.getByLabel(/^Date de déploiement/).fill('02/11/2026')
        await panneau.getByLabel(/^Date de revue/).fill('03/11/2026')
        await panneau.getByLabel(/^Évaluation du changement/).fill('Risque limité au compte en lecture.')
        await panneau.getByLabel(/^Décision/).selectOption('ACCEPTEE')
        await panneau.getByLabel(/^Description de la décision/).fill('Favorable après revue du plan de retour.')
        await panneau.getByRole('button', { name: 'Enregistrer les champs' }).click()
        await capturer(page, `zone-ssi-${suffixe}`)
        await terminer(page, 'Décision de la Sécurité', 'À signer')
        await panneau.getByRole('button', { name: 'Signer' }).click()
        const signature = page.getByRole('dialog', { name: 'Signer : Décision SSI' })
        await signature.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
        await expect(signature.getByText('Signature confirmée')).toBeVisible()
        await signature.getByRole('button', { name: 'Continuer' }).click()
        await expect(page.getByText('Close', { exact: true }).first()).toBeVisible()
        await capturer(page, `decision-signee-${suffixe}`)
      })
      await test.step('Le PDF de la demande et de sa signature est téléchargé', async () => {
        const telechargement = page.waitForEvent('download')
        await page.getByRole('button', { name: 'Télécharger le PDF' }).click()
        const pdf = await telechargement
        expect(pdf.suggestedFilename()).toMatch(/CHG-\d{4}-\d{2}-\d{2}-\d{6}\.pdf/)
      })
    })
  })
}
