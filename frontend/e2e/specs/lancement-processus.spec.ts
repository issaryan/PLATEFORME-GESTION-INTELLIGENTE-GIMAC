import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'

// Fonctionnalité : lancer un processus, agir à chaque étape, suivre son avancement (D-98, docs/lot1/lancement-processus.md
// §1 à §4 et §7), services simulés de l'application du département. Une collaboratrice du service Normes et Agréments
// lance, depuis « Lancer une demande », une demande de changement d'adresse IP adressée au pôle réseau (processus ouvert à
// tout le GIMAC, formulaire de lancement) ; l'ingénieur du pôle reçoit la tâche, ouvre l'étape et n'y trouve que les actions
// permises ; le chef du service valide ; l'ingénieur exécute et clôt ; l'initiatrice suit l'avancement dans « Mes demandes »
// jusqu'à la clôture. L'état simulé est gardé dans l'onglet pendant les changements de profil de démonstration.
// Captures en thème clair et sombre dans e2e/captures/d98 (ou $GIMAC_CAPTURES/d98).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd98') : resolve(ICI, '..', 'captures', 'd98')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const OBJET = 'Poste d’accueil du rez-de-chaussée'

/** Change de profil de démonstration (rechargement) en gardant l'état simulé de l'onglet. */
async function devenir(page: Page, profil: string, theme: 'light' | 'dark', chemin: string) {
  await demo(page, { profil, theme })
  await page.goto(`${ADRESSES.departement}${chemin}`)
}

