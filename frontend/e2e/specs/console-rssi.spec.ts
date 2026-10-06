import { ADRESSES, demo, expect, test } from '../fixtures'
import { confirmer, naviguer, ongletSelectionne, signer, signerEtTerminer } from './outils'

// Fonctionnalité : le RSSI pilote la posture de sécurité des comptes ; il n'octroie jamais de profil
// privilégié et ne change pas les niveaux de journalisation (D-62).

test.describe('Fonctionnalité : console du RSSI', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_RSSI', theme: 'dark' })
  })

  test('Scénario : consulter la posture de sécurité des comptes', async ({ page }) => {
    await page.goto(`${ADRESSES.console}/console`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord de la sécurité des SI')
    await expect(page.getByRole('region', { name: 'Gestion des utilisateurs' })).toBeVisible()
    await expect(page.getByRole('group', { name: 'Posture de sécurité des comptes' })).toBeVisible()
    await expect(page.getByText('Sans second facteur', { exact: true })).toBeVisible()
    await expect(page.getByText('Super administrateur : 1')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Derniers actes d’administration' })).toBeVisible()
    await expect(page.getByRole('region', { name: 'État en temps réel' })).toBeVisible()
    await page.getByRole('link', { name: 'Consulter la politique de sécurité' }).click()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Politique de sécurité')
    await page.getByRole('link', { name: 'Tableau de bord' }).click()
    await page.getByRole('link', { name: 'Voir tout le journal' }).click()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Journal d’administration')
  })

  test('Scénario : aucun profil privilégié proposé ni modifiable', async ({ page }) => {
    await page.goto(`${ADRESSES.console}/console/comptes/nouveau`)
    await expect(page.getByLabel('Normes et agréments')).toBeVisible()
    await expect(page.getByLabel('Super administrateur')).toHaveCount(0)
    await page.getByRole('button', { name: 'Créer le compte' }).click()
    await expect(page.getByText('Choisissez au moins un profil.')).toBeVisible()
    await page.getByRole('dialog').getByRole('button', { name: 'Annuler' }).click()
    await naviguer(page, 'Profils et permissions')
    await page.getByRole('row', { name: /ROLE_SUPER_ADMIN/ }).getByRole('button', { name: 'Voir la composition' }).click()
    await expect(page.getByRole('dialog').getByText('Consultation seule.')).toBeVisible()
    await ongletSelectionne(page.getByRole('dialog'), 'Droits')
    await expect(page.getByRole('dialog').getByLabel('IAM_ACCOUNT_READ')).toBeDisabled()
    await page.getByRole('dialog').getByRole('button', { name: 'Fermer' }).last().click()
  })

  test('Scénario : la plateforme garde toujours un super administrateur actif', async ({ page }) => {
    await page.goto(`${ADRESSES.console}/console/comptes/u-super-admin`)
    await page.getByRole('button', { name: 'Désactiver le compte' }).click()
    await confirmer(page, 'Désactiver le compte', 'Tentative de désactivation par le RSSI')
    await expect(page.getByText('La plateforme doit garder au moins un super administrateur actif.')).toBeVisible()
    await page.getByRole('button', { name: 'Modifier les profils' }).click()
    await expect(page.getByRole('dialog').getByLabel('Super administrateur')).toHaveCount(0)
    await page.getByRole('dialog').getByRole('button', { name: 'Annuler' }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await test.step('Quand le RSSI ajoute un profil standard, le profil privilégié détenu est conservé', async () => {
      await page.getByRole('button', { name: 'Modifier les profils' }).click()
      await page.getByRole('dialog').getByLabel('Conformité').check()
      await page.getByRole('button', { name: 'Continuer vers la signature' }).click()
      await signerEtTerminer(page, 'Intérim de la conformité par le super administrateur')
      await expect(page.getByText('Les profils du compte ont été modifiés.')).toBeVisible()
      await expect(page.locator('.gm-objhead').getByText('Super administrateur')).toBeVisible()
    })
    await test.step('Quand il tente de supprimer le dernier super administrateur, MS-IAM refuse', async () => {
      await page.getByRole('button', { name: 'Supprimer le compte' }).click()
      await page.getByRole('alertdialog').getByLabel('Je confirme : cette action est irréversible.').check()
      await page.getByRole('button', { name: 'Continuer vers la signature' }).click()
      const d = await signer(page, 'Tentative de suppression du dernier super administrateur')
      await expect(d.getByText('La plateforme doit garder au moins un super administrateur actif.')).toBeVisible()
      await d.getByRole('button', { name: 'Annuler' }).click()
    })
  })

  test('Scénario : consulter la journalisation sans pouvoir la modifier', async ({ page }) => {
    await page.goto(`${ADRESSES.console}/console/plateforme/journalisation?service=participant`)
    await expect(page.getByRole('cell', { name: 'cm.gimac.des', exact: true })).toBeVisible()
    await expect(page.getByText(/leur changement est réservé au super administrateur/)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Modifier le niveau' })).toHaveCount(0)
  })
})
