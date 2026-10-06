import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { naviguer, signerEtTerminer } from './outils'

// Fonctionnalité : pouvoirs des chefs sur leurs subordonnés (D-81, docs/lot1/pouvoirs-hierarchiques.md §7),
// services simulés. Le chef du DES parcourt l'organisation de son équipe, demande un compte à l'administration,
// confie une tâche au pôle réseau et l'attribue, confie une tâche à plusieurs personnes, suit le groupe et relance ;
// un membre du pôle réseau prend la tâche à prendre que le chef a confiée au pôle ; le super administrateur accepte
// la demande de compte avec sa clé de sécurité, modifie la matrice des pouvoirs et crée un profil. Captures en
// thème clair et sombre dans e2e/captures/pouvoirs (ou $GIMAC_CAPTURES/pouvoirs).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'pouvoirs') : resolve(ICI, '..', 'captures', 'pouvoirs')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const toast = (page: Page, texte: string | RegExp) => page.getByRole('status').filter({ hasText: texte })

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : pouvoirs hiérarchiques, services simulés (${suffixe})`, () => {
    test('Scénario : le chef du DES organise son équipe, demande un compte et confie des tâches', async ({ page }) => {
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)

      await test.step('Étant donné « Mon équipe », onglet Organisation : arbre des subordonnés et des unités dirigées', async () => {
        await naviguer(page, 'Mon équipe')
        await page.getByRole('tab', { name: 'Organisation' }).click()
        const arbre = page.getByRole('tree', { name: 'Mes subordonnés' })
        await arbre.getByText('Idriss Mahamat Saleh').click()
        const gestes = page.getByRole('group', { name: 'Gestes permis' })
        await expect(gestes.getByRole('button', { name: 'Confier une tâche' })).toBeVisible()
        await expect(gestes.getByRole('button', { name: 'Demander un profil' })).toBeVisible()
        await expect(gestes.getByRole('button', { name: 'Modifier le rattachement' })).toHaveCount(0)
        await expect(page.getByRole('tree', { name: 'Unités dirigées' }).getByText('Pôle système')).toBeVisible()
        await capturer(page, `equipe-organisation-${suffixe}`)
      })

      await test.step('Quand il demande un compte pour le pôle système, puis suit sa demande', async () => {
        await page.getByRole('button', { name: 'Demander un compte' }).click()
        const d = page.getByRole('dialog', { name: 'Demander un compte' })
        await d.getByLabel(/^Prénom/).fill('Blaise')
        await d.getByLabel(/^Nom/).fill('Ondo Mba')
        await d.getByLabel(/^Identifiant/).fill('b.ondo')
        await d.getByLabel(/^Courriel/).fill('b.ondo@gimac-afr.org')
        await d.getByLabel(/^Fonction/).fill('Technicien système')
        await d.getByLabel(/^Unité/).selectOption('POLE_SYSTEME')
        await d.locator('select[name="responsableId"]').selectOption('u-resp-info')
        await d.getByRole('group', { name: 'Profils demandés' }).getByLabel('Pôle système', { exact: true }).check()
        await d.getByLabel(/^Motif/).fill('Renfort du pôle système pour les recettes de 2026.')
        await capturer(page, `demande-compte-${suffixe}`)
        await d.getByRole('button', { name: 'Envoyer la demande' }).click()
        await expect(toast(page, 'Demande adressée à l’administration.')).toBeVisible()
        await page.getByRole('tab', { name: 'Mes demandes' }).click()
        await expect(page.getByText('Nouveau compte : Blaise Ondo Mba')).toBeVisible()
        await expect(page.getByRole('row').filter({ hasText: 'Désactivation ou réactivation' }).getByText('Refusée')).toBeVisible()
        await capturer(page, `mes-demandes-${suffixe}`)
      })

      await test.step('Et il confie une tâche au pôle réseau, la voit « À prendre » et l’attribue', async () => {
        await naviguer(page, 'Tâches')
        await page.getByRole('button', { name: 'Nouvelle tâche' }).click()
        const d = page.getByRole('dialog', { name: 'Nouvelle tâche' })
        await d.getByLabel(/^Intitulé/).fill('Superviser la recette du lien de secours')
        await d.getByLabel('Une unité').check()
        await d.getByLabel(/^Unité/).selectOption('POLE_RESEAU')
        await capturer(page, `tache-unite-${suffixe}`)
        await d.getByRole('button', { name: 'Créer la tâche' }).click()
        await expect(toast(page, 'Tâche confiée à l’unité « Pôle réseau ».')).toBeVisible()
        await page.getByRole('tab', { name: 'À prendre' }).click()
        await page.getByRole('button', { name: 'Superviser la recette du lien de secours' }).click()
        const f = page.getByRole('dialog', { name: 'Superviser la recette du lien de secours' })
        await expect(f.getByText('À prendre : Pôle réseau')).toBeVisible()
        await f.getByRole('button', { name: 'Attribuer la tâche' }).click()
        const a = page.getByRole('dialog', { name: 'Attribuer la tâche' })
        await a.getByLabel(/^Responsable/).selectOption('u-collab')
        await a.getByRole('button', { name: 'Attribuer la tâche' }).click()
        await expect(toast(page, 'Tâche attribuée à Nadège Mvondo Assiga.')).toBeVisible()
        const fiche = page.getByRole('dialog', { name: 'Superviser la recette du lien de secours' })
        await expect(fiche.getByText('Nadège Mvondo Assiga', { exact: true })).toBeVisible()
        await fiche.getByRole('button', { name: 'Fermer' }).last().click()
      })

      await test.step('Et il confie une tâche à plusieurs personnes, suit le groupe et relance', async () => {
        await page.getByRole('button', { name: 'Nouvelle tâche' }).click()
        const d = page.getByRole('dialog', { name: 'Nouvelle tâche' })
        await d.getByLabel(/^Intitulé/).fill('Relire la procédure de bascule')
        await d.getByLabel('Plusieurs personnes').check()
        await d.getByLabel(/^Idriss Mahamat Saleh/).check()
        await d.getByLabel(/^Chantal Ngo Bassong/).check()
        await d.getByRole('button', { name: 'Créer la tâche' }).click()
        await expect(toast(page, 'Tâche confiée à 2 personnes.')).toBeVisible()
        await page.getByRole('tab', { name: 'Confiées' }).click()
        await page.getByRole('button', { name: 'Suivi du groupe « Relire la procédure de bascule »' }).first().click()
        const g = page.getByRole('dialog', { name: 'Suivi : Relire la procédure de bascule' })
        await expect(g.getByText('0 sur 2')).toBeVisible()
        await g.getByRole('button', { name: 'Relancer Idriss Mahamat Saleh' }).click()
        await expect(g.getByText('Relance envoyée à Idriss Mahamat Saleh.')).toBeVisible()
        await capturer(page, `suivi-groupe-${suffixe}`)
        await g.getByRole('button', { name: 'Relancer Idriss Mahamat Saleh' }).click()
        await expect(g.getByText('Une relance a déjà été envoyée il y a moins de 24 heures.')).toBeVisible()
      })
    })

    test('Scénario : un membre du pôle réseau prend la tâche confiée au pôle par le chef du DES', async ({ page }) => {
      await demo(page, { profil: 'ROLE_POLE_RESEAU', theme })
      await page.goto(`${ADRESSES.departement}/des/taches`)
      await page.getByRole('tab', { name: 'À prendre' }).click()
      await page.getByRole('button', { name: 'Vérifier la supervision des liens du participant' }).click()
      const f = page.getByRole('dialog', { name: 'Vérifier la supervision des liens du participant' })
      await expect(f.getByText(/^Créée par Paul Mbarga Atangana/)).toBeVisible()
      await capturer(page, `a-prendre-${suffixe}`)
      await f.getByRole('button', { name: 'Prendre la tâche' }).click()
      await expect(toast(page, 'Tâche prise : vous en êtes le responsable.')).toBeVisible()
      await f.getByRole('button', { name: 'Fermer' }).last().click()
      // Reste à prendre l'étape de processus du pôle (D-87), réservée au profil requis.
      await expect(page.getByRole('button', { name: 'Vérifier la supervision des liens du participant' })).toHaveCount(0)
      await expect(page.getByText('Profil requis : Pôle réseau')).toBeVisible()
      await page.getByRole('tab', { name: 'Mes tâches' }).click()
      await expect(page.getByRole('button', { name: 'Vérifier la supervision des liens du participant' })).toBeVisible()
    })

    test('Scénario : le super administrateur accepte une demande, modifie la matrice et crée un profil', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)

      await test.step('Demandes à traiter : la demande de compte du chef du DES, acceptée avec la clé de sécurité', async () => {
        await naviguer(page, 'Demandes à traiter')
        await expect(page.getByRole('heading', { level: 1 })).toHaveText('Demandes à traiter')
        await page.getByRole('row').filter({ hasText: 'Nouveau compte : Ingrid Fouda Ngono' }).getByRole('button', { name: 'Création de compte' }).click()
        const fiche = page.getByRole('dialog', { name: 'Création de compte' })
        await expect(fiche.getByText('i.fouda', { exact: true })).toBeVisible()
        await capturer(page, `demande-a-traiter-${suffixe}`)
        await fiche.getByRole('button', { name: 'Accepter avec ma clé' }).click()
        await signerEtTerminer(page, 'Création validée par la direction')
        await expect(page.getByText('Demande acceptée : Nouveau compte : Ingrid Fouda Ngono.')).toBeVisible()
        await page.getByRole('row').filter({ hasText: 'Nadège Mvondo Assiga' }).getByRole('button', { name: 'Profils' }).click()
        await page.getByRole('dialog', { name: 'Profils' }).getByRole('button', { name: 'Refuser la demande' }).click()
        const r = page.getByRole('alertdialog')
        await r.getByLabel(/Motif/).fill('Profil non justifié par la fiche de poste')
        await r.getByRole('button', { name: 'Refuser la demande' }).click()
        await expect(page.getByText('Demande refusée : Nadège Mvondo Assiga.')).toBeVisible()
      })

      await test.step('Matrice des pouvoirs : portée par cellule, profils demandables, enregistrement signé', async () => {
        await naviguer(page, 'Matrice des pouvoirs')
        await expect(page.getByText('ROLE_RESPONSABLE_SERVICE')).toBeVisible()
        await capturer(page, `matrice-${suffixe}`)
        await page.getByRole('button', { name: 'Pouvoirs du profil Responsable de service' }).click()
        const d = page.getByRole('dialog', { name: 'Pouvoirs du profil Responsable de service' })
        await d.getByLabel(/^Modifier le rattachement/).selectOption('EQUIPE')
        await d.getByRole('group', { name: 'Profils demandables' }).getByLabel('Conducteur de projet').uncheck()
        await capturer(page, `matrice-ligne-${suffixe}`)
        await d.getByRole('button', { name: 'Continuer vers la signature' }).click()
        await signerEtTerminer(page, 'Revue annuelle des pouvoirs')
        await expect(page.getByText('Pouvoirs du profil Responsable de service enregistrés.')).toBeVisible()
      })

      await test.step('Profils et permissions : création d’un profil signée', async () => {
        await naviguer(page, 'Profils et permissions')
        await page.getByRole('button', { name: 'Nouveau profil' }).click()
        const d = page.getByRole('dialog', { name: 'Nouveau profil' })
        await d.getByLabel(/^Code/).fill('ROLE_ASTREINTE_NOC')
        await d.getByLabel(/^Description/).fill('Astreinte de supervision du NOC')
        // Arbre Module > Fonctionnalité > Droit (D-90) : un droit précis dans une fonctionnalité dépliée.
        await d.getByRole('button', { name: 'Droits de Liaisons et parcours' }).click()
        await d.getByRole('checkbox', { name: /\(HOM_LIAISON_READ\)/ }).check()
        await d.getByRole('checkbox', { name: 'Journal d’administration' }).check()
        await expect(d.getByText(/Profil privilégié/)).toBeVisible()
        await capturer(page, `nouveau-profil-${suffixe}`)
        await d.getByRole('checkbox', { name: 'Journal d’administration' }).uncheck()
        await d.getByRole('button', { name: 'Continuer vers la signature' }).click()
        await signerEtTerminer(page, 'Astreinte de nuit du NOC')
        await expect(page.getByText('Profil ROLE_ASTREINTE_NOC créé.')).toBeVisible()
        // Dernière ligne de la liste, sous la barre de pagination : geste au clavier.
        await page.getByRole('button', { name: 'Supprimer le profil ROLE_ASTREINTE_NOC' }).press('Enter')
        await signerEtTerminer(page, 'Profil créé par erreur')
        await expect(page.getByText('Profil ROLE_ASTREINTE_NOC supprimé.')).toBeVisible()
      })
    })
  })
}

test('Scénario : la console sert les mêmes écrans (demandes, matrice, organisation)', async ({ page }) => {
  await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme: 'light' })
  await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)
  await naviguer(page, 'Demandes à traiter')
  await expect(page.getByText('Nouveau compte : Ingrid Fouda Ngono')).toBeVisible()
  await naviguer(page, 'Matrice des pouvoirs')
  await expect(page.getByText('ROLE_CHEF_DES')).toBeVisible()
  await naviguer(page, 'Organisation')
  await page.getByRole('tree').getByText('Département Exploitation et Supervision').click()
  await expect(page.getByText('Christian Ndong Obiang')).toBeVisible()
})

test('Scénario : le chef du DES agit sur un subordonné direct et sur une unité dirigée, puis suit ses demandes', async ({ page }) => {
  await demo(page, { profil: 'ROLE_CHEF_DES', theme: 'light' })
  await page.goto(`${ADRESSES.departement}/des/equipe?vue=organisation`)
  const arbre = page.getByRole('tree', { name: 'Mes subordonnés' })
  const gestes = page.getByRole('group', { name: 'Gestes permis' })

  await test.step('Demander un profil : contrôles, puis demande déposée', async () => {
    await arbre.getByText('Gaston Ndzana Ebode').click()
    await gestes.getByRole('button', { name: 'Demander un profil' }).click()
    const d = page.getByRole('dialog', { name: 'Demander un profil' })
    await d.getByRole('button', { name: 'Envoyer la demande' }).click()
    await expect(d.getByText('Choisissez au moins un profil à ajouter ou à retirer.')).toBeVisible()
    await d.getByRole('group', { name: 'Profils à ajouter' }).getByLabel('Normes et Agréments').check()
    await d.getByRole('group', { name: 'Profils à retirer' }).getByLabel('Normes et Agréments').check()
    await d.getByLabel(/^Motif/).fill('Suivi des agréments')
    await d.getByRole('button', { name: 'Envoyer la demande' }).click()
    await expect(d.getByText('Un même profil ne peut être à la fois ajouté et retiré.')).toBeVisible()
    await d.getByRole('group', { name: 'Profils à retirer' }).getByLabel('Normes et Agréments').uncheck()
    await d.getByRole('button', { name: 'Envoyer la demande' }).click()
    await expect(toast(page, 'Demande adressée à l’administration.')).toBeVisible()
  })

  await test.step('Demander une désactivation, puis modifier le rattachement', async () => {
    await arbre.getByText('Gaston Ndzana Ebode').click()
    await gestes.getByRole('button', { name: 'Demander la désactivation' }).click()
    const d = page.getByRole('dialog', { name: 'Demander la désactivation' })
    await d.getByRole('button', { name: 'Envoyer la demande' }).click()
    await expect(d.getByText('Motif de 5 à 500 caractères.')).toBeVisible()
    await d.getByLabel(/^Motif/).fill('Départ en retraite au 31 décembre')
    await d.getByRole('button', { name: 'Envoyer la demande' }).click()
    await expect(d).toHaveCount(0)
    await arbre.getByText('Gaston Ndzana Ebode').click()
    await gestes.getByRole('button', { name: 'Modifier le rattachement' }).click()
    const r = page.getByRole('dialog', { name: 'Modifier le rattachement' })
    await r.getByLabel(/^Unité/).selectOption('DES')
    await r.getByRole('button', { name: 'Enregistrer le rattachement' }).click()
    await expect(toast(page, 'Rattachement de Gaston Ndzana Ebode modifié.')).toBeVisible()
  })

  await test.step('Unité dirigée : demander un compte dans l’unité, confier une tâche à l’unité', async () => {
    await page.getByRole('tree', { name: 'Unités dirigées' }).getByText('Pôle système').click()
    await page.getByRole('button', { name: 'Demander un compte dans l’unité' }).click()
    const d = page.getByRole('dialog', { name: 'Demander un compte' })
    await expect(d.getByLabel(/^Unité/)).toHaveValue('POLE_SYSTEME')
    await d.getByRole('button', { name: 'Envoyer la demande' }).click()
    await expect(d.getByText('Choisissez au moins un profil.')).toBeVisible()
    await d.getByRole('button', { name: 'Annuler' }).click()
    const abandon = page.getByRole('alertdialog')
    if (await abandon.count()) await abandon.getByRole('button').last().click()
    await page.getByRole('button', { name: 'Confier une tâche à l’unité' }).click()
    const t = page.getByRole('dialog', { name: 'Nouvelle tâche' })
    await expect(t.getByLabel(/^Unité/)).toHaveValue('POLE_SYSTEME')
    await t.getByLabel(/^Intitulé/).fill('Mettre à jour les serveurs de recette')
    await t.getByRole('button', { name: 'Créer la tâche' }).click()
    await expect(toast(page, 'Tâche confiée à l’unité « Pôle système ».')).toBeVisible()
  })

  await test.step('Mes demandes : filtre, fiche avec décision, annulation', async () => {
    await page.goto(`${ADRESSES.departement}/des/equipe/demandes?demande=DEM-0003`)
    const fiche = page.getByRole('dialog', { name: 'Désactivation ou réactivation' })
    await expect(fiche.getByText('Le départ n’est pas encore confirmé par les ressources humaines.')).toBeVisible()
    await fiche.getByRole('button', { name: 'Fermer' }).last().click()
    await page.getByRole('row').filter({ hasText: 'Ingrid Fouda Ngono' }).getByRole('button', { name: 'Création de compte' }).click()
    await page.getByRole('dialog', { name: 'Création de compte' }).getByRole('button', { name: 'Annuler la demande' }).click()
    await expect(toast(page, 'Demande annulée.')).toBeVisible()
    await page.getByLabel('État').selectOption('ANNULEE')
    await expect(page.getByText('Nouveau compte : Ingrid Fouda Ngono')).toBeVisible()
    await page.getByRole('tab', { name: 'Activité du jour' }).click()
    await expect(page.getByRole('button', { name: 'Jour précédent' })).toBeVisible()
  })
})

test('Scénario : l’administration désigne le responsable d’une unité et filtre les demandes', async ({ page }) => {
  await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme: 'light' })
  await page.goto(`${ADRESSES.departement}/des/administration/organisation`)
  const arbre = page.getByRole('tree')
  await arbre.getByText('Service Production informatique').click()
  await page.getByRole('button', { name: 'Modifier l’unité' }).click()
  const d = page.getByRole('dialog', { name: 'Modifier l’unité' })
  await d.getByLabel(/^Responsable de l’unité/).selectOption('u-ingenieur')
  await d.getByRole('button', { name: 'Enregistrer l’unité' }).click()
  await expect(page.getByText('Unité « Service Production informatique » modifiée.')).toBeVisible()
  await expect(page.getByText('Patrick Mbida Essomba')).toBeVisible()
  await page.goto(`${ADRESSES.departement}/des/administration/demandes`)
  await page.getByLabel('État').selectOption('')
  await expect(page.getByText('Bertrand Nguema Obiang')).toBeVisible()
  await page.getByRole('row').filter({ hasText: 'Bertrand Nguema Obiang' }).getByRole('button', { name: 'Profils' }).click()
  await expect(page.getByRole('dialog', { name: 'Profils' }).getByText('Conduite du projet de la Banque de Libreville.')).toBeVisible()
})
