import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { joindre, naviguer, ongletSelectionne } from './outils'

// Fonctionnalité : processus d'unité (D-87, docs/lot1/processus-unite.md), services simulés de l'application du
// département. Le chef du DES modélise un processus (modale d'étape à onglets, contrôle, publication avec levée
// motivée des avertissements) ; un membre du pôle réseau lance « Ouverture d'un flux réseau », prend l'étape d'analyse,
// dépose sa note et la termine ; le chef du service signe le procès-verbal de recette avec sa clé de sécurité.
// Captures en thème clair et sombre dans e2e/captures/d86-d90 (ou $GIMAC_CAPTURES/d86-d90).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd86-d90') : resolve(ICI, '..', 'captures', 'd86-d90')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const toast = (page: Page, texte: string | RegExp) => page.getByRole('status').filter({ hasText: texte })

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : processus d’unité, services simulés (${suffixe})`, () => {
    test('Scénario : le chef du DES modélise un processus, le contrôle et le publie en levant les avertissements', async ({ page }) => {
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/processus`)

      await test.step('Étant donné le catalogue des processus, groupé par unité', async () => {
        await expect(page.getByRole('heading', { level: 2, name: /^Pôle réseau/ })).toBeVisible()
        await expect(page.getByRole('button', { name: 'Ouverture d’un flux réseau', exact: true }).first()).toBeVisible()
        await capturer(page, `processus-catalogue-${suffixe}`)
        await page.getByRole('button', { name: 'Accès VPN d’un participant' }).first().click()
        await expect(page.getByRole('heading', { level: 1, name: 'Accès VPN d’un participant' })).toBeVisible()
      })

      await test.step('Quand il ouvre l’étape de configuration : un onglet par aspect de l’étape', async () => {
        await expect(page.getByTestId('processus-canevas')).toBeVisible()
        await page.getByRole('button', { name: 'Modifier CONFIGURATION' }).click()
        const d = page.getByRole('dialog', { name: 'Étape CONFIGURATION' })
        await capturer(page, `processus-etape-general-${suffixe}`)
        await ongletSelectionne(d, 'Intervenant et validateur')
        await d.getByLabel(/^Profil de remplacement/).first().selectOption('ROLE_POLE_RESEAU')
        await expect(d.getByLabel(/^Personne/)).toBeEnabled()
        await d.getByLabel(/^Personne/).selectOption('u-res')
        await capturer(page, `processus-etape-intervenant-${suffixe}`)
        await ongletSelectionne(d, 'Livrables')
        await capturer(page, `processus-etape-livrables-${suffixe}`)
        await ongletSelectionne(d, 'Signatures')
        await d.getByRole('button', { name: 'Ajouter une signature' }).click()
        await d.getByLabel(/^Libellé/).first().fill('Visa du chef de pôle')
        await d.getByLabel('FICHE_VPN · Fiche de paramètres').check()
        await capturer(page, `processus-etape-signatures-${suffixe}`)
        await ongletSelectionne(d, 'Champs')
        await d.getByRole('button', { name: 'Ajouter un champ' }).click()
        await d.getByLabel(/^Libellé/).first().fill('Adresse du concentrateur')
        await capturer(page, `processus-etape-champs-${suffixe}`)
        await ongletSelectionne(d, 'Chronologie')
        await capturer(page, `processus-etape-chronologie-${suffixe}`)
        await d.getByRole('button', { name: 'Enregistrer' }).click()
        await expect(d).toHaveCount(0)
      })

      await test.step('Alors le contrôle signale l’avertissement levable et la publication demande un motif', async () => {
        await page.getByRole('button', { name: 'Contrôler' }).click()
        await expect(page.getByText('Profil non couvert dans l’unité').or(page.getByText('Unité sans chef')).first()).toBeVisible()
        await page.getByRole('button', { name: 'Publier' }).click()
        await page.getByRole('alertdialog', { name: 'Publier le processus' }).getByRole('button', { name: 'Publier' }).click()
        const lever = page.getByRole('alertdialog', { name: 'Lever les avertissements' })
        await lever.getByLabel(/Motif/).fill('Validation par le chef du service en attendant la désignation')
        await lever.getByRole('button', { name: 'Lever et publier' }).click()
        await expect(page.getByText('Version publiée ou retirée : lecture seule ; créez une nouvelle version pour la modifier.')).toBeVisible()
      })
    })

    test('Scénario : un membre du pôle réseau lance un processus, prend l’étape d’analyse et la termine', async ({ page }) => {
      await demo(page, { profil: 'ROLE_POLE_RESEAU', theme })
      await page.goto(`${ADRESSES.departement}/des/processus`)

      await test.step('Étant donné la modale de lancement : intitulé, champs de départ, désignation au lancement', async () => {
        await page.getByRole('button', { name: 'Lancer le processus Ouverture d’un flux réseau' }).click()
        const d = page.getByRole('dialog', { name: 'Lancer le processus' })
        await d.getByLabel(/^Intitulé de l’instance/).fill('Flux SNMP vers la supervision')
        await d.getByLabel(/^Adresse source/).fill('10.20.4.21')
        await d.getByLabel(/^Adresse de destination/).fill('172.16.8.12')
        await d.getByLabel(/^Port/).fill('161')
        await d.getByLabel(/^Protocole/).selectOption('UDP')
        await d.getByLabel(/^Intervenant de « Mise en œuvre/).selectOption('u-collab')
        await capturer(page, `processus-lancement-${suffixe}`)
        await d.getByRole('button', { name: 'Lancer l’instance' }).click()
        await expect(page.getByRole('heading', { level: 1, name: 'Flux SNMP vers la supervision' })).toBeVisible()
      })

      await test.step('Quand il prend l’étape d’analyse d’une autre instance, dépose sa note et termine l’étape', async () => {
        await page.goto(`${ADRESSES.departement}/des/processus?vue=instances`)
        await page.getByRole('combobox', { name: 'Instances' }).selectOption('unite')
        await page.getByRole('button', { name: 'Ouverture du port 443 pour la supervision' }).click()
        await page.getByRole('button', { name: 'Liste' }).click()
        await page.getByRole('button', { name: 'Ouvrir l’étape ANALYSE' }).click()
        await page.getByRole('button', { name: 'Prendre l’étape' }).click()
        await expect(page.getByRole('button', { name: 'Terminer l’étape' })).toBeVisible()
        const panneau = page.getByRole('complementary', { name: /Analyse de sécurité du flux/ })
        await joindre(panneau, 'note-analyse-port-443.pdf')
        await panneau.getByRole('button', { name: 'Envoyer NOTE_ANALYSE' }).click()
        await expect(toast(page, 'Livrable note-analyse-port-443.pdf déposé.')).toBeVisible()
        await page.getByRole('button', { name: 'Terminer l’étape' }).click()
        await page.getByRole('alertdialog', { name: 'Terminer l’étape' }).getByRole('button', { name: 'Terminer l’étape' }).click()
        await expect(panneau.getByText('À valider', { exact: true }).first()).toBeVisible()
      })

      await test.step('Alors « Ma journée » ne présente plus l’étape parmi « Mes étapes »', async () => {
        // Navigation dans l'application : l'état simulé de la session est conservé.
        await naviguer(page, 'Ma journée')
        await expect(page.getByRole('heading', { name: 'Mes étapes de processus' })).toBeVisible()
        await expect(page.getByText('Ouverture du port 443 pour la supervision : Analyse de sécurité du flux')).toHaveCount(0)
      })
    })

    test('Scénario : le chef du service signe le procès-verbal de recette avec sa clé de sécurité', async ({ page }) => {
      await demo(page, { profil: 'ROLE_RESPONSABLE_SERVICE', theme })
      await page.goto(`${ADRESSES.departement}/des/activite`)
      await expect(page.getByText('Flux SFTP vers la Banque de Libreville : Recette du flux')).toBeVisible()
      await page.goto(`${ADRESSES.departement}/des/processus?vue=instances`)
      await page.getByRole('combobox', { name: 'Instances' }).selectOption('unite')
      await page.getByRole('button', { name: 'Flux SFTP vers la Banque de Libreville' }).click()
      const panneau = page.getByRole('complementary', { name: /Recette du flux/ })
      await expect(panneau.getByText('Attendue de : Gaston Ndzana Ebode')).toBeVisible()
      await panneau.getByRole('button', { name: 'Signer' }).scrollIntoViewIfNeeded()
      await capturer(page, `processus-suivi-etape-a-signer-${suffixe}`)
      await panneau.getByRole('button', { name: 'Signer' }).click()
      const d = page.getByRole('dialog', { name: 'Signer : Signature du procès-verbal' })
      await d.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
      await expect(d.getByText('Signature confirmée')).toBeVisible()
      await d.getByRole('button', { name: 'Continuer' }).click()
      await expect(page.getByText('Close', { exact: true }).first()).toBeVisible()
    })
  })
}
