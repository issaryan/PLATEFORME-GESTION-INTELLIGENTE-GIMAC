import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Locator, Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { naviguer, signerEtTerminer } from './outils'

// Fonctionnalité : profils, rôles par fonctionnalité et organigramme (D-86 et D-90, docs/lot1/profils-organisation.md
// et docs/lot1/roles-fonctionnalites.md), services simulés de la console. Le super administrateur renomme un profil
// livré, compose un profil par fonctionnalités (arbre, aperçu de la barre latérale), compare deux profils, lit les
// colonnes « processus » de la matrice, puis réorganise l'organigramme par glisser-déposer et lit les pouvoirs
// effectifs d'un compte. Captures en thème clair et sombre dans e2e/captures/d86-d90 (ou $GIMAC_CAPTURES/d86-d90).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd86-d90') : resolve(ICI, '..', 'captures', 'd86-d90')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const toast = (page: Page, texte: string | RegExp) => page.getByRole('status').filter({ hasText: texte })

/** Glisser-déposer HTML5 décomposé : saisie, survol de la cible (capture possible), puis dépôt éventuel. */
async function saisir(page: Page, source: Locator, cible: Locator) {
  const dt = await page.evaluateHandle(() => new DataTransfer())
  await source.dispatchEvent('dragstart', { dataTransfer: dt })
  await cible.dispatchEvent('dragenter', { dataTransfer: dt })
  await cible.dispatchEvent('dragover', { dataTransfer: dt })
  return async () => {
    await cible.dispatchEvent('drop', { dataTransfer: dt })
    await source.dispatchEvent('dragend', { dataTransfer: dt })
  }
}

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : profils et organigramme, services simulés (${suffixe})`, () => {
    test('Scénario : le super administrateur présente un profil, le compose par fonctionnalités et compare deux profils', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)

      await test.step('Étant donné la liste des profils : libellé, description et dernière modification du serveur', async () => {
        await naviguer(page, 'Profils et permissions')
        const ligne = page.getByRole('row').filter({ hasText: 'ROLE_POLE_RESEAU' })
        await expect(ligne.getByText('Pôle réseau', { exact: true })).toBeVisible()
        await expect(ligne.getByText('non modifié')).toBeVisible()
        await expect(ligne.getByRole('button', { name: /Supprimer/ })).toHaveCount(0)
        await capturer(page, `profils-liste-${suffixe}`)
      })

      await test.step('Quand il modifie le libellé et la description (motif et clé de sécurité)', async () => {
        await page.getByRole('button', { name: 'Modifier le libellé et la description du profil Pôle réseau' }).click()
        const d = page.getByRole('dialog', { name: 'Modifier le profil' })
        await d.getByLabel(/^Libellé/).fill('Pôle réseau et télécoms')
        await d.getByLabel(/^Description/).fill('Liens, routage, pare-feu et télécoms du GIMAC.')
        await capturer(page, `profils-modale-modification-${suffixe}`)
        await d.getByRole('button', { name: 'Continuer vers la signature' }).click()
        await signerEtTerminer(page, 'Évolution du périmètre du pôle réseau')
        await expect(toast(page, 'Profil « Pôle réseau et télécoms » enregistré.')).toBeVisible()
        await expect(page.getByRole('row').filter({ hasText: 'ROLE_POLE_RESEAU' }).getByText('Serge Mouelle Kombi')).toBeVisible()
      })

      await test.step('Alors la fiche du profil présente l’arbre des fonctionnalités et l’aperçu de la barre latérale', async () => {
        await page.getByRole('row').filter({ hasText: 'ROLE_SNA' }).getByRole('button', { name: 'Modifier la composition' }).click()
        const d = page.getByRole('dialog', { name: 'Profil Normes et Agréments' })
        const apercu = d.getByRole('complementary', { name: 'Aperçu de la barre latérale' })
        await d.getByRole('checkbox', { name: 'Mon équipe' }).click()
        await expect(apercu.getByText('(ajoutée)')).toBeVisible()
        await d.getByRole('button', { name: 'Droits de Liaisons et parcours' }).click()
        await d.getByRole('checkbox', { name: /\(HOM_REPLAY_ADMIN\)/ }).check()
        await capturer(page, `roles-fonctionnalites-arbre-apercu-${suffixe}`)
        await d.getByRole('tab', { name: 'Droits' }).click()
        await expect(d.getByLabel('HOM_REPLAY_ADMIN')).toBeChecked()
        await d.getByRole('tab', { name: 'Pouvoirs' }).click()
        await expect(d.getByRole('link', { name: 'Ouvrir dans la matrice des pouvoirs' }).or(d.getByRole('button', { name: 'Ouvrir dans la matrice des pouvoirs' }))).toBeVisible()
        await d.getByRole('tab', { name: 'Organisation' }).click()
        await expect(d.getByText('Rachel Biyong Mengue')).toBeVisible()
        await d.getByRole('button', { name: 'Continuer vers la signature' }).click()
        await signerEtTerminer(page, 'Suivi de l’équipe confié au service SNA')
        await expect(page.getByText(/La composition du profil Normes et Agréments a été enregistrée/)).toBeVisible()
      })

      await test.step('Et la comparaison de deux profils présente les droits qui diffèrent', async () => {
        await page.getByRole('button', { name: 'Comparer deux profils' }).click()
        const d = page.getByRole('dialog', { name: 'Comparer deux profils' })
        await d.getByLabel('Premier profil').selectOption('ROLE_SNA')
        await d.getByLabel('Second profil').selectOption('ROLE_CHEF_DES')
        await d.getByLabel('Seulement les différences').check()
        await expect(d.getByText(/droits? diff[èe]re/)).toBeVisible()
        await capturer(page, `roles-fonctionnalites-comparaison-${suffixe}`)
        await d.getByRole('button', { name: 'Fermer' }).last().click()
      })

      await test.step('Et la matrice des pouvoirs porte les colonnes « processus » et le libellé de chaque ligne', async () => {
        await naviguer(page, 'Matrice des pouvoirs')
        await expect(page.getByRole('columnheader', { name: /Processus : définir/ })).toBeVisible()
        await expect(page.getByRole('columnheader', { name: /Processus : lancer/ })).toBeVisible()
        await expect(page.getByRole('row').filter({ hasText: 'ROLE_CHEF_DES' }).getByText('Chef du DES')).toBeVisible()
        await capturer(page, `matrice-colonnes-processus-${suffixe}`)
      })
    })

    test('Scénario : le super administrateur réorganise l’organigramme par glisser-déposer et lit les pouvoirs d’un compte', async ({ page }) => {
      await demo(page, { profil: 'ROLE_SUPER_ADMIN', theme })
      await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)
      await naviguer(page, 'Organigramme')
      const unite = (code: string) => page.locator(`[data-unite="${code}"]`)
      const compte = (id: string) => page.locator(`[data-compte="${id}"]`)

      await test.step('Étant donné l’arbre des unités avec chef et effectif', async () => {
        await expect(unite('DES')).toContainText('Chef : Christian Ndong Obiang')
        await unite('SNA').click()
        await expect(page.getByRole('group', { name: 'Membres de Service Normes et Agréments' })).toBeVisible()
        await capturer(page, `organigramme-arbre-${suffixe}`)
      })

      await test.step('Quand il glisse le pôle système sur le service SNA puis confirme avec un motif', async () => {
        const deposer = await saisir(page, unite('POLE_SYSTEME'), unite('SNA'))
        await expect(unite('SNA')).toHaveClass(/gm-org-cible/)
        await capturer(page, `organigramme-glisser-en-cours-${suffixe}`)
        await deposer()
        const c = page.getByRole('alertdialog', { name: 'Déplacer l’unité' })
        await expect(c.getByText('Service Production informatique → Service Normes et Agréments')).toBeVisible()
        await c.getByLabel(/Motif/).fill('Réorganisation du pôle système')
        await c.getByRole('button', { name: 'Déplacer l’unité' }).click()
        await expect(page.getByText('Unité déplacée.')).toBeVisible()
      })

      await test.step('Et il fait d’un compte le n+1 d’un autre en le déposant dessus', async () => {
        const deposer = await saisir(page, compte('u-sna'), compte('u-ingenieur'))
        await deposer()
        const c = page.getByRole('alertdialog', { name: 'Changer le n+1' })
        await c.getByLabel(/Motif/).fill('Encadrement par l’ingénieur')
        await c.getByRole('button', { name: 'Changer le n+1' }).click()
        await expect(page.getByText('Nouveau n+1 enregistré.')).toBeVisible()
      })

      await test.step('Alors le panneau « Pouvoirs » présente la portée effective et mène à la matrice', async () => {
        await page.getByRole('button', { name: 'Pouvoirs de Rachel Biyong Mengue' }).first().click()
        const panneau = page.getByRole('region', { name: 'Pouvoirs de Rachel Biyong Mengue' })
        await expect(panneau).toBeVisible()
        await panneau.scrollIntoViewIfNeeded()
        await capturer(page, `organigramme-panneau-pouvoirs-${suffixe}`)
        await panneau.getByRole('button', { name: /^Ligne de la matrice des pouvoirs/ }).first().click()
        await expect(page).toHaveURL(/\/console\/matrice-pouvoirs\?profil=/)
      })

      await test.step('Et les pouvoirs du chef du DES couvrent son équipe et les unités qu’il dirige', async () => {
        await naviguer(page, 'Organigramme')
        await unite('DES').click()
        await page.getByRole('button', { name: 'Pouvoirs de Christian Ndong Obiang' }).first().click()
        const panneau = page.getByRole('region', { name: 'Pouvoirs de Christian Ndong Obiang' })
        await expect(panneau.getByText(/personnes? couvertes?/).first()).toBeVisible()
        await panneau.getByText(/personnes? couvertes?/).first().click()
      })

      await test.step('Et il crée une sous-unité, la renomme puis la désactive', async () => {
        await unite('SNA').click()
        await page.getByRole('button', { name: 'Sous-unité' }).click()
        let d = page.getByRole('dialog', { name: 'Nouvelle unité' })
        await d.getByLabel(/^Code/).fill('CELLULE_VEILLE')
        await d.getByLabel(/^Libellé/).fill('Cellule veille')
        await d.getByRole('button', { name: 'Créer l’unité' }).click()
        await expect(page.getByText('Unité « Cellule veille » créée.')).toBeVisible()
        await page.getByRole('button', { name: 'Renommer' }).click()
        d = page.getByRole('dialog', { name: 'Modifier l’unité' })
        await d.getByLabel(/^Libellé/).fill('Cellule veille normative')
        await d.getByRole('button', { name: 'Enregistrer l’unité' }).click()
        await expect(page.getByText('Unité « Cellule veille normative » modifiée.')).toBeVisible()
        await page.getByRole('button', { name: 'Désactiver l’unité' }).click()
        const c = page.getByRole('alertdialog', { name: 'Désactiver l’unité' })
        await c.getByLabel(/Motif/).fill('Cellule fusionnée avec le service')
        await c.getByRole('button', { name: 'Désactiver l’unité' }).click()
        await expect(page.getByText('Unité « Cellule veille normative » désactivée.')).toBeVisible()
      })

      await test.step('Et la vue Liste offre les mêmes gestes au clavier', async () => {
        await naviguer(page, 'Organigramme')
        await page.getByRole('button', { name: 'Liste' }).click()
        await page.getByRole('button', { name: 'Désigner le chef de Pôle réseau' }).focus()
        await page.keyboard.press('Enter')
        const d = page.getByRole('dialog', { name: 'Désigner le chef d’unité' })
        await d.getByLabel(/^Chef d’unité/).selectOption('u-sna')
        await d.getByRole('button', { name: 'Continuer' }).click()
        const c = page.getByRole('alertdialog', { name: 'Désigner le chef d’unité' })
        await c.getByLabel(/Motif/).fill('Cheffe du pôle réseau')
        await c.getByRole('button', { name: 'Désigner le chef d’unité' }).click()
        await expect(page.getByText('Chef d’unité désigné.')).toBeVisible()
      })
    })
  })
}
