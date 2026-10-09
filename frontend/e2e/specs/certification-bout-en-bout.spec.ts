import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Locator, Page } from '@playwright/test'
import { ADRESSES, expect, test } from '../fixtures'
import { capturePleinePage, carte, confirmer, confirmerAction, joindre, naviguer, ongletSelectionne, signerCle, tiroir } from './outils'

// D-98 (docs/lot1/lancement-processus.md §8) : parcours de certification de bout en bout, en services simulés et en
// démonstration continue (gm.demoContinue : l'état du parcours et du registre est gardé d'un acteur à l'autre). Le chef
// du DES lance une certification (« Lancer une certification » : institution, type de projet créé dans la plateforme,
// conducteur) ; chaque acteur suivant trouve son opération dans « Mes actions de certification », l'ouvre par son lien
// direct et agit (preuve, renvoi motivé, validation signée, constat de couverture, procès-verbal établi puis signé des deux
// parties, dossier et fiche de changement, approbation de la sécurité, bascule) ; le participant signe sur son portail
// (l'état de la démonstration est transmis d'une application à l'autre) ; la liaison atteint EN_PRODUCTION et
// l'institution devient ACTIF. Captures en thème clair et sombre dans e2e/captures/d98 (ou $GIMAC_CAPTURES/d98).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd98') : resolve(ICI, '..', 'captures', 'd98')
const D = ADRESSES.departement
const P = ADRESSES.portail
const ETAT = ['gm.demo.parcours', 'gm.demo.participant']

/**
 * Capture de l'écran courant en thème clair puis sombre (rechargement : l'état de la démonstration est gardé) ; `pleine` :
 * toute la hauteur de l'écran, sinon la fenêtre, l'élément attendu amené en vue.
 */
async function capturer(page: Page, nom: string, attendu?: (p: Page) => Locator, pleine = false) {
  mkdirSync(CAPTURES, { recursive: true })
  for (const [theme, suffixe] of [['light', 'clair'], ['dark', 'sombre']] as const) {
    await page.evaluate((t) => localStorage.setItem('gm.theme', t), theme)
    await page.reload()
    await expect(page.locator('html')).toHaveAttribute('data-theme', theme)
    if (attendu) {
      await expect(attendu(page).first()).toBeVisible()
      if (!pleine) await attendu(page).first().scrollIntoViewIfNeeded()
    }
    await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
    const chemin = join(CAPTURES, `${nom}-${suffixe}.png`)
    if (pleine) await capturePleinePage(page, chemin)
    else await page.screenshot({ path: chemin })
  }
  await page.evaluate(() => localStorage.setItem('gm.theme', 'light'))
}

/** Session de démonstration d'un acteur de l'application interne, sur l'état laissé par le précédent. */
async function devenir(page: Page, profil: string) {
  await page.evaluate((r) => {
    localStorage.setItem('gm.profilDemo', r)
    localStorage.setItem('gm.theme', 'light')
    localStorage.setItem('gm.demoContinue', '1')
  }, profil)
  await page.goto(`${D}/des/tableau-de-bord`)
  await expect(page.getByRole('navigation', { name: 'Navigation principale' })).toBeVisible()
}

/** « Mes actions de certification » depuis la barre latérale, puis l'opération par son lien direct. */
async function ouvrirMonAction(page: Page, liaison: string, libelle: string) {
  await naviguer(page, 'Mes actions de certification')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mes actions de certification')
  const ligne = page.getByRole('row').filter({ hasText: liaison }).filter({ hasText: libelle })
  await expect(ligne).toHaveCount(1)
  await ligne.getByRole('link', { name: libelle, exact: true }).click()
  await expect(page).toHaveURL(new RegExp(`/des/parcours/${liaison}\\?onglet=(operations|decision)&operation=`))
  return ligne
}

/**
 * Transmet l'état de la démonstration d'une application à l'autre (autre origine, autre stockage local) : l'état est
 * écrit depuis une ressource statique de l'origine cible, où aucune application ne tourne, puis l'écran voulu est ouvert.
 */
