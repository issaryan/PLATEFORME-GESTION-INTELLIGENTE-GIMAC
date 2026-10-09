import { mkdirSync, readFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { naviguer } from './outils'

// Fonctionnalité : organigramme ergonomique, affectation de personnes et téléchargement en PDF (D-102,
// docs/lot1/organigramme.md), services simulés de la console. Le super administrateur parcourt l'organigramme descendant
// (repli des branches, zoom, ajustement, recherche, plein écran), affecte une personne sans unité au pôle système depuis le
// menu de la carte, la retrouve dans le panneau de l'unité, puis télécharge l'organigramme en PDF (exemple de
// démonstration). Sur petit écran, l'organigramme redevient un arbre vertical. Captures en thème clair et sombre dans
// e2e/captures/d102 (ou $GIMAC_CAPTURES/d102).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd102') : resolve(ICI, '..', 'captures', 'd102')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const annonce = (page: Page) => page.locator('[role="status"][aria-live="polite"]').first()

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : organigramme, affectations et PDF, services simulés (${suffixe})`, () => {
    test('Scénario : le super administrateur parcourt l’organigramme, affecte une personne à un service et télécharge le PDF', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)
      await naviguer(page, 'Organigramme')
      const unite = (code: string) => page.locator(`[data-unite="${code}"]`)

      await test.step('Étant donné l’organigramme descendant : cartes typées, chef avec photo ou initiales, effectif', async () => {
        await expect(page.getByRole('list', { name: 'Unités' })).toBeVisible()
        await expect(unite('DES')).toContainText('Chef : Christian Ndong Obiang · 5 personnes')
        await expect(unite('DG')).toHaveAttribute('data-type', 'DIRECTION')
        // Les sous-unités sont sous leur parent (organigramme descendant), non pas en liste indentée.
        const dg = await unite('DG').boundingBox()
        const des = await unite('DES').boundingBox()
        expect(des!.y).toBeGreaterThan(dg!.y + dg!.height)
        await capturer(page, `organigramme-vue-ensemble-${suffixe}`)
      })

      await test.step('Quand il replie une branche, réduit puis ajuste le zoom', async () => {
        await page.getByRole('button', { name: 'Replier Service Production informatique' }).click()
        await expect(unite('POLE_RESEAU')).toHaveCount(0)
        await expect(page.getByRole('button', { name: 'Déplier Service Production informatique' })).toHaveText('2')
        await page.getByRole('button', { name: 'Tout replier' }).click()
        await expect(unite('DES')).toHaveCount(0)
        await page.getByRole('button', { name: 'Tout déplier' }).click()
        await expect(unite('POLE_SYSTEME')).toBeVisible()
        await page.getByRole('button', { name: 'Réduire' }).click()
        await expect(annonce(page)).toHaveText(/Zoom : \d+ %/)
        await page.getByRole('button', { name: 'Ajuster à l’écran' }).click()
        const vue = page.getByRole('region', { name: /faites glisser le fond/ })
        const largeurVue = (await vue.boundingBox())!.width
        const largeurDg = (await unite('DG').boundingBox())!.width
        expect(largeurDg).toBeLessThan(largeurVue)
        await capturer(page, `organigramme-zoom-ajuste-${suffixe}`)
      })

      await test.step('Et il cherche une personne : son unité est mise en évidence, le panneau l’affiche', async () => {
        const champ = page.getByRole('searchbox', { name: 'Rechercher une personne ou une unité' })
        await champ.fill('Biyong')
        await expect(page.getByRole('list', { name: 'Résultats de la recherche' })).toBeVisible()
        await capturer(page, `organigramme-recherche-${suffixe}`)
        await champ.press('Enter')
        await expect(unite('SNA')).toHaveClass(/gm-org-trouve/)
        await expect(annonce(page)).toHaveText('Rachel Biyong Mengue : Service Normes et Agréments.')
        await expect(page.getByRole('group', { name: 'Membres de Service Normes et Agréments' }).locator('[data-compte="u-sna"]')).toHaveClass(/gm-org-trouve/)
      })

      await test.step('Et il affecte une personne sans unité au pôle système depuis le menu de la carte, avec un motif', async () => {
        await page.getByRole('button', { name: 'Actions sur l’unité Pôle système' }).click()
        await page.getByRole('menu', { name: 'Actions sur l’unité Pôle système' }).getByRole('menuitem', { name: 'Affecter des personnes' }).click()
        const d = page.getByRole('dialog', { name: 'Affecter des personnes' })
        await expect(d.getByText('n+1 proposé : Christian Ndong Obiang (chef de Département Exploitation et Supervision).')).toBeVisible()
        await d.getByRole('searchbox', { name: /Rechercher dans l’annuaire/ }).fill('hamadou')
        await d.getByRole('checkbox', { name: /Bachir Hamadou Ali/ }).check()
        await d.getByLabel(/Motif/).fill('Renfort du pôle système pour l’exploitation')
        await capturer(page, `organigramme-affecter-des-personnes-${suffixe}`)
        await d.getByRole('button', { name: 'Affecter 1 personne' }).click()
        await expect(page.getByText('1 personne affectée à Pôle système.')).toBeVisible()
        const membres = page.getByRole('group', { name: 'Membres de Pôle système' })
        await expect(membres.getByText('Bachir Hamadou Ali')).toBeVisible()
        await expect(membres.getByText('n+1 : Christian Ndong Obiang')).toBeVisible()
        await expect(unite('POLE_SYSTEME')).toContainText('1 personne')
        await expect(page.getByRole('heading', { name: 'Comptes sans unité' })).toBeVisible()
        await capturer(page, `organigramme-panneau-membres-${suffixe}`)
      })

      await test.step('Et il passe en plein écran, puis le quitte par Échap', async () => {
        await page.getByRole('button', { name: 'Plein écran' }).click()
        await expect(page.locator('.gm-org-plein-ecran')).toBeVisible()
        await capturer(page, `organigramme-plein-ecran-${suffixe}`)
        await page.keyboard.press('Escape')
        await expect(page.locator('.gm-org-plein-ecran')).toHaveCount(0)
      })

      await test.step('Alors il télécharge l’organigramme en PDF (exemple de démonstration)', async () => {
        await page.getByRole('button', { name: 'Télécharger en PDF' }).click()
        const d = page.getByRole('dialog', { name: 'Télécharger l’organigramme en PDF' })
        await expect(d.getByText(/exemple de démonstration/)).toBeVisible()
        await d.getByLabel(/Annexe des membres/).check()
        await capturer(page, `organigramme-telecharger-pdf-${suffixe}`)
        const [fichier] = await Promise.all([page.waitForEvent('download'), d.getByRole('button', { name: 'Télécharger' }).click()])
        expect(fichier.suggestedFilename()).toMatch(/^organigramme-moabi-\d{4}-\d{2}-\d{2}\.pdf$/)
        const contenu = readFileSync((await fichier.path())!)
        expect(contenu.subarray(0, 5).toString('latin1')).toBe('%PDF-')
        expect(contenu.length).toBeGreaterThan(50_000)
        await expect(page.getByText(/Organigramme téléchargé : organigramme-moabi-/)).toBeVisible()
      })
    })

    test('Scénario : sur un petit écran, l’organigramme devient un arbre vertical indenté', async ({ page }) => {
      await page.setViewportSize({ width: 390, height: 844 })
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.console}/console/organigramme`)
      const dg = page.locator('[data-unite="DG"]')
      const des = page.locator('[data-unite="DES"]')
      await expect(des).toBeVisible()
      const a = (await dg.boundingBox())!
      const b = (await des.boundingBox())!
      // Arbre vertical : la sous-unité est sous son parent, décalée vers la droite, et tient dans la largeur de l'écran.
      expect(b.y).toBeGreaterThan(a.y)
      expect(b.x).toBeGreaterThan(a.x)
      expect(b.x + b.width).toBeLessThanOrEqual(390)
      await capturer(page, `organigramme-petit-ecran-${suffixe}`)
    })
  })
}

