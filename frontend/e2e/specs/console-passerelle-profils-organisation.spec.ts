import type { Locator, Page } from '@playwright/test'
import { ADRESSES, expect, test } from '../fixtures'
import { signerEtTerminer } from './outils'
import { Passerelle, PERMISSIONS_SUPER_ADMIN } from './passerelle'
import { PasserelleProfilsOrganisation } from './passerelle-profils-organisation'

// Fonctionnalité : profils, rôles par fonctionnalité et organigramme sur la passerelle (D-86 et D-90,
// VITE_SERVICES=http). Chaque geste atteint la route du contrat avec son corps et, pour les actes sensibles,
// l'assertion FIDO2 (X-Reauth-Assertion) : présentation d'un profil (PUT /configuration/profils/{code}),
// composition par fonctionnalités (route de composition existante), colonne « processus » de la matrice,
// déplacement d'une unité, n+1 et rattachement (routes d'organisation existantes, unité et responsable toujours reçus).

async function deposer(page: Page, source: Locator, cible: Locator) {
  const dt = await page.evaluateHandle(() => new DataTransfer())
  await source.dispatchEvent('dragstart', { dataTransfer: dt })
  await cible.dispatchEvent('dragover', { dataTransfer: dt })
  await cible.dispatchEvent('drop', { dataTransfer: dt })
  await source.dispatchEvent('dragend', { dataTransfer: dt })
}

