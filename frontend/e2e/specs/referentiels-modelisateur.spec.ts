import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import type { Locator, Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { PasserelleReferentiels } from './passerelle-referentiels'

// Fonctionnalité : modélisateur de processus des référentiels (DES-12, D-77 §6). L'ingénieur d'homologation
// dérive un brouillon de la version publiée, y ajoute une opération, la relie en tirant d'une carte à l'autre,
// la complète dans la modale (rôles, livrables), contrôle le graphe et publie ; une version publiée se lit sans
// pouvoir être modifiée. Captures des thèmes clair et sombre (GIMAC_CAPTURES=<dossier>). Puis le même travail
// sur la passerelle (VITE_SERVICES=http), dont Playwright simule MS-HOMOLOGATION : routes et corps exacts.

const CAPTURES = process.env.GIMAC_CAPTURES
async function capturer(page: Page, nom: string) {
  if (!CAPTURES) return
  mkdirSync(CAPTURES, { recursive: true })
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const PAGE = '/des/referentiels/operations'
const carte = (page: Page, code: string) => page.locator(`.react-flow__node[data-id="${code}"]`)

/**
 * Amène deux cartes au centre du canevas puis zoome tant qu'elles y tiennent : la disposition par couches de Kahn
 * est large et la vue d'ensemble initiale (ajustée) rend les poignées minuscules.
 */
async function rapprocher(page: Page, a: string, b: string) {
  const zone = page.getByTestId('modelisateur-canevas')
  await zone.scrollIntoViewIfNeeded()
  const boites = async () => {
    const [z, ba, bb] = await Promise.all([zone.boundingBox(), carte(page, a).boundingBox(), carte(page, b).boundingBox()])
    if (!z || !ba || !bb) throw new Error('cartes introuvables')
    return { z, ba, bb, mx: (ba.x + ba.width / 2 + bb.x + bb.width / 2) / 2, my: (ba.y + ba.height / 2 + bb.y + bb.height / 2) / 2 }
  }
  let { z, mx, my } = await boites()
  const cx = z.x + z.width / 2
  const cy = z.y + z.height / 2
  // Panoramique : glisser le fond depuis un coin libre (ni mini-carte ni contrôles).
  await page.mouse.move(z.x + 12, z.y + 12)
  await page.mouse.down()
  await page.mouse.move(z.x + 12 + (cx - mx), z.y + 12 + (cy - my), { steps: 10 })
  await page.mouse.up()
  for (let i = 0; i < 12; i++) {
    const r = await boites()
    z = r.z
    if (r.ba.width >= 200) break
    const f = 1.4
    const dedans = [r.ba, r.bb].every((x) => {
      const g = r.mx + (x.x - r.mx) * f
      const d = r.mx + (x.x + x.width - r.mx) * f
      const h = r.my + (x.y - r.my) * f
      const bas = r.my + (x.y + x.height - r.my) * f
      return g > z.x + 16 && d < z.x + z.width - 16 && h > z.y + 16 && bas < z.y + z.height - 16
    })
    if (!dedans) break
    await page.mouse.move(r.mx, r.my)
    await page.mouse.wheel(0, -150)
    await page.waitForTimeout(250)
  }
}

/** Tire un arc de la sortie d'une carte vers l'entrée d'une autre, à la souris. */
async function relier(page: Page, de: string, vers: string) {
  await rapprocher(page, de, vers)
  const depart = await carte(page, de).locator('.react-flow__handle.source').boundingBox()
  const arrivee = await carte(page, vers).locator('.react-flow__handle.target').boundingBox()
  if (!depart || !arrivee) throw new Error('poignées introuvables')
  await page.mouse.move(depart.x + depart.width / 2, depart.y + depart.height / 2)
  await page.mouse.down()
  await page.mouse.move((depart.x + arrivee.x) / 2, (depart.y + arrivee.y) / 2, { steps: 5 })
  await page.mouse.move(arrivee.x + arrivee.width / 2, arrivee.y + arrivee.height / 2, { steps: 8 })
  await page.mouse.up()
  if (process.env.GIMAC_DEBUG) await page.screenshot({ path: process.env.GIMAC_DEBUG })
}
async function choisir(champ: Locator, option: string) {
  await champ.click()
  await champ.fill(option.slice(0, 4))
  await champ.page().getByRole('listbox').getByRole('option', { name: option, exact: true }).click()
}

test.describe('Fonctionnalité : modélisateur des référentiels (services simulés)', () => {
  test('Scénario : l\'ingénieur d\'homologation dérive, modélise, contrôle et publie un brouillon', async ({ page }) => {
    await demo(page, { profil: 'ROLE_INGENIEUR_HOMOLOGATION', theme: 'light' })
    await test.step('Étant donné la version publiée de la carte interbancaire, en lecture seule', async () => {
      await page.goto(`${ADRESSES.departement}${PAGE}?version=REF-CI-3`)
      await expect(page.getByTestId('modelisateur-canevas')).toBeVisible()
      await expect(carte(page, '19')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Ajouter une opération' })).toHaveCount(0)
      await expect(page.getByText(/Chemin critique : \d+ jours/)).toBeVisible()
    })
    await test.step('Quand il dérive un brouillon', async () => {
      await page.getByRole('button', { name: 'Dériver un brouillon' }).first().click()
      await page.getByRole('alertdialog').getByRole('button', { name: 'Dériver un brouillon' }).click()
      await expect(page.getByRole('heading', { name: 'Carte interbancaire, version 3, brouillon' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'Ajouter une opération' })).toBeVisible()
    })
    await test.step('Et il ajoute une opération dans la modale', async () => {
      await page.getByRole('button', { name: 'Ajouter une opération' }).click()
      const d = page.getByRole('dialog', { name: 'Nouvelle opération' })
      await d.getByLabel(/^Code/).fill('C12')
      await d.getByLabel(/^Libellé/).fill('Revue des journaux du switch après bascule')
      await d.getByLabel(/^Étape/).selectOption('MISE_EN_PRODUCTION')
      await d.getByRole('tab', { name: 'Rôles' }).click()
      await d.getByLabel(/Responsable \(R\)/).selectOption('PRODUCTION_MONETIQUE')
      await d.getByLabel(/Validateur \(V\)/).selectOption('PRODUCTION_MONETIQUE')
      await d.getByRole('button', { name: 'Créer l\'opération' }).click()
      await expect(d.getByText('Le validateur doit être distinct du responsable.')).toBeVisible()
      await d.getByLabel(/Validateur \(V\)/).selectOption('CONDUCTEUR')
      await d.getByRole('button', { name: 'Créer l\'opération' }).click()
      await expect(d).toHaveCount(0)
      await expect(carte(page, 'C12')).toHaveCount(1)
    })
    await test.step('Et il range le canevas puis relie C10 à la nouvelle opération en tirant un arc', async () => {
      await page.getByRole('button', { name: 'Disposition automatique' }).click()
      await expect(page.getByText('Disposition automatique enregistrée.')).toBeAttached()
      await page.waitForTimeout(500)
      await relier(page, 'C10', 'C12')
      await expect(page.getByTestId('rf__edge-C10>C12')).toBeAttached()
      await expect(page.getByText('Arc C10 vers C12 créé.')).toBeAttached()
    })
    await test.step('Et il complète l\'opération : un consulté et un livrable déposé par le participant', async () => {
      await carte(page, 'C12').getByRole('button', { name: 'Modifier l\'opération C12' }).click()
      const d = page.getByRole('dialog', { name: 'Opération C12' })
      await d.getByRole('tab', { name: 'Rôles' }).click()
      await choisir(d.getByRole('combobox', { name: 'Contributeurs (C)' }), 'Pôle réseau')
      // Le premier clic hors de la liste de choix la referme ; on réessaie jusqu'à la sélection de l'onglet.
      const onglet = d.getByRole('tab', { name: 'Livrables et preuves' })
      await expect(async () => {
        await onglet.click()
        await expect(onglet).toHaveAttribute('aria-selected', 'true', { timeout: 1000 })
      }).toPass()
      await d.getByRole('button', { name: 'Ajouter un livrable ou une preuve' }).click()
      const l = d.getByRole('group', { name: 'Livrable 1' })
      await l.getByLabel(/^Libellé/).fill('Extrait des journaux signé')
      await l.getByLabel(/^Nature/).selectOption('PREUVE')
      await l.getByLabel(/^Déposant/).selectOption('PARTICIPANT')
      await d.getByRole('tab', { name: 'Chronologie' }).click()
      await expect(d.locator('.gm-tag', { hasText: /^C10 · / })).toBeVisible()
      await capturer(page, 'modelisateur-modale-light')
      await d.getByRole('button', { name: 'Enregistrer' }).click()
      await expect(d).toHaveCount(0)
      await expect(carte(page, 'C12')).toContainText('1 livrable')
    })
    await test.step('Alors le contrôle ne relève aucune anomalie et la version est publiée', async () => {
      await page.getByRole('button', { name: 'Contrôler', exact: true }).click()
      await expect(page.getByText('Aucune anomalie : le graphe est publiable.')).toBeVisible()
      await capturer(page, 'modelisateur-canevas-light')
      await page.getByRole('toolbar', { name: 'Outils du modélisateur' }).getByRole('button', { name: 'Publier la version' }).click()
      await page.getByRole('alertdialog').getByRole('button', { name: 'Publier la version' }).click()
      await expect(page.getByRole('button', { name: 'Ajouter une opération' })).toHaveCount(0)
      await expect(page.getByText(/lecture seule/).first()).toBeVisible()
      await expect(page.getByRole('treeitem', { name: /Version 4/ })).toBeVisible()
    })
  })

  test('Scénario : anomalies surlignées et modale en thème sombre ; vue liste équivalente', async ({ page }) => {
    await demo(page, { profil: 'ROLE_INGENIEUR_HOMOLOGATION', theme: 'dark' })
    await page.goto(`${ADRESSES.departement}${PAGE}?version=REF-CI-4`)
    await expect(carte(page, 'C11')).toContainText('Validateur absent')
    await page.getByRole('button', { name: 'Contrôler', exact: true }).click()
    await page.getByRole('button', { name: /Validateur absent · C11/ }).click()
    await expect(carte(page, 'C11').locator('.gm-mod-carte-surlignee')).toBeVisible()
    await capturer(page, 'modelisateur-canevas-dark')
    await carte(page, 'C11').getByRole('button', { name: 'Modifier l\'opération C11' }).click()
    const d = page.getByRole('dialog', { name: 'Opération C11' })
    await d.getByRole('tab', { name: 'Exécution' }).click()
    await capturer(page, 'modelisateur-modale-dark')
    await d.getByRole('tab', { name: 'Rôles' }).click()
    await d.getByLabel(/Validateur \(V\)/).selectOption('CONDUCTEUR')
    await d.getByRole('button', { name: 'Enregistrer' }).click()
    await expect(carte(page, 'C11')).not.toContainText('Validateur absent')
    await page.getByRole('button', { name: 'Liste', exact: true }).click()
    await expect(page).toHaveURL(/vue=liste/)
    // Liste paginée : elle s'ouvre sur la page de l'opération surlignée (C11) ; le filtre retrouve l'opération 3.
    await expect(page.getByRole('button', { name: 'Arcs de l\'opération C11', exact: true })).toBeVisible()
    await page.getByRole('searchbox', { name: 'Filtrer les opérations (code ou libellé)' }).fill('3')
    await page.getByRole('button', { name: 'Arcs de l\'opération 3', exact: true }).click()
    const arcs = page.getByRole('dialog', { name: 'Arcs de l\'opération 3' })
    await arcs.getByLabel('Ajouter un prédécesseur').selectOption('35')
    await arcs.getByRole('button', { name: 'Relier' }).click()
    await expect(page.getByText('Cet arc fermerait un cycle : une opération dépendrait d\'elle-même.')).toBeVisible()
    await capturer(page, 'modelisateur-liste-dark')
  })
})

test.describe('Fonctionnalité : modélisateur sur la passerelle (MS-HOMOLOGATION simulé)', () => {
  test('Scénario : chaque geste atteint la route du contrat avec son corps exact', async ({ page }) => {
    const gw = new PasserelleReferentiels()
    await gw.brancher(page)
    const V = '7d0c2a2e-4a51-4c1e-9a55-5f1f0d2c0a01'
    const R = `/bff/des/homologation/referentiels/${V}`
    await page.goto(`${ADRESSES.departementHttp}${PAGE}?version=${V}&vue=liste`)
    await expect(page.getByRole('cell', { name: 'Constat de la couverture' })).toBeVisible()
    await expect(page.getByText('Chemin critique : 9 jours (3 opérations)')).toBeVisible()
    await test.step('Création (POST) puis arc refusé pour cycle (409) et arc accepté (PUT /arcs)', async () => {
      await page.getByRole('button', { name: 'Ajouter une opération' }).click()
      const d = page.getByRole('dialog', { name: 'Nouvelle opération' })
      await d.getByLabel(/^Code/).fill('4')
      await d.getByLabel(/^Libellé/).fill('Recette du switch délégataire')
      await d.getByLabel(/^Étape/).selectOption('TESTS')
      await d.getByLabel(/^Segment/).selectOption('AMONT')
      await d.getByRole('tab', { name: 'Rôles' }).click()
      await d.getByLabel(/Responsable \(R\)/).selectOption('SNA')
      await d.getByLabel(/Validateur \(V\)/).selectOption('CONDUCTEUR')
      await d.getByRole('button', { name: 'Créer l\'opération' }).click()
      await expect(page.getByRole('cell', { name: 'Recette du switch délégataire' })).toBeVisible()
      const post = gw.appels.find((a) => a.methode === 'POST' && a.chemin === `${R}/operations`)!
      expect(post.corps).toMatchObject({ code: '4', libelle: 'Recette du switch délégataire', etape: 'TESTS', segment: 'AMONT', responsable: 'SNA', validateur: 'CONDUCTEUR', delaiJours: 3, livrables: [] })
      await page.getByRole('button', { name: 'Arcs de l\'opération 1' }).click()
      const arcs = page.getByRole('dialog', { name: 'Arcs de l\'opération 1' })
      await arcs.getByLabel('Ajouter un prédécesseur').selectOption('3')
      await arcs.getByRole('button', { name: 'Relier' }).click()
      await expect(page.getByText('Cet arc fermerait un cycle : une opération dépendrait d\'elle-même.')).toBeVisible()
      await expect(page.getByText('e2e-HOM_REFERENTIAL_CYCLE')).toBeVisible()
      await arcs.getByRole('button', { name: 'Fermer' }).last().click()
      await page.getByRole('button', { name: 'Arcs de l\'opération 4' }).click()
      const arcs4 = page.getByRole('dialog', { name: 'Arcs de l\'opération 4' })
      await arcs4.getByLabel('Ajouter un prédécesseur').selectOption('3')
      await arcs4.getByRole('button', { name: 'Relier' }).click()
      await expect(arcs4.getByRole('button', { name: 'Retirer l\'arc 3 vers 4' })).toBeVisible()
      expect(gw.appels.filter((a) => a.chemin === `${R}/arcs`).map((a) => a.corps)).toEqual([{ de: '3', vers: '1', present: true }, { de: '3', vers: '4', present: true }])
      await arcs4.getByRole('button', { name: 'Fermer' }).last().click()
      await expect(page.getByText('Chemin critique : 12 jours (4 opérations)')).toBeVisible()
    })
    await test.step('Modification complète (PUT), pilote (PUT /pilote), disposition (PUT /positions), retrait (DELETE)', async () => {
      await page.getByRole('button', { name: 'Modifier l\'opération 2' }).click()
      const d = page.getByRole('dialog', { name: 'Opération 2' })
      await d.getByRole('tab', { name: 'Exécution' }).click()
      await d.getByLabel(/^Geste/).selectOption('EVENEMENT_EXTERNE')
      await d.getByRole('button', { name: 'Enregistrer' }).click()
      await expect(d.getByText('Un événement d\'un autre service exige son déclencheur.')).toBeVisible()
      await d.getByLabel(/^Déclencheur/).selectOption('certification.pv-signed.v1')
      await d.getByRole('button', { name: 'Enregistrer' }).click()
      await expect(d).toHaveCount(0)
      const put = gw.appels.find((a) => a.methode === 'PUT' && a.chemin === `${R}/operations/2`)!
      expect(put.corps).toMatchObject({ geste: 'EVENEMENT_EXTERNE', declencheur: 'certification.pv-signed.v1', predecesseurs: ['1'],
        livrables: [{ code: 'L1', libelle: 'Compte rendu', nature: 'LIVRABLE', deposant: 'SNA', typesPieces: ['application/pdf'], obligatoire: true }] })
      expect(put.corps).not.toHaveProperty('code')
      await page.getByRole('combobox', { name: /Pilote du brouillon/ }).selectOption('u-ing-2')
      await expect(page.getByText('Pilote : Clarisse Mengue Obame')).toBeVisible()
      expect(gw.appels.find((a) => a.chemin === `${R}/pilote`)!.corps).toEqual({ compteId: 'u-ing-2' })
      await page.getByRole('button', { name: 'Disposition automatique' }).click()
      await expect(page.getByText('Disposition automatique enregistrée.')).toBeAttached()
      const positions = gw.appels.find((a) => a.chemin === `${R}/positions`)!.corps as { code: string; x: number; y: number }[]
      expect(positions.map((p) => p.code).sort()).toEqual(['1', '2', '3', '4'])
      await page.getByRole('button', { name: 'Modifier l\'opération 4' }).click()
      await page.getByRole('dialog', { name: 'Opération 4' }).getByRole('button', { name: 'Supprimer l\'opération' }).click()
      await page.getByRole('alertdialog').getByRole('button', { name: 'Supprimer l\'opération' }).click()
      await expect(page.getByRole('cell', { name: 'Recette du switch délégataire' })).toHaveCount(0)
      expect(gw.appels.some((a) => a.methode === 'DELETE' && a.chemin === `${R}/operations/4`)).toBe(true)
    })
    await test.step('Et le canevas affiche les positions enregistrées', async () => {
      await page.getByRole('button', { name: 'Canevas', exact: true }).click()
      await expect(carte(page, '3')).toContainText('Apte à la certification')
      await expect(page.getByTestId('rf__edge-2>3')).toBeAttached()
    })
  })
})
