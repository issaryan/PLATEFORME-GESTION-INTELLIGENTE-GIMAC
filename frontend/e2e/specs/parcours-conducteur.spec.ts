import { ADRESSES, demo, expect, test } from '../fixtures'
import { carte, cheminDossier, confirmerAction, joindre, monTravail, naviguer, signerCle, tiroir } from './outils'

// Fonctionnalité : le conducteur de projet conduit les liaisons qui lui sont confiées (D-56 à D-59) :
// « Mes projets », livrables de phase, preuves avec code de contrôle (cérémonie de clés C1), validation
// signée, attribution des qualités, filtres des opérations, et établissement du procès-verbal de
// certification une fois la couverture complète constatée (verrou 1). Services simulés.

/**
 * DDR-05 : une preuve se dépose par le responsable (R) de l'opération ou le conducteur désigné de la liaison, et se valide par
 * le validateur (V) désigné. Les liaisons LIA-2026-012 et LIA-2026-005 sont confiées à un autre conducteur : le chef de la
 * DDRM les confie d'abord, motif à l'appui, au conducteur de la démonstration ; la preuve est déposée par le titulaire de la
 * fonction responsable, puis le conducteur valide. Démonstration continue (D-98) : l'acteur change par la session de
 * démonstration, sur le même état.
 */