/** L'exécutant renseigne les champs de l'étape ouverte, les enregistre et termine l'étape. */
async function terminerEtape(page: Page, libelle: string, champs: [RegExp, string, 'select' | 'fill'][]) {
  const panneau = page.getByRole('complementary', { name: new RegExp(libelle) })
  await expect(panneau.getByRole('button', { name: 'Terminer l’étape' })).toBeVisible()
  for (const [etiquette, valeur, geste] of champs) {
    if (geste === 'select') await panneau.getByLabel(etiquette).selectOption(valeur)
    else await panneau.getByLabel(etiquette).fill(valeur)
  }
  if (champs.length) {
    await panneau.getByRole('button', { name: 'Enregistrer les champs' }).click()
    await expect(page.getByRole('status').filter({ hasText: /enregistr/i }).first()).toBeAttached()
  }
  await panneau.getByRole('button', { name: 'Terminer l’étape' }).click()
  await page.getByRole('alertdialog', { name: 'Terminer l’étape' }).getByRole('button', { name: 'Terminer l’étape' }).click()
  await expect(panneau.getByText('Terminée', { exact: true }).first()).toBeVisible()
}

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : lancer une demande et suivre son avancement, services simulés (${suffixe})`, () => {
    test('Scénario : une collaboratrice du SNA demande un changement d’adresse IP au pôle réseau et suit sa demande jusqu’à la clôture', async ({ page }) => {
      test.setTimeout(120_000)
      let instance = ''

      await test.step('Étant donné « Lancer une demande » dans la barre latérale : processus ouverts, groupés par service destinataire', async () => {
        await devenir(page, 'ROLE_SNA', theme, '/des/tableau-de-bord')
        await page.getByRole('link', { name: 'Lancer une demande' }).first().click()
        await expect(page.getByRole('heading', { level: 1, name: 'Lancer une demande' })).toBeVisible()
        await expect(page.getByRole('heading', { level: 2, name: 'Pôle réseau' })).toBeVisible()
        await expect(page.getByRole('heading', { level: 2, name: 'Service des ressources humaines' })).toBeVisible()
        // Le processus réservé aux membres du pôle n'est pas proposé.
        await expect(page.getByText('Ouverture d’un flux réseau')).toHaveCount(0)
        await capturer(page, `lancer-demande-${suffixe}`)
      })

      await test.step('Quand elle remplit le formulaire de lancement (objet, équipement, adresses, justification) et envoie la demande', async () => {
        await page.getByRole('button', { name: 'Lancer « Demande de changement d’adresse IP »' }).click()
        const d = page.getByRole('dialog', { name: 'Nouvelle demande' })
        await expect(d.getByText(/adressée à : Pôle réseau/)).toBeVisible()
        await d.getByRole('button', { name: 'Envoyer la demande' }).click()
        await expect(d.getByText('Champ obligatoire.').first()).toBeVisible()
        await d.getByLabel(/^Objet de la demande/).fill(OBJET)
        await d.getByLabel(/^Équipement/).fill('PC-SNA-014')
        await d.getByLabel(/^Adresse IP actuelle/).fill('10.20.4.15')
        await d.getByLabel(/^Adresse IP demandée/).fill('10.20.6.40')
        await d.getByLabel(/^Justification/).fill('Changement de bureau vers le plateau du service.')
        await capturer(page, `lancer-demande-formulaire-${suffixe}`)
        await d.getByRole('button', { name: 'Envoyer la demande' }).click()
        await expect(page.getByRole('heading', { level: 1, name: OBJET })).toBeVisible()
        instance = new URL(page.url()).pathname.split('/').pop()!
        expect(instance).toMatch(/^INS-/)
        await expect(page.getByTestId('valeurs-lancement').getByText('10.20.6.40')).toBeVisible()
      })

      await test.step('Alors « Mes demandes » montre l’étape en cours et la personne du pôle réseau qui la tient', async () => {
        await page.getByRole('button', { name: 'Mes demandes' }).click()
        await expect(page.getByRole('heading', { level: 1, name: 'Mes demandes' })).toBeVisible()
        const carte = page.getByTestId(`demande-${instance}`)
        await expect(carte.getByText(/tenue par Idriss Mahamat Saleh/)).toBeVisible()
        await expect(carte.getByRole('list', { name: 'Étapes de la demande : 0 terminée(s) sur 4' })).toBeVisible()
        await expect(page.getByText('Demande de changement d’adresse IP · Pôle réseau')).toBeVisible()
        await capturer(page, `mes-demandes-envoyee-${suffixe}`)
      })

      await test.step('Quand l’ingénieur du pôle réseau reçoit la tâche, ouvre l’étape et la réalise (seules les actions permises)', async () => {
        await devenir(page, 'ROLE_POLE_RESEAU', theme, '/des/taches')
        await page.getByText(`${OBJET} : Analyse technique`).first().click()
        await page.getByRole('link', { name: 'Ouvrir', exact: true }).click()
        await expect(page.getByRole('heading', { level: 1, name: OBJET })).toBeVisible()
        const panneau = page.getByRole('complementary', { name: /Analyse technique/ })
        await expect(panneau.getByRole('button', { name: 'Terminer l’étape' })).toBeVisible()
        // Pas de pilotage de l'instance ni de réaffectation pour l'exécutant.
        await expect(page.getByRole('button', { name: 'Suspendre' })).toHaveCount(0)
        await expect(panneau.getByRole('button', { name: 'Valider' })).toHaveCount(0)
        await expect(page.getByTestId('valeurs-lancement').getByText('PC-SNA-014')).toBeVisible()
        await capturer(page, `etape-intervenant-${suffixe}`)
        await terminerEtape(page, 'Analyse technique', [[/^Adresse disponible/, 'Oui', 'select']])
      })

      await test.step('Alors l’initiatrice voit l’avancement : analyse terminée, validation tenue par le chef du service', async () => {
        await devenir(page, 'ROLE_SNA', theme, '/des/demandes')
        const carte = page.getByTestId(`demande-${instance}`)
        await expect(carte.getByText(/tenue par Gaston Ndzana Ebode/)).toBeVisible()
        await expect(carte.getByRole('button', { name: 'Ouvrir l’étape Analyse technique (Terminée)' })).toBeVisible()
        await expect(page.getByText('Demande de changement d’adresse IP · Pôle réseau')).toBeVisible()
        await capturer(page, `mes-demandes-suivi-${suffixe}`)
      })

      await test.step('Quand le chef du service valide, puis l’ingénieur exécute le changement et clôt la demande', async () => {
        await devenir(page, 'ROLE_RESPONSABLE_SERVICE', theme, `/des/processus/instances/${instance}?etape=VALIDATION`)
        await terminerEtape(page, 'Validation du chef de pôle', [[/^Décision/, 'Accordée', 'select']])
        await devenir(page, 'ROLE_POLE_RESEAU', theme, `/des/processus/instances/${instance}?etape=EXECUTION`)
        await terminerEtape(page, 'Mise en œuvre du changement', [[/^Adresse IP attribuée/, '10.20.6.40', 'fill']])
        await page.goto(`${ADRESSES.departement}/des/processus/instances/${instance}?etape=CLOTURE`)
        await terminerEtape(page, 'Vérification et clôture', [])
      })

      await test.step('Alors l’initiatrice voit sa demande close, toutes les étapes terminées', async () => {
        await devenir(page, 'ROLE_SNA', theme, '/des/demandes')
        const carte = page.getByTestId(`demande-${instance}`)
        await expect(carte.getByText(/Demande close le/)).toBeVisible()
        await expect(carte.getByRole('list', { name: 'Étapes de la demande : 4 terminée(s) sur 4' })).toBeVisible()
        await expect(page.getByText('Demande de changement d’adresse IP · Pôle réseau')).toBeVisible()
        await capturer(page, `mes-demandes-close-${suffixe}`)
        await page.getByRole('button', { name: OBJET, exact: true }).click()
        await expect(page.getByText('Close', { exact: true }).first()).toBeVisible()
        await capturer(page, `demande-close-suivi-${suffixe}`)
      })
    })
  })
}