// Gestes complémentaires (D-102) : retrait d'un membre motivé, recherche sans résultat, agrandissement, PDF d'une
// branche en A4 portrait sans annexe.
test.describe('Fonctionnalité : organigramme, gestes complémentaires, services simulés', () => {
  test('Scénario : le super administrateur retire un membre, cherche sans résultat, agrandit et télécharge une branche', async ({ page }) => {
    await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme: 'light' })
    await page.goto(`${ADRESSES.console}/console/organigramme`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Organigramme')

    await test.step('Quand il retire un membre du service Normes et Agréments, motif à l’appui', async () => {
      await page.locator('[data-unite="SNA"]').click()
      const membres = page.getByRole('group', { name: 'Membres de Service Normes et Agréments' })
      await expect(membres).toBeVisible()
      const retirer = membres.getByRole('button', { name: /^Retirer .+ de l’unité$/ }).first()
      const nom = ((await retirer.getAttribute('aria-label')) ?? '').replace(/^Retirer /, '').replace(/ de l’unité$/, '')
      await retirer.click()
      const d = page.getByRole('alertdialog', { name: 'Retirer de l’unité' })
      await d.getByLabel(/Motif/).fill('Départ vers une autre direction du GIMAC')
      await d.getByRole('button', { name: 'Retirer de l’unité' }).click()
      await expect(page.getByText('Personne retirée de l’unité.')).toBeVisible()
      await expect(membres.getByText(nom, { exact: true })).toHaveCount(0)
    })

    await test.step('Et il cherche une personne inconnue, puis efface la recherche par Échap', async () => {
      const champ = page.getByRole('searchbox', { name: 'Rechercher une personne ou une unité' })
      await champ.fill('zzzz')
      await expect(page.getByText('Aucune personne ni unité ne correspond.')).toBeVisible()
      await champ.press('Escape')
      await expect(champ).toHaveValue('')
    })

    await test.step('Et il agrandit l’organigramme', async () => {
      const zoom = page.locator('output.gm-org-zoom')
      const avant = await zoom.textContent()
      await page.getByRole('button', { name: 'Agrandir' }).click()
      await expect(zoom).not.toHaveText(avant ?? '')
    })

    // Geste choisi dans une fenêtre (cible), puis confirmé avec motif.
    const gesteAvecMotif = async (titre: string, cible: string, fait: string) => {
      const choix = page.getByRole('dialog', { name: new RegExp(titre.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')) })
      await choix.getByRole('combobox', { name: cible, exact: true }).selectOption({ index: 1 })
      await choix.getByRole('button', { name: 'Continuer' }).click()
      const confirmer = page.getByRole('alertdialog', { name: titre })
      await confirmer.getByLabel(/Motif/).fill('Réorganisation validée par la direction générale')
      await confirmer.getByRole('button', { name: titre }).click()
      await expect(page.getByText(fait)).toBeVisible()
    }

    await test.step('Et il rattache un membre du DES à une autre unité', async () => {
      await page.locator('[data-unite="DES"]').click()
      await page.getByRole('group', { name: 'Membres de Département Exploitation et Supervision' }).getByRole('button', { name: /^Changer l’unité/ }).first().click()
      await gesteAvecMotif('Rattacher à l’unité', 'Unité', 'Compte rattaché à l’unité.')
    })

    await test.step('Et il désigne le chef du pôle système', async () => {
      await page.locator('[data-unite="POLE_SYSTEME"]').click()
      await page.getByRole('button', { name: 'Désigner le chef' }).click()
      await gesteAvecMotif('Désigner le chef d’unité', 'Chef d’unité', 'Chef d’unité désigné.')
    })

    await test.step('Et il change le n+1 d’un compte sans unité', async () => {
      await page.getByRole('button', { name: /^Changer le n\+1 de / }).first().click()
      await gesteAvecMotif('Changer le n+1', 'Nouveau n+1', 'Nouveau n+1 enregistré.')
    })

    await test.step('Et il crée une cellule sous le pôle système, la déplace, la désactive puis la réactive', async () => {
      await page.locator('[data-unite="POLE_SYSTEME"]').click()
      await page.getByRole('button', { name: 'Sous-unité', exact: true }).click()
      const d = page.getByRole('dialog', { name: /Nouvelle unité/ })
      await d.getByRole('textbox', { name: 'Code' }).fill('CELLULE_SAUVEGARDE')
      await d.getByRole('combobox', { name: 'Type', exact: true }).selectOption('CELLULE')
      await d.getByRole('textbox', { name: 'Libellé' }).fill('Cellule sauvegarde')
      await d.getByRole('button', { name: 'Créer l’unité' }).click()
      const cellule = page.locator('[data-unite="CELLULE_SAUVEGARDE"]')
      await expect(cellule).toBeVisible()
      await cellule.click()
      await page.getByRole('button', { name: 'Déplacer', exact: true }).click()
      await gesteAvecMotif('Déplacer l’unité', 'Nouvelle unité parente', 'Unité déplacée.')
      for (const etat of ['Désactiver l’unité', 'Réactiver l’unité']) {
        await page.getByRole('button', { name: etat, exact: true }).click()
        const confirmer = page.getByRole('alertdialog')
        await confirmer.getByLabel(/Motif/).fill('Réorganisation validée par la direction générale')
        await confirmer.getByRole('button', { name: etat }).click()
        await expect(confirmer).toHaveCount(0)
      }
      await expect(page.getByRole('button', { name: 'Désactiver l’unité', exact: true })).toBeVisible()
    })

    await test.step('Et il affecte deux personnes au service Normes et Agréments, n+1 choisi puis aucun', async () => {
      await page.locator('[data-unite="SNA"]').click()
      await page.getByRole('button', { name: 'Affecter des personnes', exact: true }).click()
      const d = page.getByRole('dialog', { name: /Affecter des personnes/ })
      const recherche = d.getByRole('searchbox', { name: 'Rechercher dans l’annuaire (nom ou identifiant)' })
      await recherche.fill('zzzz')
      await expect(d.getByText('Aucun compte ne correspond.')).toBeVisible()
      await recherche.fill('')
      const cases = d.getByRole('group', { name: 'Comptes internes actifs' }).getByRole('checkbox', { disabled: false })
      await cases.nth(0).check()
      await cases.nth(1).check()
      await expect(d.getByText('2 personnes choisies')).toBeVisible()
      await d.getByRole('radio', { name: 'Choisir un n+1' }).check()
      await d.getByRole('combobox', { name: 'n+1', exact: true }).selectOption({ index: 1 })
      await d.getByRole('radio', { name: 'Aucun n+1' }).check()
      await d.getByLabel(/Motif/).fill('Renfort du service pour la campagne de certification')
      await d.getByRole('button', { name: 'Affecter 2 personnes' }).click()
      await expect(d).toHaveCount(0)
    })

    await test.step('Alors il télécharge la branche du DES en A4 portrait, sans annexe', async () => {
      await page.getByRole('button', { name: 'Télécharger en PDF' }).click()
      const d = page.getByRole('dialog', { name: 'Télécharger l’organigramme en PDF' })
      await d.getByLabel('Périmètre').selectOption({ label: 'Département Exploitation et Supervision' })
      await d.getByLabel('A4').check()
      await d.getByLabel('Portrait').check()
      const [fichier] = await Promise.all([page.waitForEvent('download'), d.getByRole('button', { name: 'Télécharger' }).click()])
      expect(fichier.suggestedFilename()).toMatch(/^organigramme-moabi-\d{4}-\d{2}-\d{2}\.pdf$/)
    })
  })
})