async function acteur(page: import('@playwright/test').Page, role: string, adresse: string) {
  await page.evaluate((r) => sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: '', role: r })), role)
  await page.goto(adresse)
}
const continu = (page: import('@playwright/test').Page) => page.addInitScript(() => localStorage.setItem('gm.demoContinue', '1'))
/** Redésignation motivée du conducteur de la liaison (D-58) par le chef de la DDRM : le conducteur de la démonstration devient V. */
async function confier(page: import('@playwright/test').Page, liaison: string) {
  await acteur(page, 'ROLE_CHEF_DDRM', `${ADRESSES.departement}${cheminDossier(liaison)}`)
  await page.getByRole('button', { name: 'Redésigner le conducteur' }).click()
  const d = page.getByRole('dialog')
  await d.getByLabel(/Conducteur désigné/).selectOption('U-COND-1')
  await d.getByLabel(/Motif de la redésignation/).fill('Reprise de la liaison dans le portefeuille du conducteur')
  await d.getByRole('button', { name: 'Redésigner le conducteur' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
}

test.describe('Fonctionnalité : conduite de projet de certification', () => {
  test.beforeEach(async ({ page }) => {
    await demo(page, { profil: 'ROLE_CONDUCTEUR_PROJET', theme: 'light' })
  })

  test('Scénario : établir le procès-verbal de certification après le constat de couverture', async ({ page }) => {
    await test.step('Étant donné le conducteur et son procès-verbal à établir', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await expect(monTravail(page).locator('[aria-label="Mes compteurs"]').getByText('Procès-verbaux à établir')).toBeVisible()
      const aEtablir = carte(page, 'Procès-verbal à établir (1)')
      await expect(aEtablir.getByText('LIA-2026-025')).toBeVisible()
      await aEtablir.getByRole('link', { name: 'Établir le procès-verbal' }).click()
    })
    await test.step('Et la couverture complète constatée par Normes et Agréments', async () => {
      await expect(page).toHaveURL(/\/des\/parcours\/LIA-2026-025\?onglet=decision/)
      await expect(page.getByText(/Complète : 3 services sur 3/)).toBeVisible()
      await expect(page.getByRole('button', { name: 'Signer le procès-verbal pour le GIMAC' })).toHaveCount(0)
    })
    await test.step('Quand il établit le procès-verbal', async () => {
      await page.getByRole('button', { name: 'Établir le procès-verbal' }).click()
      await expect(page.getByRole('alertdialog').getByText(/figé avec son empreinte/)).toBeVisible()
      await confirmerAction(page, 'Établir le procès-verbal')
    })
    await test.step('Alors il est figé, établi par lui, et attend les deux signatures', async () => {
      const pv = carte(page, 'Verrou 1 : procès-verbal de certification')
      await expect(pv.getByText('Établi, à signer').first()).toBeVisible()
      await expect(pv.getByText('Aïcha Mahamat Nour')).toBeVisible()
      await expect(pv.getByText('Attendue du chef du DES')).toBeVisible()
      await expect(page.getByText('Établir le procès-verbal : effectué.')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Établir le procès-verbal' })).toHaveCount(0)
    })
  })

  test('Scénario : déposer le compte rendu de lancement d’un de ses projets', async ({ page }) => {
    await test.step('Étant donné ses projets, depuis la navigation « Mes projets »', async () => {
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await naviguer(page, 'Mes projets')
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mes projets')
      await page.getByRole('link', { name: 'LIA-2026-041' }).click()
      await page.getByRole('tab', { name: 'Opérations' }).click()
    })
    await test.step('Quand il ouvre l’opération 7 et dépose le livrable sans fichier, puis avec', async () => {
      await page.getByRole('button', { name: /^Ouvrir l'opération 7 : Réunion de lancement/ }).click()
      const t = tiroir(page, '7')
      await t.getByRole('button', { name: 'Déposer le livrable' }).click()
      await expect(t.getByText('Choisissez le fichier de preuve.')).toBeVisible()
      await joindre(t, 'cr-lancement.pdf')
      await t.getByLabel(/Commentaire/).fill('Compte rendu relu par les participants')
      await t.getByRole('button', { name: 'Déposer le livrable' }).click()
      await expect(page.getByRole('alertdialog').getByText('cr-lancement.pdf')).toBeVisible()
      await confirmerAction(page, 'Déposer le livrable')
    })
    await test.step('Alors l’opération attend le visa du conducteur configuré au référentiel, puis se clôt (recette HOM-05)', async () => {
      const t = tiroir(page, '7')
      await expect(t.getByText(/ne se clôt qu'après ses signatures/)).toBeVisible()
      await t.getByRole('button', { name: /^Signer/ }).first().click()
      await signerCle(page)
      await expect(t.getByText(/Opération close le/)).toBeVisible()
      await t.getByRole('tab', { name: 'Preuve' }).click()
      await expect(t.getByText('cr-lancement.pdf')).toBeVisible()
      await expect(t.getByText('Empreinte SHA-256')).toBeVisible()
    })
  })

  test('Scénario : cérémonie de clés C1 avec ses codes de contrôle, puis validation signée', async ({ page }) => {
    // Garde de non-régression : la date de tenue saisie au format du champ (JJ/MM/AAAA) est envoyée en ISO.
    await continu(page)
    const c1 = `${ADRESSES.departement}${cheminDossier('LIA-2026-012', 'operations', 'C1')}`
    await page.goto(c1)
    // DDR-05 : la cérémonie revient à la sécurité des SI (R) ; le conducteur désigné de la liaison (V) valide.
    await confier(page, 'LIA-2026-012')
    await acteur(page, 'ROLE_RSSI', c1)
    const t = tiroir(page, 'C1')
    await test.step('Étant donné la cérémonie de clés de la liaison LIA-2026-012', async () => {
      await expect(t.getByText('Cérémonie de clés : clé maîtresse de zone en trois composantes')).toBeVisible()
      await expect(t.getByText(/jamais la valeur d'une clé ou d'une composante/)).toBeVisible()
    })
    await test.step('Quand il soumet sans la tenue ni les porteurs, puis saisit une valeur trop longue', async () => {
      await t.getByRole('button', { name: 'Déposer la preuve' }).click()
      await expect(t.getByText('La date de tenue est exigée.')).toBeVisible()
      await expect(t.getByText('Chaque composante a un porteur.')).toHaveCount(3)
      const kcv1 = t.getByLabel(/Code de contrôle de la composante 1/)
      await kcv1.fill('0123456789ABCDEF0123456789ABCDEF')
      await expect(kcv1).toHaveValue('')
      await expect(t.getByText(/Saisie refusée et effacée/)).toBeVisible()
    })
    await test.step('Et qu’il complète la cérémonie : date, trois porteurs et leurs codes de contrôle', async () => {
      await joindre(t, 'pv-ceremonie-C1.pdf')
      await t.getByLabel(/Date de tenue/).fill('28/09/2026')
      for (const n of [1, 2, 3]) {
        await t.getByLabel(new RegExp(`Porteur de la composante ${n}`)).fill(`Dépositaire ${n}`)
        await t.getByLabel(new RegExp(`Code de contrôle de la composante ${n}`)).fill(`a1 b2 c${n}`)
      }
      await t.getByRole('button', { name: 'Déposer la preuve' }).click()
      const conf = page.getByRole('alertdialog')
      await expect(conf.getByText('Dépositaire 3 · A1B2C3')).toBeVisible()
      await confirmerAction(page, 'Déposer la preuve')
    })
    await test.step('Alors la preuve attend la validation ; le conducteur la valide avec sa clé de sécurité', async () => {
      await expect(t.getByText('À valider').first()).toBeVisible()
      await acteur(page, 'ROLE_CONDUCTEUR_PROJET', c1)
      await expect(t.getByText('Dépositaire 1 · A1B2C1')).toBeVisible()
      await t.getByRole('button', { name: 'Valider l\'opération' }).click()
      await signerCle(page)
      await expect(t.getByText(/Opération close le/)).toBeVisible()
    })
    await test.step('Et la preuve montre la cérémonie, sans aucune composante', async () => {
      await t.getByRole('tab', { name: 'Preuve' }).click()
      await expect(t.getByText('Cérémonie de clés', { exact: true })).toBeVisible()
      await expect(t.getByText('Composante 3')).toBeVisible()
      await expect(t.getByText(/elle ne porte aucune composante/)).toBeVisible()
    })
  })

  test('Scénario : filtrer les opérations d’une liaison et attribuer une qualité, motif à l’appui', async ({ page }) => {
    await continu(page)
    const ops = `${ADRESSES.departement}${cheminDossier('LIA-2026-005', 'operations')}`
    await page.goto(ops)
    await confier(page, 'LIA-2026-005')
    await acteur(page, 'ROLE_CONDUCTEUR_PROJET', ops)
    await test.step('Étant donné les opérations de LIA-2026-005 et l’alerte permanente de l’opération 28', async () => {
      await expect(page.getByRole('heading', { name: 'Mise en production', exact: true })).toBeVisible()
      await expect(page.getByText('Mise à jour du script de dépôt : à ne pas omettre')).toBeVisible()
      await expect(page.getByRole('heading', { name: 'Branche du type de projet · Switch interbancaire' })).toBeVisible()
    })
    await test.step('Quand il filtre, les compteurs restent ceux de la liaison entière', async () => {
      await page.getByLabel('Afficher').selectOption('a-faire')
      await expect(page.getByText(/Opérations affichées : 2 sur \d+/)).toBeVisible()
      await page.getByLabel('Afficher').selectOption('retard')
      await page.getByLabel('Afficher').selectOption('a-valider')
      await page.getByLabel('Afficher').selectOption('')
      await page.getByLabel('Étape', { exact: true }).selectOption('INTEGRATION')
      await expect(page.getByRole('heading', { name: 'Mise en production', exact: true })).toHaveCount(0)
      await page.getByRole('switch', { name: 'Ma fonction seulement' }).click()
      await page.getByLabel('Rechercher (rang ou libellé)').fill('zzz')
      await expect(page.getByText('Aucune opération ne répond à ces filtres.')).toBeVisible()
      await page.getByRole('button', { name: 'Effacer les filtres' }).click()
      await expect(page.getByText('Aucune opération ne répond à ces filtres.')).toHaveCount(0)
    })
    await test.step('Et une étape close se déplie puis se replie', async () => {
      await expect(page.getByText('Arrêt du périmètre des services')).toHaveCount(0)
      await page.getByRole('button', { name: 'Afficher les 3 opérations' }).first().click()
      await expect(page.getByText('Arrêt du périmètre des services')).toBeVisible()
      await page.getByRole('button', { name: 'Replier' }).first().click()
      await expect(page.getByText('Arrêt du périmètre des services')).toHaveCount(0)
    })
    await test.step('Quand il ouvre l’opération 28 depuis l’alerte, puis l’opération 29 (secret)', async () => {
      await page.getByRole('button', { name: 'Ouvrir l\'opération 28', exact: true }).click()
      await expect(tiroir(page, '28').getByText('Mise à jour du script de dépôt des fichiers', { exact: true })).toBeVisible()
      await tiroir(page, '28').getByRole('button', { name: 'Fermer' }).click()
      await page.getByRole('button', { name: /^Ouvrir l'opération 29 :/ }).click()
      await expect(tiroir(page, '29').getByText('Aucun secret dans le dossier')).toBeVisible()
      await expect(tiroir(page, '29').getByLabel(/Code de contrôle/)).toHaveCount(0)
    })
    await test.step('Et qu’il attribue la qualité de validateur à un ingénieur Normes et Agréments', async () => {
      const t = tiroir(page, '29')
      await t.getByRole('button', { name: 'Attribuer' }).click()
      await expect(t.getByText('Choisissez l\'intervenant.')).toBeVisible()
      await t.getByLabel(/^Qualité/).selectOption('V')
      await t.getByLabel(/^Fonction/).selectOption('SNA')
      await expect(t.getByLabel(/^Intervenant/).locator('option', { hasText: 'Rachel Biyong Mengue' })).toHaveCount(1)
      await t.getByLabel(/^Intervenant/).selectOption('U-SNA-1')
      await t.getByRole('button', { name: 'Attribuer' }).click()
      await confirmerAction(page, 'Attribuer', 'Relecture croisée des essais')
    })
    await test.step('Alors le validateur figure parmi les intervenants de l’opération', async () => {
      const t = tiroir(page, '29')
      await t.getByRole('tab', { name: 'Intervenants' }).click()
      await expect(t.getByText('Rachel Biyong Mengue · Normes et Agréments')).toBeVisible()
      await t.getByRole('button', { name: 'Fermer' }).click()
    })
    await test.step('Quand le pôle système (R) dépose la trace de l’opération 28, puis le conducteur la valide avec sa clé de sécurité', async () => {
      await acteur(page, 'ROLE_POLE_SYSTEME', ops)
      await page.getByRole('button', { name: 'Ouvrir l\'opération 28', exact: true }).click()
      let t = tiroir(page, '28')
      await t.getByRole('button', { name: 'Déposer la preuve' }).click()
      await expect(t.getByText('Choisissez le fichier de preuve.')).toBeVisible()
      await joindre(t, 'trace-script-depot.png', 'trace', 'image/png')
      await t.getByRole('button', { name: 'Déposer la preuve' }).click()
      await confirmerAction(page, 'Déposer la preuve')
      await expect(t.getByText('trace-script-depot.png')).toBeVisible()
      await acteur(page, 'ROLE_CONDUCTEUR_PROJET', ops)
      await page.getByRole('button', { name: 'Ouvrir l\'opération 28', exact: true }).click()
      t = tiroir(page, '28')
      await t.getByRole('button', { name: 'Valider l\'opération' }).click()
      await signerCle(page)
    })
    await test.step('Alors l’opération 28 est close et l’alerte d’oubli reste signalée', async () => {
      const t = tiroir(page, '28')
      await expect(t.getByText(/Opération close le/)).toBeVisible()
      await expect(t.getByText('Opération faite : elle reste signalée')).toBeVisible()
      await expect(page.getByText('Mise à jour du script de dépôt : à ne pas omettre')).toHaveCount(0)
    })
  })
})