test.describe('Fonctionnalité : profils et organigramme par la passerelle', () => {
  test('Scénario : le super administrateur présente et compose un profil, puis règle la matrice des processus', async ({ page }) => {
    const console = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await console.brancher(page)
    const gw = new PasserelleProfilsOrganisation()
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/profils`)

    await test.step('Présentation : libellé et description du serveur, PUT signé avec le motif', async () => {
      const ligne = page.getByRole('row').filter({ hasText: 'ROLE_SNA' })
      await expect(ligne.getByText('Carine Ebongue Ndjock')).toBeVisible()
      await page.getByRole('button', { name: 'Modifier le libellé et la description du profil Pôle réseau' }).click()
      const d = page.getByRole('dialog', { name: 'Modifier le profil' })
      await d.getByLabel(/^Libellé/).fill('Pôle réseau et télécoms')
      await d.getByLabel(/^Description/).fill('Liens, routage et télécoms.')
      await d.getByRole('button', { name: 'Continuer vers la signature' }).click()
      await signerEtTerminer(page, 'Évolution du périmètre du pôle')
      await expect(page.getByRole('status').filter({ hasText: 'Profil « Pôle réseau et télécoms » enregistré.' })).toBeVisible()
      const put = gw.dernier('PUT', '/bff/console/iam/configuration/profils/ROLE_POLE_RESEAU')!
      expect(put.corps).toEqual({ libelle: 'Pôle réseau et télécoms', description: 'Liens, routage et télécoms.', motif: 'Évolution du périmètre du pôle' })
      expect(put.entetes['x-reauth-assertion']).toBeTruthy()
    })

    await test.step('Composition par fonctionnalités : la route de composition reçoit les permissions des droits cochés', async () => {
      await page.getByRole('row').filter({ hasText: 'ROLE_SNA' }).getByRole('button', { name: 'Modifier la composition' }).click()
      const d = page.getByRole('dialog', { name: 'Profil Normes et Agréments' })
      await d.getByRole('checkbox', { name: 'Mon équipe' }).click()
      await d.getByRole('button', { name: 'Continuer vers la signature' }).click()
      await signerEtTerminer(page, 'Suivi de l’équipe confié au service')
      const put = gw.dernier('PUT', '/bff/console/iam/configuration/profils/ROLE_SNA/permissions')!
      expect(put.entetes['x-reauth-assertion']).toBeTruthy()
      expect(put.corps).toMatchObject({ motif: 'Suivi de l’équipe confié au service' })
      expect((put.corps as { permissions: string[] }).permissions).toEqual(expect.arrayContaining(['HOM_EVIDENCE_SUBMIT', 'HOM_LIAISON_LIST', 'PIL_TEAM_READ']))
    })

    await test.step('Matrice : colonnes « processus », portée de lancement modifiée et signée', async () => {
      await page.goto(`${ADRESSES.consoleHttp}/console/matrice-pouvoirs`)
      await expect(page.getByRole('columnheader', { name: /Processus : lancer/ })).toBeVisible()
      await page.getByRole('button', { name: 'Pouvoirs du profil Chef du DES' }).click()
      const m = page.getByRole('dialog', { name: 'Pouvoirs du profil Chef du DES' })
      await m.getByLabel(/^Lancer un processus/).selectOption('DIRECTS')
      await m.getByRole('button', { name: 'Continuer vers la signature' }).click()
      await signerEtTerminer(page, 'Lancement réservé aux subordonnés directs')
      const put = gw.dernier('PUT', '/bff/console/iam/pouvoirs/matrice/ROLE_CHEF_DES')!
      expect(put.entetes['x-reauth-assertion']).toBeTruthy()
      expect(put.corps).toMatchObject({ actions: { PROCESSUS_DEFINIR: 'UNITES', PROCESSUS_LANCER: 'DIRECTS' }, motif: 'Lancement réservé aux subordonnés directs' })
    })
  })

  test('Scénario : le super administrateur réorganise l’organigramme (glisser-déposer et clavier)', async ({ page }) => {
    const console = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await console.brancher(page)
    const gw = new PasserelleProfilsOrganisation()
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/organigramme`)
    const unite = (code: string) => page.locator(`[data-unite="${code}"]`)
    const compte = (id: string) => page.locator(`[data-compte="${id}"]`)
    await expect(unite('DES')).toContainText('Chef : Christian Ndong Obiang')
    expect(gw.dernier('GET', '/bff/console/iam/comptes')!.recherche).toContain('taille=100')

    await test.step('Unité glissée sur une autre : PUT de l’unité avec sa nouvelle unité parente', async () => {
      await deposer(page, unite('POLE_RESEAU'), unite('DES'))
      const c = page.getByRole('alertdialog', { name: 'Déplacer l’unité' })
      await c.getByLabel(/Motif/).fill('Rattachement direct au département')
      await c.getByRole('button', { name: 'Déplacer l’unité' }).click()
      await expect(page.getByText('Unité déplacée.')).toBeVisible()
      expect(gw.dernier('PUT', '/bff/console/iam/organisation/unites/POLE_RESEAU')!.corps).toMatchObject({ code: 'POLE_RESEAU', parent: 'DES', libelle: 'Pôle réseau' })
    })

    await test.step('Compte glissé sur un compte : rattachement avec l’unité inchangée et le nouveau responsable', async () => {
      await unite('POLE_RESEAU').click()
      await deposer(page, compte('u-collab'), compte('u-res'))
      const c = page.getByRole('alertdialog', { name: 'Changer le n+1' })
      await c.getByLabel(/Motif/).fill('Encadrement par l’ingénieur réseau')
      await c.getByRole('button', { name: 'Changer le n+1' }).click()
      await expect(page.getByText('Nouveau n+1 enregistré.')).toBeVisible()
      expect(gw.dernier('PUT', '/bff/console/iam/comptes/u-collab/rattachement')!.corps).toEqual({ unite: 'POLE_RESEAU', responsableId: 'u-res' })
    })

    await test.step('Vue liste au clavier : chef d’unité désigné par PUT de l’unité', async () => {
      await page.getByRole('button', { name: 'Liste' }).click()
      await page.getByRole('button', { name: 'Désigner le chef de Service Normes et Agréments' }).press('Enter')
      const d = page.getByRole('dialog', { name: 'Désigner le chef d’unité' })
      await d.getByLabel(/^Chef d’unité/).selectOption('u-sna')
      await d.getByRole('button', { name: 'Continuer' }).click()
      const c = page.getByRole('alertdialog', { name: 'Désigner le chef d’unité' })
      await c.getByLabel(/Motif/).fill('Cheffe du service SNA')
      await c.getByRole('button', { name: 'Désigner le chef d’unité' }).click()
      await expect(page.getByText('Chef d’unité désigné.')).toBeVisible()
      expect(gw.dernier('PUT', '/bff/console/iam/organisation/unites/SNA')!.corps).toMatchObject({ responsableId: 'u-sna', parent: 'DES' })
      await page.getByRole('button', { name: 'Changer l’unité de Nadège Mvondo Assiga' }).press('Enter')
      const r = page.getByRole('dialog', { name: 'Rattacher à l’unité' })
      await r.getByLabel(/^Unité/).selectOption('SNA')
      await r.getByRole('button', { name: 'Continuer' }).click()
      const cr = page.getByRole('alertdialog', { name: 'Rattacher à l’unité' })
      await cr.getByLabel(/Motif/).fill('Renfort du service SNA')
      await cr.getByRole('button', { name: 'Rattacher à l’unité' }).click()
      await expect(page.getByText('Compte rattaché à l’unité.')).toBeVisible()
      expect(gw.dernier('PUT', '/bff/console/iam/comptes/u-collab/rattachement')!.corps).toEqual({ unite: 'SNA', responsableId: 'u-res' })
      await page.getByRole('button', { name: 'Pouvoirs de Christian Ndong Obiang' }).click()
      const panneau = page.getByRole('region', { name: 'Pouvoirs de Christian Ndong Obiang' })
      await expect(panneau).toBeVisible()
      await panneau.getByText(/personnes? couvertes?/).first().click()
    })
  })
})