async function transmettre(page: Page, vers: string) {
  const etat = await page.evaluate((cles) => cles.map((c) => [c, localStorage.getItem(c)] as const), ETAT)
  await page.goto(`${new URL(vers).origin}/favicon.svg`)
  await page.evaluate((e) => {
    localStorage.setItem('gm.demoContinue', '1')
    localStorage.setItem('gm.theme', 'light')
    e.forEach(([c, v]) => (v === null ? localStorage.removeItem(c) : localStorage.setItem(c, v)))
  }, etat)
  await page.goto(vers)
}

async function deposerPreuve(page: Page, code: string, fichier: string) {
  const t = tiroir(page, code)
  await joindre(t, fichier)
  await t.getByRole('button', { name: 'Déposer la preuve' }).click()
  await confirmerAction(page, 'Déposer la preuve')
  await expect(t.getByText('À valider').first()).toBeVisible()
}

async function valider(page: Page, code: string) {
  const t = tiroir(page, code)
  await t.getByRole('button', { name: 'Valider l\'opération' }).click()
  await signerCle(page)
  await expect(t.getByText(/Opération close le/)).toBeVisible()
}

test.describe('Fonctionnalité : parcours de certification de bout en bout (D-98)', () => {
  test('Scénario : un acteur lance une certification, chaque acteur suivant agit depuis « Mes actions de certification », jusqu’à la mise en production', async ({ page }) => {
    test.setTimeout(600_000)
    let liaison = ''

    await test.step('Étant donné la démonstration continue, le chef du DES lance une certification', async () => {
      await page.goto(`${D}/des/tableau-de-bord`)
      await page.evaluate((cles) => cles.forEach((c) => localStorage.removeItem(c)), ETAT)
      await devenir(page, 'ROLE_CHEF_DES')
      await naviguer(page, 'Lancer une certification')
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Lancer une certification')
      const institution = page.getByLabel('Institution', { exact: true })
      await expect(institution.locator('option', { hasText: 'Établissement de paiement A' })).toHaveCount(1)
      await institution.selectOption({ label: 'Établissement de paiement A · En homologation' })
      await expect(page.getByText('Adhésion prononcée : une liaison peut être ouverte sur cette institution.')).toBeVisible()
      await page.getByRole('radio', { name: 'Agrégateur de paiement' }).check()
      await page.getByLabel(/Conducteur de projet/).selectOption({ label: 'Aïcha Mahamat Nour' })
      await capturer(page, 'lancer-certification', (p) => p.getByRole('radio', { name: 'Agrégateur de paiement' }), true)
      await page.getByRole('radio', { name: 'Agrégateur de paiement' }).check()
      await page.getByLabel(/Conducteur de projet/).selectOption({ label: 'Aïcha Mahamat Nour' })
      await page.getByRole('button', { name: 'Ouvrir la liaison' }).click()
      await expect(page).toHaveURL(/\/des\/parcours\/LIA-\d{4}-\d{3}\?nouvelle=1/)
      liaison = /\/des\/parcours\/(LIA-\d{4}-\d{3})/.exec(page.url())![1]
      await expect(page.getByText(`Liaison ${liaison} ouverte au cadrage.`)).toBeVisible()
      await capturer(page, 'dossier-ouvert', (p) => p.getByRole('heading', { level: 1, name: 'Établissement de paiement A' }))
    })

    await test.step('Quand le chargé de la DDRM trouve la preuve du cadrage dans « Mes actions » et la dépose', async () => {
      await devenir(page, 'ROLE_CHARGE_DDRM')
      await ouvrirMonAction(page, liaison, 'Arrêter le périmètre et le calendrier du projet')
      await deposerPreuve(page, 'A1', 'perimetre-calendrier.pdf')
    })

    await test.step('Et le conducteur, validateur désigné, la renvoie avec un motif', async () => {
      await devenir(page, 'ROLE_CONDUCTEUR_PROJET')
      await naviguer(page, 'Mes actions de certification')
      const ligne = page.getByRole('row').filter({ hasText: liaison }).filter({ hasText: 'Arrêter le périmètre' })
      await expect(ligne.getByText('V · Validateur')).toBeVisible()
      await expect(ligne.getByText('Renvoyer')).toBeVisible()
      await capturer(page, 'mes-actions-conducteur', (p) => p.getByRole('row').filter({ hasText: liaison }))
      await ouvrirMonAction(page, liaison, 'Arrêter le périmètre et le calendrier du projet')
      await tiroir(page, 'A1').getByRole('button', { name: 'Renvoyer' }).click()
      await confirmerAction(page, 'Renvoyer', 'Calendrier de bascule à préciser avec le participant')
    })

    await test.step('Alors le chargé de la DDRM la retrouve renvoyée, la redépose, et le conducteur la valide', async () => {
      await devenir(page, 'ROLE_CHARGE_DDRM')
      await ouvrirMonAction(page, liaison, 'Arrêter le périmètre et le calendrier du projet')
      await expect(tiroir(page, 'A1').getByText(/Calendrier de bascule à préciser/)).toBeVisible()
      await deposerPreuve(page, 'A1', 'perimetre-calendrier-v2.pdf')
      await devenir(page, 'ROLE_CONDUCTEUR_PROJET')
      await ouvrirMonAction(page, liaison, 'Arrêter le périmètre et le calendrier du projet')
      await valider(page, 'A1')
    })

    await test.step('Quand l’ingénieur Normes et Agréments dépose les essais, validés par le conducteur, puis constate la couverture', async () => {
      await devenir(page, 'ROLE_SNA')
      await ouvrirMonAction(page, liaison, 'Exécuter les essais de certification')
      await deposerPreuve(page, 'A2', 'rapport-essais.pdf')
      await devenir(page, 'ROLE_CONDUCTEUR_PROJET')
      await ouvrirMonAction(page, liaison, 'Exécuter les essais de certification')
      await valider(page, 'A2')
      await devenir(page, 'ROLE_SNA')
      await ouvrirMonAction(page, liaison, 'Constater la couverture de certification')
      await tiroir(page, 'A3').getByRole('button', { name: 'Constater la couverture' }).click()
      await confirmerAction(page, 'Constater la couverture')
      await expect(tiroir(page, 'A3').getByText(/Opération close le/)).toBeVisible()
    })

    await test.step('Et le conducteur établit le procès-verbal, que le chef du DES signe pour le GIMAC', async () => {
      await devenir(page, 'ROLE_CONDUCTEUR_PROJET')
      await ouvrirMonAction(page, liaison, 'Établir le procès-verbal de certification')
      await page.getByRole('button', { name: 'Établir le procès-verbal' }).click()
      await confirmerAction(page, 'Établir le procès-verbal')
      await expect(carte(page, 'Verrou 1 : procès-verbal de certification').getByText('Établi, à signer').first()).toBeVisible()
      await devenir(page, 'ROLE_CHEF_DES')
      await ouvrirMonAction(page, liaison, 'Signer le procès-verbal de certification')
      // Onglet Décision : la carte du geste visé par ?operation= défile dans la vue et se signale.
      const visee = page.locator('#decision-pv.gm-decision-cible')
      await expect(visee.getByText('Opération visée : Signer le procès-verbal de certification')).toBeVisible()
      await expect(page.locator('#decision-approbation')).not.toHaveClass(/gm-decision-cible/)
      await expect(visee).toBeInViewport()
      await capturer(page, 'decision-carte-visee', (p) => p.locator('#decision-pv.gm-decision-cible'))
      await page.getByRole('button', { name: 'Signer le procès-verbal pour le GIMAC' }).click()
      await signerCle(page)
      await expect(carte(page, 'Verrou 1 : procès-verbal de certification').getByText('Signé par le GIMAC').first()).toBeVisible()
    })

    await test.step('Quand le participant, sur son portail, trouve la signature attendue et signe', async () => {
      await transmettre(page, `${P}/portail/tableau-de-bord`)
      const attendu = page.getByRole('region', { name: 'Ce que le GIMAC attend de vous' })
      const ligne = attendu.getByRole('row').filter({ hasText: liaison })
      await expect(ligne.getByText('Signer le procès-verbal de certification')).toBeVisible()
      await expect(ligne.getByRole('button', { name: 'Signer' })).toBeVisible()
      await capturer(page, 'portail-signature-attendue', (p) => p.getByRole('region', { name: 'Ce que le GIMAC attend de vous' }).getByRole('row').filter({ hasText: liaison }))
      await page.getByRole('region', { name: 'Ce que le GIMAC attend de vous' }).getByRole('row').filter({ hasText: liaison }).getByRole('button', { name: 'Signer' }).click()
      await expect(page).toHaveURL(new RegExp(`/portail/liaisons/${liaison}/proces-verbal`))
      await page.getByRole('button', { name: 'Signer le procès-verbal' }).click()
      await signerCle(page)
      await expect(page.getByRole('button', { name: 'Signer le procès-verbal' })).toHaveCount(0)
      await transmettre(page, `${D}/des/tableau-de-bord`)
    })

    await test.step('Alors la production monétique constitue le dossier et référence la fiche de changement, que la sécurité relit', async () => {
      await devenir(page, 'ROLE_PRODUCTION_MONETIQUE')
      await ouvrirMonAction(page, liaison, 'Constituer le dossier de changement')
      await expect(page.locator('#decision-approbation.gm-decision-cible').getByText('Opération visée : Constituer le dossier de changement')).toBeVisible()
      await page.getByRole('button', { name: 'Constituer le dossier de changement' }).click()
      await joindre(page.getByRole('dialog'), 'dossier-changement.pdf')
      await page.getByRole('dialog').getByRole('button', { name: 'Constituer le dossier de changement' }).click()
      await confirmerAction(page, 'Constituer le dossier de changement')
      await ouvrirMonAction(page, liaison, 'Faire approuver le changement par la sécurité')
      await page.getByRole('button', { name: 'Référencer la fiche de changement' }).click()
      await page.getByRole('dialog').getByRole('textbox', { name: /Référence de la fiche/ }).fill('CHG-2026-420')
      await page.getByRole('dialog').getByRole('button', { name: 'Référencer la fiche de changement' }).click()
      await confirmerAction(page, 'Référencer la fiche de changement')
      await devenir(page, 'ROLE_RSSI')
      await ouvrirMonAction(page, liaison, 'Faire approuver le changement par la sécurité')
      await page.getByRole('button', { name: 'Relire la fiche de changement' }).click()
      await expect(page.getByText('Approuvée par la sécurité').first()).toBeVisible()
    })

    await test.step('Et la production monétique bascule le participant, le conducteur valide : la liaison est en production', async () => {
      await devenir(page, 'ROLE_PRODUCTION_MONETIQUE')
      await ouvrirMonAction(page, liaison, 'Basculer le participant en production')
      await deposerPreuve(page, 'A8', 'compte-rendu-bascule.pdf')
      await devenir(page, 'ROLE_CONDUCTEUR_PROJET')
      await ouvrirMonAction(page, liaison, 'Basculer le participant en production')
      await valider(page, 'A8')
      await expect(page.getByText('En production').first()).toBeVisible()
    })

    await test.step('Alors le conducteur voit la liaison en production et tous ses jalons atteints ; l’institution est active', async () => {
      await page.goto(`${D}/des/certification/mes-actions?onglet=liaisons`)
      const fiche = carte(page, new RegExp(`^${liaison} · `))
      await expect(fiche.getByText('En production').first()).toBeVisible()
      await expect(fiche.getByText('8 opérations faites sur 8')).toBeVisible()
      for (const j of ['Apte à la certification', 'Certifiée', 'Approuvée par la sécurité', 'En production']) await expect(fiche.getByText(new RegExp(`^${j} : Atteint le`))).toBeVisible()
      await capturer(page, 'liaisons-suivies-en-production', (p) => carte(p, new RegExp(`^${liaison} · `)))
      await page.goto(`${D}/des/institutions/INST-0042`)
      await expect(page.getByRole('heading', { level: 1, name: 'Établissement de paiement A' })).toBeVisible()
      await expect(page.getByText('Actif').first()).toBeVisible()
    })

    await test.step('Et le participant suit sa liaison en production sur son portail', async () => {
      await transmettre(page, `${P}/portail/liaisons/${liaison}`)
      await expect(page.getByText('En production').first()).toBeVisible()
      await capturer(page, 'portail-en-production', (p) => p.getByText('En production'))
    })
  })

  test('Scénario : l’adhésion proposée est offerte aux approbateurs, notifiée vers la fiche, close au prononcé', async ({ page }) => {
    const titre = 'Prononcer l’adhésion : Banque de Libreville'
    await test.step('Étant donné le chargé de la DDRM qui propose l’adhésion d’un dossier complet', async () => {
      await page.goto(`${D}/des/tableau-de-bord`)
      await page.evaluate((cles) => cles.forEach((c) => localStorage.removeItem(c)), ETAT)
      await devenir(page, 'ROLE_CHARGE_DDRM')
      await naviguer(page, 'Institutions membres')
      await page.getByRole('link', { name: 'Banque de Libreville' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Banque de Libreville')
      await page.getByRole('button', { name: 'Proposer l\'adhésion' }).click()
      await confirmer(page, 'Proposer l\'adhésion', 'Dossier complet')
      await expect(page.getByRole('alertdialog')).toHaveCount(0)
    })

    await test.step('Quand le chef de la DDRM trouve la décision à prononcer dans ses tâches à prendre', async () => {
      await devenir(page, 'ROLE_CHEF_DDRM')
      await page.goto(`${D}/des/taches?vue=unite`)
      const ligne = page.getByRole('row').filter({ hasText: titre })
      // DDR-16 : la colonne Unité dit à qui la décision est offerte (toutes les unités porteuses de la permission).
      await expect(ligne.getByText('Toutes unités')).toBeVisible()
      await expect(ligne.getByText(/Offerte aux porteurs de :/)).toBeVisible()
      await capturer(page, 'adhesion-a-prononcer', (p) => p.getByRole('row').filter({ hasText: titre }))
    })

    await test.step('Et ouvre sa notification, qui mène à la proposition sur la fiche de l’institution', async () => {
      await page.getByRole('banner').getByRole('button', { name: /^Notifications/ }).click()
      await page.getByRole('dialog', { name: 'Centre de notifications' }).getByRole('button', { name: new RegExp(titre) }).click()
      await expect(page).toHaveURL(/\/des\/institutions\/[^?]+\?onglet=transitions$/)
      await ongletSelectionne(page, 'Propositions de statut')
    })

    await test.step('Alors il prononce l’adhésion avec sa clé de sécurité, et la tâche est close', async () => {
      await page.getByRole('button', { name: 'Approuver' }).click()
      const d = page.getByRole('dialog')
      await d.getByRole('button', { name: 'Utiliser ma clé de sécurité' }).click()
      await expect(d.getByText('Signature confirmée')).toBeVisible()
      await d.getByRole('button', { name: 'Continuer' }).click()
      await expect(page.getByRole('table').getByText('Approuvée')).toBeVisible()
      await page.goto(`${D}/des/taches?vue=unite`)
      await expect(page.getByRole('heading', { level: 1, name: 'Tâches' })).toBeVisible()
      await expect(page.getByRole('row').filter({ hasText: titre })).toHaveCount(0)
    })
  })
})
