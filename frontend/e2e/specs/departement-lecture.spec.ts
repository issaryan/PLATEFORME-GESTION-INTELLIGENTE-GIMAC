import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : les autres acteurs du département (normes et agréments, ingénieur d'homologation)
// consultent en lecture seule : tableau de bord sans cadrage ni « Mes projets », fiche d'un adhérent
// sans ouverture de liaison, liste des liaisons sans ouverture ni désignation (D-58, D-61).

test.describe('Fonctionnalité : tableaux de bord en lecture seule', () => {
  for (const [profil, libelle] of [['ROLE_SNA', 'Normes et agréments'], ['ROLE_INGENIEUR_HOMOLOGATION', 'Ingénieur d\'homologation']] as const) {
    test(`Scénario : ${libelle} consulte sans ouvrir ni désigner`, async ({ page }) => {
      await demo(page, { profil, theme: 'light' })
      await test.step('Étant donné son tableau de bord', async () => {
        await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
        await expect(page.getByText(libelle, { exact: true })).toBeVisible()
        await expect(page.getByRole('link', { name: 'Pilotage des liaisons' })).toBeVisible()
        await expect(page.getByRole('region', { name: 'Mes accès' })).toBeVisible()
        await expect(page.getByRole('region', { name: 'Cadrage des liaisons' })).toHaveCount(0)
        await expect(page.getByRole('region', { name: 'Mes projets' })).toHaveCount(0)
        await expect(page.getByRole('region', { name: 'Relations avec les membres (DDRM)' })).toHaveCount(0)
      })
      await test.step('Alors la fiche d’un adhérent ne propose aucune ouverture de liaison', async () => {
        await page.getByRole('region', { name: 'Mes accès' }).getByRole('link', { name: 'Institutions membres' }).click()
        await expect(page.getByRole('button', { name: 'Enregistrer une prise de contact' })).toHaveCount(0)
        await page.getByRole('link', { name: 'Mobile Money Sanaga' }).click()
        await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mobile Money Sanaga')
        await expect(page.getByRole('button', { name: 'Ouvrir une liaison (cadrage)' })).toHaveCount(0)
        await page.getByRole('tab', { name: 'Liaisons' }).click()
        await expect(page.getByText('Aucune liaison n\'est ouverte pour cette institution.')).toBeVisible()
      })
      await test.step('Et la liste des liaisons n’offre ni ouverture ni désignation', async () => {
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
