import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { signer } from './outils'

// Exploitation Kubernetes en services simulés (docs/exploitation/operations-infrastructure.md §3) : fiche d'une
// charge (courbes, pods, événements), nœuds du cluster, jobs et cronjobs ; gestes arrêter, démarrer et rétablir
// la version précédente, motivés et confirmés par la clé de sécurité.

const ligne = (page: Page, nom: string) => page.getByRole('row').filter({ has: page.getByText(nom, { exact: true }) }).first()
async function geste(page: Page, nom: string, libelle: string, motif: string) {
  await ligne(page, nom).getByRole('button', { name: libelle }).click()
  const d = await signer(page, motif)
  await expect(d.getByText('Signature confirmée')).toBeVisible()
  await d.getByRole('button', { name: 'Continuer' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
}

test.describe('Fonctionnalité : exploitation Kubernetes (lecture et gestes)', () => {
  test('Scénario : pods et événements d’une charge, nœuds et tâches ; arrêt, démarrage, retour à la version précédente', async ({ page }) => {
    test.slow()
    await demo(page, { profil: 'ROLE_ADMIN_SYSTEME' })
    await page.goto(`${ADRESSES.departement}/des/plateforme/infrastructure`)
    await expect(page.getByRole('heading', { level: 1, name: 'Infrastructure' })).toBeVisible()

    await test.step('Fiche de la charge : courbes, pods, événements', async () => {
      await ligne(page, 'homologation').getByRole('button', { name: 'Courbes de homologation' }).click()
      const fiche = page.getByRole('dialog')
      await fiche.getByRole('tab', { name: 'Pods' }).click()
      await expect(fiche.getByRole('table')).toBeVisible()
      await fiche.getByRole('tab', { name: 'Événements' }).click()
      await expect(fiche.getByRole('table')).toBeVisible()
      await fiche.getByRole('button', { name: 'Fermer' }).first().click()
      await expect(page.getByRole('dialog')).toHaveCount(0)
    })

    await test.step('Nœuds du cluster, jobs et cronjobs', async () => {
      await expect(page.getByRole('heading', { name: 'Nœuds du cluster' })).toBeVisible()
      await expect(page.getByRole('heading', { name: 'Tâches (jobs et cronjobs)' })).toBeVisible()
    })

    await test.step('Gestes : arrêter puis démarrer, rétablir la version précédente', async () => {
      await geste(page, 'certification', 'Arrêter certification', 'Maintenance du service de certification')
      await expect(ligne(page, 'certification').getByRole('button', { name: 'Démarrer certification' })).toBeVisible({ timeout: 15_000 })
      await geste(page, 'certification', 'Démarrer certification', 'Fin de la maintenance de la certification')
      await geste(page, 'homologation', 'Rétablir la version précédente de homologation', 'Régression constatée après la mise à jour')
      const historique = page.locator('section.gm-card').filter({ has: page.getByRole('heading', { name: 'Historique des opérations' }) })
      await expect(historique.getByText('Régression constatée après la mise à jour', { exact: true })).toBeVisible({ timeout: 10_000 })
    })
  })
})
