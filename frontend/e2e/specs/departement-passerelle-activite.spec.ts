import { ADRESSES, expect, test } from '../fixtures'
import { naviguer } from './outils'
import { PasserellePilotage } from './passerelle-pilotage'

// Fonctionnalité : « Mon activité » sur la passerelle (VITE_SERVICES=http), dont Playwright simule MS-PILOTAGE
// et l'organisation de MS-IAM : chaque geste atteint la route exacte du contrat (docs/lot1/mon-activite.md §2),
// avec son corps ; un refus 422 (PIL_RAPPORT_INCOMPLET) est expliqué, message traduit et détail du service, et le
// rapport reste un brouillon, modifié (jamais recréé) au nouvel essai ; le destinataire et son motif sont annoncés.

test('Scénario : un collaborateur tient sa journée et soumet son rapport par la passerelle', async ({ page }) => {
  const gw = new PasserellePilotage()
  await gw.brancher(page)
  await page.goto(`${ADRESSES.departementHttp}/des/tableau-de-bord`)
  const section = page.getByRole('region', { name: 'Ma journée' })
  await expect(section.getByText('0 / 2')).toBeVisible()
  await naviguer(page, 'Ma journée')
  const activites = page.getByRole('table')
  await expect(activites.getByText('Configurer le VPN du participant')).toBeVisible()
  await test.step('Quand il ajoute une activité et en termine une autre', async () => {
    await page.getByRole('button', { name: 'Ajouter une activité' }).click()
    const d = page.getByRole('dialog', { name: 'Ajouter une activité' })
    await d.getByLabel(/^Intitulé/).fill('Tester la liaison de secours')
    await d.getByLabel(/Temps passé/).fill('30')
    await d.getByRole('button', { name: 'Ajouter une activité' }).click()
    await expect(activites.getByText('Tester la liaison de secours')).toBeVisible()
    await activites.getByRole('combobox', { name: 'Statut de « Configurer le VPN du participant »' }).selectOption('FAITE')
    await expect(page.getByRole('status').filter({ hasText: 'Statut changé : Faite.' })).toBeVisible()
    const creation = gw.appels.find((a) => a.methode === 'POST' && a.chemin === '/bff/des/pilotage/activites')!
    expect(creation.corps).toMatchObject({ titre: 'Tester la liaison de secours', dureeMinutes: 30, description: null })
    expect(gw.appels.find((a) => a.chemin === '/bff/des/pilotage/activites/A-1/statut')!.corps).toEqual({ statut: 'FAITE' })
    expect(gw.appels.find((a) => a.methode === 'GET' && a.chemin === '/bff/des/pilotage/activites')!.recherche).toMatch(/^\?du=\d{4}-\d{2}-\d{2}&au=\d{4}-\d{2}-\d{2}$/)
  })
  await test.step('Et il soumet son rapport prérempli : refus expliqué, puis remis à son n+1', async () => {
    await page.getByRole('button', { name: 'Rédiger mon rapport' }).click()
    const d = page.getByRole('dialog', { name: 'Nouveau rapport' })
    await expect(d.getByText('Le rapport soumis sera remis à Gaston Ndzana Ebode, votre responsable.')).toBeVisible()
    await d.getByRole('button', { name: 'Préremplir à partir de mes activités' }).click()
    await expect(d.getByLabel(/Contenu du rapport/)).toHaveValue(/## En cours/)
    await d.getByRole('button', { name: 'Enregistrer et soumettre' }).click()
    await expect(d.getByText(/Le rapport n’a ni texte ni document/)).toBeVisible()
    await expect(d.getByText('Le rapport est vide')).toBeVisible()
    await expect(d.getByText('e2e-PIL_RAPPORT_INCOMPLET')).toBeVisible()
    await d.getByRole('button', { name: 'Enregistrer et soumettre' }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Rapport remis à Gaston Ndzana Ebode (votre n+1).' })).toBeVisible()
    const ecritures = gw.appels.filter((a) => a.methode !== 'GET' && a.chemin.startsWith('/bff/des/pilotage/rapports')).map((a) => `${a.methode} ${a.chemin}`)
    expect(ecritures).toEqual([
      'POST /bff/des/pilotage/rapports/preremplissage',
      'POST /bff/des/pilotage/rapports',
      'POST /bff/des/pilotage/rapports/R-1/soumission',
      'PUT /bff/des/pilotage/rapports/R-1',
      'POST /bff/des/pilotage/rapports/R-1/soumission',
    ])
    expect(gw.appels.find((a) => a.chemin === '/bff/des/pilotage/rapports' && a.methode === 'POST')!.corps).toMatchObject({ type: 'QUOTIDIEN', mode: 'PREREMPLI' })
  })
  await test.step('Alors « Mon compte » montre son rattachement, lu sur MS-IAM', async () => {
    await page.goto(`${ADRESSES.departementHttp}/des/mon-compte`)
    await expect(page.getByText('Gaston Ndzana Ebode, Chef du service Production informatique')).toBeVisible()
    expect(gw.appels.some((a) => a.chemin === '/bff/des/iam/organisation/moi')).toBe(true)
  })
})
