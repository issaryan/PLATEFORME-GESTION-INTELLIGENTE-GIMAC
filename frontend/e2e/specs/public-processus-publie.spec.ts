import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test, type ProfilDemo } from '../fixtures'

// Fonctionnalité : le public d'un processus publié se règle sans nouvelle version (D-103,
// docs/lot1/lancement-processus.md §11), services simulés de l'application du département. L'ingénieur d'homologation,
// administrateur des référentiels métier (REF_METIER_ADMIN), ouvre la « Création d'une machine virtuelle » publiée du pôle
// système, réservée à deux personnes désignées : la fenêtre « Qui peut voir et lancer ce processus » laisse le public
// modifiable (formulaire et séparation des fonctions en lecture), avec un motif obligatoire. Il ajoute la responsable de la
// conformité ; elle voit aussitôt le processus dans « Lancer une demande ». Captures en thème clair et sombre dans
// e2e/captures/d103 (ou $GIMAC_CAPTURES/d103).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd103') : resolve(ICI, '..', 'captures', 'd103')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const VM = 'Création d’une machine virtuelle'
const MOTIF = 'Demandes de machines de recette pour la conformité'

/** Change de profil de démonstration (rechargement) en gardant l'état simulé de l'onglet. */
async function devenir(page: Page, profil: ProfilDemo, theme: 'light' | 'dark', chemin: string) {
  await demo(page, { profil, theme })
  await page.goto(`${ADRESSES.departement}${chemin}`)
}

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : public d’un processus publié modifiable sans nouvelle version, services simulés (${suffixe})`, () => {
    test('Scénario : l’administrateur des référentiels ajoute une personne au public avec un motif ; elle voit aussitôt le processus', async ({ page }) => {
      test.setTimeout(120_000)

      await test.step('Étant donné la responsable de la conformité sur « Lancer une demande » : le processus réservé n’y figure pas', async () => {
        await devenir(page, 'ROLE_CONFORMITE', theme, '/des/demandes/lancer')
        await expect(page.getByRole('heading', { level: 1, name: 'Lancer une demande' })).toBeVisible()
        await expect(page.getByRole('heading', { level: 2, name: 'Service des ressources humaines' })).toBeVisible()
        await expect(page.getByText(VM)).toHaveCount(0)
      })

      await test.step('Quand l’ingénieur d’homologation ouvre le processus publié : son public reste réglable', async () => {
        await devenir(page, 'ROLE_INGENIEUR_HOMOLOGATION', theme, '/des/processus')
        await page.getByRole('button', { name: VM, exact: true }).click()
        await expect(page.getByRole('heading', { level: 1, name: VM })).toBeVisible()
        await expect(page.getByText(/son public reste réglable/)).toBeVisible()
        await expect(page.getByRole('button', { name: 'Ajouter une étape' })).toHaveCount(0)
        await expect(page.getByRole('button', { name: 'Nouvelle version' })).toBeVisible()
        await capturer(page, `modelisateur-publie-${suffixe}`)
        await page.getByRole('button', { name: `Régler qui peut voir et lancer « ${VM} »` }).click()
      })

      await test.step('Alors la fenêtre laisse le public modifiable, le formulaire et la séparation des fonctions en lecture', async () => {
        const d = page.getByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
        await expect(d.getByText('Version publiée : seul le public se règle ici')).toBeVisible()
        await expect(d.getByText('Le changement vaut pour les prochains lancements ; les demandes déjà lancées ne changent pas.')).toBeVisible()
        await expect(d.getByLabel('Personnes désignées')).toBeEnabled()
        await expect(d.getByLabel('L’initiateur peut aussi exécuter, valider ou signer les étapes de sa propre demande')).toBeDisabled()
        await expect(d.getByRole('button', { name: 'Ajouter un champ' })).toHaveCount(0)
        await expect(d.getByRole('button', { name: 'Enregistrer le public' })).toBeDisabled()
        await capturer(page, `fenetre-public-publie-${suffixe}`)
      })

      await test.step('Quand il ajoute la responsable de la conformité avec un motif et enregistre', async () => {
        const d = page.getByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
        const zone = d.getByTestId('initiateurs-personnes')
        await expect(zone.getByText('2 personnes choisies')).toBeVisible()
        await zone.getByRole('searchbox').fill('Mireille')
        await zone.getByRole('button', { name: /Mireille Obame Nze/ }).click()
        await expect(zone.getByText('3 personnes choisies')).toBeVisible()
        // Motif obligatoire : 10 caractères au moins.
        await expect(d.getByRole('button', { name: 'Enregistrer le public' })).toBeDisabled()
        await d.getByLabel(/^Motif/).fill(MOTIF)
        await d.getByLabel(/^Motif/).scrollIntoViewIfNeeded()
        await capturer(page, `fenetre-public-motif-${suffixe}`)
        await d.getByRole('button', { name: 'Enregistrer le public' }).click()
        await expect(d).toHaveCount(0)
        await expect(page.getByTestId('public-processus')).toContainText('Personnes désignées (Idriss Mahamat Saleh, Rachel Biyong Mengue, Mireille Obame Nze)')
        await expect(page.getByRole('heading', { level: 1, name: VM })).toBeVisible()
        await capturer(page, `public-enregistre-${suffixe}`)
      })

      await test.step('Alors la responsable de la conformité voit aussitôt le processus dans « Lancer une demande »', async () => {
        await devenir(page, 'ROLE_CONFORMITE', theme, '/des/demandes/lancer')
        await expect(page.getByRole('heading', { level: 2, name: 'Pôle système' })).toBeVisible()
        await expect(page.getByRole('button', { name: `Lancer « ${VM} »` })).toBeVisible()
        await expect(page.getByText('Ouvert à des personnes désignées')).toBeVisible()
        await capturer(page, `conformite-lancer-demande-${suffixe}`)
      })
    })
  })
}
