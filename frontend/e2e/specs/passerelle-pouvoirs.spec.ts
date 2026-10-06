import { ADRESSES, expect, test } from '../fixtures'
import { confirmer, signerEtTerminer } from './outils'
import { Passerelle, PERMISSIONS_SUPER_ADMIN } from './passerelle'
import { PasserellePouvoirs } from './passerelle-pouvoirs'

// Fonctionnalité : pouvoirs hiérarchiques sur la passerelle (D-81, VITE_SERVICES=http). Chaque geste atteint la
// route du contrat avec son corps : demandes du chef (MS-IAM), tâches confiées à une unité ou à plusieurs personnes,
// attribution, relance (MS-PILOTAGE) ; acceptation, refus, matrice et profils créés avec l'assertion FIDO2
// (X-Reauth-Assertion) côté console. Les refus du service sont expliqués.

test.describe('Fonctionnalité : pouvoirs hiérarchiques par la passerelle', () => {
  test('Scénario : le chef du DES demande un compte, confie des tâches et relance', async ({ page }) => {
    const gw = new PasserellePouvoirs({ departement: true })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.departementHttp}/des/equipe?vue=organisation`)

    await test.step('Organisation lue sur GET /bff/des/iam/pouvoirs/moi ; demande de compte refusée puis déposée', async () => {
      await page.getByRole('tree', { name: 'Mes subordonnés' }).getByText('Idriss Mahamat Saleh').click()
      await expect(page.getByRole('group', { name: 'Gestes permis' }).getByRole('button', { name: 'Demander la désactivation' })).toBeVisible()
      await page.getByRole('button', { name: 'Demander un compte' }).click()
      const d = page.getByRole('dialog', { name: 'Demander un compte' })
      await d.getByLabel(/^Prénom/).fill('Blaise')
      await d.getByLabel(/^Nom/).fill('Ondo')
      await d.getByLabel(/^Identifiant/).fill('b.ondo')
      await d.getByLabel(/^Courriel/).fill('b.ondo@gimac-afr.org')
      await d.getByLabel(/^Fonction/).fill('Technicien réseau')
      await d.getByLabel(/^Unité/).selectOption('POLE_RESEAU')
      await d.getByRole('group', { name: 'Profils demandés' }).getByLabel('Pôle réseau', { exact: true }).check()
      await d.getByLabel(/^Motif/).fill('Renfort du pôle réseau')
      await d.getByRole('button', { name: 'Envoyer la demande' }).click()
      await expect(d.getByText('Une demande de ce type est déjà en attente pour cette personne.')).toBeVisible()
      await d.getByRole('button', { name: 'Envoyer la demande' }).click()
      await expect(page.getByRole('status').filter({ hasText: 'Demande adressée à l’administration.' })).toBeVisible()
      expect(gw.dernier('POST', '/bff/des/iam/demandes')!.corps).toMatchObject({
        type: 'CREATION_COMPTE', cibleId: null, motif: 'Renfort du pôle réseau',
        contenu: { identifiant: 'b.ondo', unite: 'POLE_RESEAU', responsableId: 'U-001', profils: ['ROLE_POLE_RESEAU'] },
      })
    })

    await test.step('Rattachement d’un subordonné direct : route du responsable, les deux valeurs et le motif', async () => {
      await page.getByRole('tree', { name: 'Mes subordonnés' }).getByText('Gaston Ndzana Ebode').click()
      await page.getByRole('group', { name: 'Gestes permis' }).getByRole('button', { name: 'Modifier le rattachement' }).click()
      const d = page.getByRole('dialog', { name: 'Modifier le rattachement' })
      await d.getByLabel(/^Unité/).selectOption('DES')
      await d.getByLabel(/^Motif/).fill('Rattachement direct au département')
      await d.getByRole('button', { name: 'Enregistrer le rattachement' }).click()
      await expect(d.getByText('Boucle hiérarchique : ce choix placerait le compte dans sa propre chaîne.')).toBeVisible()
      await d.getByRole('button', { name: 'Enregistrer le rattachement' }).click()
      await expect(page.getByRole('status').filter({ hasText: 'Rattachement de Gaston Ndzana Ebode modifié.' })).toBeVisible()
      expect(gw.dernier('PUT', '/bff/des/iam/pouvoirs/subordonnes/u-resp-info/rattachement')!.corps).toEqual({ unite: 'DES', responsableId: 'U-001', motif: 'Rattachement direct au département' })
      expect(gw.appels.some((x) => /\/iam\/comptes\/[^/]+\/rattachement$/.test(x.chemin))).toBe(false)
    })

    await test.step('Mes demandes : liste « miennes » paginée à partir de 0, annulation', async () => {
      await page.getByRole('tab', { name: 'Mes demandes' }).click()
      await expect(page.getByText('Nouveau compte : Blaise Ondo')).toBeVisible()
      expect(gw.dernier('GET', '/bff/des/iam/demandes')!.recherche).toBe('?vue=miennes&page=0&taille=20')
      await page.getByRole('row').filter({ hasText: 'Nadège Mvondo Assiga' }).getByRole('button', { name: 'Désactivation ou réactivation' }).click()
      await page.getByRole('dialog', { name: 'Désactivation ou réactivation' }).getByRole('button', { name: 'Annuler la demande' }).click()
      await expect(page.getByRole('status').filter({ hasText: 'Demande annulée.' })).toBeVisible()
      expect(gw.dernier('POST', '/bff/des/iam/demandes/DEM-2/annulation')).toBeTruthy()
    })

    await test.step('Tâches : à une unité (uniteCible), attribution, à plusieurs personnes (responsables), groupe, relance', async () => {
      await page.goto(`${ADRESSES.departementHttp}/des/taches?unite=POLE_RESEAU`)
      let d = page.getByRole('dialog', { name: 'Nouvelle tâche' })
      await d.getByLabel(/^Intitulé/).fill('Superviser la recette')
      await d.getByRole('button', { name: 'Créer la tâche' }).click()
      await expect(page.getByRole('status').filter({ hasText: 'Tâche confiée à l’unité « Pôle réseau ».' })).toBeVisible()
      expect(gw.dernier('POST', '/bff/des/pilotage/taches')!.corps).toMatchObject({ titre: 'Superviser la recette', uniteCible: 'POLE_RESEAU' })
      await page.getByRole('tab', { name: 'À prendre' }).click()
      await page.getByRole('button', { name: 'Superviser la recette' }).click()
      expect(gw.appels.some((x) => x.chemin === '/bff/des/pilotage/taches' && x.recherche === '?vue=unite&page=0&taille=20')).toBe(true)
      await page.getByRole('dialog', { name: 'Superviser la recette' }).getByRole('button', { name: 'Attribuer la tâche' }).click()
      const a = page.getByRole('dialog', { name: 'Attribuer la tâche' })
      await a.getByLabel(/^Responsable/).selectOption('u-res')
      await a.getByRole('button', { name: 'Attribuer la tâche' }).click()
      await expect(page.getByRole('status').filter({ hasText: 'Tâche attribuée à Idriss Mahamat Saleh.' })).toBeVisible()
      expect(gw.dernier('POST', '/bff/des/pilotage/taches/T-1/attribution')!.corps).toEqual({ responsableId: 'u-res' })
      await page.getByRole('dialog', { name: 'Superviser la recette' }).getByRole('button', { name: 'Fermer' }).last().click()

      await page.getByRole('button', { name: 'Nouvelle tâche' }).click()
      d = page.getByRole('dialog', { name: 'Nouvelle tâche' })
      await d.getByLabel(/^Intitulé/).fill('Relire la procédure')
      await d.getByLabel('Plusieurs personnes').check()
      await d.getByLabel(/^Idriss Mahamat Saleh/).check()
      await d.getByLabel(/^Nadège Mvondo Assiga/).check()
      await d.getByRole('button', { name: 'Créer la tâche' }).click()
      await expect(page.getByRole('status').filter({ hasText: 'Tâche confiée à 2 personnes.' })).toBeVisible()
      expect(gw.dernier('POST', '/bff/des/pilotage/taches')!.corps).toMatchObject({ titre: 'Relire la procédure', responsables: ['u-res', 'u-collab'] })
      await page.getByRole('tab', { name: 'Confiées' }).click()
      await page.getByRole('button', { name: 'Suivi du groupe « Relire la procédure »' }).first().click()
      const g = page.getByRole('dialog', { name: 'Suivi : Relire la procédure' })
      await expect(g.getByText('0 sur 2')).toBeVisible()
      expect(gw.dernier('GET', '/bff/des/pilotage/taches/groupes/G-1')).toBeTruthy()
      await g.getByRole('button', { name: 'Relancer Nadège Mvondo Assiga' }).click()
      await expect(g.getByText('Relance envoyée à Nadège Mvondo Assiga.')).toBeVisible()
      await g.getByRole('button', { name: 'Relancer Nadège Mvondo Assiga' }).click()
      await expect(g.getByText('Une relance a déjà été envoyée il y a moins de 24 heures.')).toBeVisible()
      expect(gw.appels.filter((x) => x.methode === 'POST' && /\/relance$/.test(x.chemin))).toHaveLength(2)
    })
  })

  test('Scénario : le super administrateur traite les demandes, modifie la matrice et crée un profil (console)', async ({ page }) => {
    const console = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await console.brancher(page)
    const gw = new PasserellePouvoirs()
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/demandes`)
    await page.getByRole('row').filter({ hasText: 'Ingrid Fouda Ngono' }).getByRole('button', { name: 'Création de compte' }).click()
    expect(gw.dernier('GET', '/bff/console/iam/demandes')!.recherche).toBe('?vue=a-traiter&etat=EN_ATTENTE&page=0&taille=20')
    await page.getByRole('dialog', { name: 'Création de compte' }).getByRole('button', { name: 'Accepter avec ma clé' }).click()
    await signerEtTerminer(page, 'Création validée par la direction')
    await expect(page.getByText('Demande acceptée : Nouveau compte : Ingrid Fouda Ngono.')).toBeVisible()
    const acceptation = gw.dernier('POST', '/bff/console/iam/demandes/DEM-1/acceptation')!
    expect(acceptation.entetes['x-reauth-assertion']).toBeTruthy()
    expect(acceptation.corps).toEqual({ motif: 'Création validée par la direction' })
    await page.getByRole('row').filter({ hasText: 'Nadège Mvondo Assiga' }).getByRole('button', { name: 'Désactivation ou réactivation' }).click()
    await page.getByRole('dialog', { name: 'Désactivation ou réactivation' }).getByRole('button', { name: 'Refuser la demande' }).click()
    await confirmer(page, 'Refuser la demande', 'Départ non confirmé par les ressources humaines')
    await expect(page.getByText('Demande refusée : Nadège Mvondo Assiga.')).toBeVisible()
    expect(gw.dernier('POST', '/bff/console/iam/demandes/DEM-2/refus')!.corps).toEqual({ motif: 'Départ non confirmé par les ressources humaines' })

    await page.goto(`${ADRESSES.consoleHttp}/console/matrice-pouvoirs`)
    await page.getByRole('button', { name: 'Pouvoirs du profil Chef du DES' }).click()
    const m = page.getByRole('dialog', { name: 'Pouvoirs du profil Chef du DES' })
    await m.getByLabel(/^Demander une désactivation/).selectOption('DIRECTS')
    await m.getByRole('button', { name: 'Continuer vers la signature' }).click()
    await signerEtTerminer(page, 'Revue annuelle des pouvoirs')
    await expect(page.getByText('Pouvoirs du profil Chef du DES enregistrés.')).toBeVisible()
    const put = gw.dernier('PUT', '/bff/console/iam/pouvoirs/matrice/ROLE_CHEF_DES')!
    expect(put.entetes['x-reauth-assertion']).toBeTruthy()
    expect(put.corps).toMatchObject({ actions: { STATUT_DEMANDER: 'DIRECTS', TACHE_CONFIER: 'UNITES' }, profilsDemandables: ['ROLE_COLLABORATEUR', 'ROLE_POLE_RESEAU'], motif: 'Revue annuelle des pouvoirs' })

    await page.goto(`${ADRESSES.consoleHttp}/console/profils`)
    await page.getByRole('button', { name: 'Nouveau profil' }).click()
    const p = page.getByRole('dialog', { name: 'Nouveau profil' })
    await p.getByLabel(/^Code/).fill('ROLE_ASTREINTE')
    await p.getByLabel(/^Description/).fill('Astreinte de supervision')
    await p.getByLabel(/^Libellé/).fill('Astreinte')
    await p.getByRole('button', { name: 'Droits de Liaisons et parcours' }).click()
    await p.getByRole('checkbox', { name: /\(HOM_EVIDENCE_SUBMIT\)/ }).check()
    await p.getByRole('button', { name: 'Continuer vers la signature' }).click()
    await signerEtTerminer(page, 'Astreinte de nuit du NOC')
    await expect(page.getByText('Profil ROLE_ASTREINTE créé.')).toBeVisible()
    expect(gw.dernier('POST', '/bff/console/iam/configuration/profils')!.corps).toEqual({ code: 'ROLE_ASTREINTE', libelle: 'Astreinte', description: 'Astreinte de supervision', permissions: ['HOM_DASHBOARD_READ', 'HOM_EVIDENCE_SUBMIT', 'HOM_LIAISON_LIST', 'HOM_LIAISON_READ'], motif: 'Astreinte de nuit du NOC' })
    await page.getByRole('button', { name: 'Supprimer le profil ROLE_ASTREINTE' }).press('Enter')
    await signerEtTerminer(page, 'Profil créé par erreur')
    await expect(page.getByText('Profil ROLE_ASTREINTE supprimé.')).toBeVisible()
    expect(gw.dernier('DELETE', '/bff/console/iam/configuration/profils/ROLE_ASTREINTE')!.entetes['x-reauth-assertion']).toBeTruthy()
  })
})
