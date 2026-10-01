import { ADRESSES, demo, expect, test } from '../fixtures'
import { monTravail, naviguer } from './outils'

// Fonctionnalité : les autres acteurs du département (Normes et Agréments, ingénieur d'homologation)
// ne cadrent pas : « Mon travail » sans ouverture ni désignation, sans pilotage des projets, fiche d'un
// adhérent sans ouverture de liaison, listes des liaisons sans ouverture ni désignation (D-58, D-61).

test.describe('Fonctionnalité : tableaux de bord en lecture seule', () => {
  for (const [profil, libelle] of [['ROLE_SNA', 'Normes et Agréments'], ['ROLE_INGENIEUR_HOMOLOGATION', 'Ingénieur d\'homologation']] as const) {
    test(`Scénario : ${libelle} consulte sans ouvrir ni désigner`, async ({ page }) => {
      await demo(page, { profil, theme: 'light' })
      await test.step('Étant donné son tableau de bord', async () => {
        await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
        await expect(page.locator('.gm-pagehead-user', { hasText: libelle })).toBeVisible()
        await expect(monTravail(page).getByRole('heading', { name: /^Liaisons suivies \(\d+\)$/ })).toBeVisible()
        await expect(monTravail(page).getByRole('button', { name: 'Ouvrir une liaison' })).toHaveCount(0)
        await expect(monTravail(page).getByRole('button', { name: 'Désigner le conducteur' })).toHaveCount(0)
        await expect(page.getByRole('region', { name: 'Pilotage des projets de certification' })).toHaveCount(0)
        await expect(page.getByRole('region', { name: 'Mes accès' })).toHaveCount(0)
        await expect(page.getByRole('region', { name: 'Relations avec les membres (DDRM)' })).toHaveCount(0)
      })
      await test.step('Alors la fiche d’un adhérent ne propose aucune ouverture de liaison', async () => {
        await naviguer(page, 'Institutions membres')
        await expect(page.getByRole('button', { name: 'Enregistrer une prise de contact' })).toHaveCount(0)
        await page.getByRole('link', { name: 'Mobile Money Sanaga' }).click()
        await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mobile Money Sanaga')
        await expect(page.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' })).toHaveCount(0)
        await page.getByRole('tab', { name: 'Liaisons' }).click()
        await expect(page.getByText('Aucune liaison n\'est ouverte pour cette institution.')).toBeVisible()
      })
      await test.step('Et la liste des liaisons du parcours n’offre ni ouverture ni « Mes projets »', async () => {
        await naviguer(page, 'Liaisons et parcours')
        await expect(page.getByRole('heading', { level: 1 })).toHaveText('Liaisons')
        await expect(page.getByRole('link', { name: 'LIA-2026-030' })).toBeVisible()
        await expect(page.getByRole('button', { name: 'Ouvrir une liaison' })).toHaveCount(0)
        await expect(page.getByRole('tab', { name: 'Mes projets' })).toHaveCount(0)
      })
      await test.step('Et la liste historique des liaisons n’offre ni ouverture ni désignation', async () => {
        await page.goto(`${ADRESSES.departement}/des/liaisons?onglet=pilotage`)
        await expect(page.getByRole('link', { name: 'Établissement de paiement A' })).toBeVisible()
        await page.getByRole('tab', { name: 'Liste des liaisons' }).click()
        await expect(page.getByRole('switch', { name: 'Mes projets' })).not.toBeChecked()
        await expect(page.getByRole('button', { name: 'Ouvrir une liaison' })).toHaveCount(0)
        await expect(page.getByRole('button', { name: 'Désigner le conducteur' })).toHaveCount(0)
      })
    })
  }
})
