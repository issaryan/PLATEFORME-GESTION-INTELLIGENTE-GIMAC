import type { Page } from '@playwright/test'
import { ADRESSES, expect, test } from '../fixtures'
import { confirmerAction, signerCle } from './outils'

// Fonctionnalité : le participant (correspondant de l'Établissement de paiement A, INST-0042) accomplit
// sur son portail ce que le GIMAC attend de lui (qualité Externe, CR §3.4, D-56 à D-59) : tableau de bord
// et tâches paginées, signature du procès-verbal (LIA-2026-027, clé de sécurité), coordonnées d'essai du
// rang 12 (LIA-2026-046), fiche de suivi des services (LIA-2025-088), suivi de ses projets, cloisonnement
// à son institution. Services simulés.

const P = ADRESSES.portail
const attendu = (page: Page) => page.getByRole('region', { name: 'Ce que le GIMAC attend de vous' })

test.describe('Fonctionnalité : parcours de certification sur le portail du participant', () => {
  test('Scénario : voir ce que le GIMAC attend de lui et ses liaisons', async ({ page }) => {
    await test.step('Étant donné le correspondant sur son tableau de bord', async () => {
      await page.goto(`${P}/portail/tableau-de-bord`)
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mon espace de certification')
      await expect(page.getByText('Bienvenue, Correspondant A')).toBeVisible()
      for (const c of ['Tâches à réaliser', 'Procès-verbaux à signer', 'Attestations à fournir', 'Fiches de suivi à renseigner', 'Tâches en retard', 'Échéances proches']) {
        await expect(page.getByRole('region', { name: 'En un coup d\'œil' }).getByText(c, { exact: true })).toBeVisible()
      }
    })
    await test.step('Alors chaque tâche mène à son geste, et la liste se pagine', async () => {
      const taches = attendu(page)
      await expect(taches.getByRole('row', { name: /Signature du procès-verbal/ }).getByRole('button', { name: 'Signer' })).toBeVisible()
      await expect(taches.getByRole('row', { name: /Communication de l'adresse et du port d'essai/ }).getByRole('button', { name: 'Communiquer' })).toBeVisible()
      await expect(taches.getByRole('button', { name: 'Renseigner' })).toBeVisible()
      await expect(taches.getByText(/^Page 1 sur \d+ · \d+ éléments$/)).toBeVisible()
      await expect(taches.getByRole('link', { name: 'LIA-2026-027' }).first()).toBeVisible()
    })
    await test.step('Et ses seules liaisons figurent, la liaison bloquée signalée', async () => {
      const liaisons = page.getByRole('region', { name: 'Mes liaisons' })
      await expect(liaisons.getByRole('row', { name: /LIA-2026-030/ }).getByText('Bloquée')).toBeVisible()
      await expect(liaisons.getByText(/^Page 1 sur 1 · 4 éléments$/)).toBeVisible()
      await expect(page.getByText(/LIA-2026-022|LIA-2026-041|Banque G/)).toHaveCount(0)
      await liaisons.getByRole('link', { name: 'Voir toutes mes liaisons' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mes liaisons')
      await expect(page.getByRole('link', { name: 'LIA-2026-046' })).toBeVisible()
    })
    await test.step('Quand il suit sa liaison bloquée LIA-2026-030', async () => {
      await page.getByRole('link', { name: 'LIA-2026-030' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Suivi de mon projet LIA-2026-030')
    })
    await test.step('Alors il voit le blocage, les dix étapes et ses interlocuteurs par fonction', async () => {
      await expect(page.getByText('Élément de sécurité du participant à renouveler avant la reprise des essais')).toBeVisible()
      const barre = page.getByRole('list', { name: 'Progression des dix étapes' })
      await expect(barre.getByRole('listitem')).toHaveCount(10)
      await expect(barre.getByText('6. Intégration')).toHaveAttribute('aria-current', 'step')
      await expect(page.getByText('Préalable : Approbation de la sécurité')).toBeVisible()
      await expect(page.getByText('participant-030.essai:8443')).toBeVisible()
      await expect(page.getByText('Rien ne vous est demandé pour le moment')).toBeVisible()
      await expect(page.getByText(/Conducteur de projet \d|Chargé DDRM|Ingénieur pôle/)).toHaveCount(0)
    })
    await test.step('Et une liaison d’une autre institution lui reste introuvable', async () => {
      await page.goto(`${P}/portail/liaisons/LIA-2026-022`)
      await expect(page.getByText('Ce parcours est introuvable.')).toBeVisible()
    })
  })

  test('Scénario : signer le procès-verbal de certification avec sa clé de sécurité', async ({ page }) => {
    await page.goto(`${P}/portail/tableau-de-bord`)
    await test.step('Étant donné le procès-verbal établi de LIA-2026-027', async () => {
      await attendu(page).getByRole('row', { name: /Signature du procès-verbal/ }).getByRole('button', { name: 'Signer' }).click()
      await expect(page).toHaveURL(/\/portail\/liaisons\/LIA-2026-027\/proces-verbal/)
      await expect(page.getByText('Établi, à signer')).toBeVisible()
      await expect(page.getByText('100 %')).toBeVisible()
      for (const s of ['Retrait', 'Paiement marchand', 'Transfert']) await expect(page.getByText(s, { exact: true })).toBeVisible()
      await expect(page.getByText('pas encore apposée')).toBeVisible()
    })
    await test.step('Quand il signe avec sa clé de sécurité', async () => {
      await page.getByRole('button', { name: 'Signer le procès-verbal' }).click()
      const d = page.getByRole('dialog')
      await expect(d.getByText('Procès-verbal de certification de LIA-2026-027')).toBeVisible()
      await expect(d.getByText(/couvrent 3 services sur 3/)).toBeVisible()
      await expect(d.getByText('Le procès-verbal attendra ensuite la signature du GIMAC.')).toBeVisible()
      await signerCle(page)
    })
    await test.step('Alors sa signature est apposée et le procès-verbal attend le GIMAC', async () => {
      await expect(page.getByText('Signé par le participant')).toBeVisible()
      await expect(page.getByText('Vous avez signé. Le procès-verbal attend la signature du GIMAC.')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Signer le procès-verbal' })).toHaveCount(0)
    })
    await test.step('Et ses pièces le montrent dans le suivi du projet', async () => {
      await page.getByRole('link', { name: '← Suivi de LIA-2026-027' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Suivi de mon projet LIA-2026-027')
      await expect(page.getByText('Signé par le participant')).toBeVisible()
      await expect(page.getByText('pas encore attendues')).toBeVisible()
      await expect(page.getByText('Banque B')).toBeVisible()
    })
    await test.step('Et les attestations ne sont pas encore attendues', async () => {
      await page.goto(`${P}/portail/liaisons/LIA-2026-027/attestations`)
      await expect(page.getByText('Les attestations sont attendues une fois le procès-verbal de certification signé des deux parties.')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Déposer l\'attestation' })).toHaveCount(0)
    })
  })

  test('Scénario : communiquer l’adresse et le port d’essai (rang 12), contrôlés avant l’envoi', async ({ page }) => {
    await page.goto(`${P}/portail/tableau-de-bord`)
    await test.step('Étant donné les coordonnées d’essai attendues pour LIA-2026-046', async () => {
      await attendu(page).getByRole('row', { name: /Communication de l'adresse et du port d'essai/ }).getByRole('button', { name: 'Communiquer' }).click()
      await expect(page).toHaveURL(/\/portail\/liaisons\/LIA-2026-046\/coordonnees/)
      await expect(page.getByText('Vos coordonnées d\'essai')).toBeVisible()
    })
    await test.step('Quand il saisit une adresse avec un schéma et un port trop court', async () => {
      await page.locator('[name="adresse"]').fill('http://essai participant')
      await page.locator('[name="port"]').fill('8')
      await page.getByRole('button', { name: 'Communiquer les coordonnées' }).click()
    })
    await test.step('Alors les erreurs sont signalées et le premier champ reçoit le focus', async () => {
      await expect(page.getByText(/Adresse invalide/)).toBeVisible()
      await expect(page.getByText(/Port invalide/)).toBeVisible()
      await expect(page.locator('[name="adresse"]')).toBeFocused()
      await page.locator('[name="port"]').fill('84a43')
      await expect(page.locator('[name="port"]')).toHaveValue('8443')
    })
    await test.step('Quand il corrige, ajoute ses points de terminaison et confirme', async () => {
      await page.locator('[name="adresse"]').fill('essai.participant-a.exemple')
      await page.locator('[name="pointsTerminaison"]').fill('https://essai.participant-a.exemple/transactions')
      await page.getByRole('button', { name: 'Communiquer les coordonnées' }).click()
      await expect(page.getByRole('alertdialog').getByText('essai.participant-a.exemple', { exact: true })).toBeVisible()
      await confirmerAction(page, 'Communiquer les coordonnées')
    })
    await test.step('Alors les coordonnées sont communiquées et relisibles', async () => {
      await expect(page.getByText('Coordonnées communiquées')).toBeVisible()
      await expect(page.getByText('8443', { exact: true })).toBeVisible()
      await expect(page.getByText('https://essai.participant-a.exemple/transactions')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Communiquer les coordonnées' })).toHaveCount(0)
    })
    await test.step('Et le suivi du service supplémentaire les reprend', async () => {
      await page.getByRole('link', { name: '← Suivi de LIA-2026-046' }).click()
      await expect(page.getByText('Service supplémentaire de LIA-2025-088')).toBeVisible()
      await expect(page.getByText('essai.participant-a.exemple:8443')).toBeVisible()
    })
  })

  test('Scénario : renseigner la fiche de suivi des services après la mise en production', async ({ page }) => {
    await page.goto(`${P}/portail/tableau-de-bord`)
    await test.step('Étant donné la fiche de suivi remise pour LIA-2025-088', async () => {
      await attendu(page).getByRole('button', { name: 'Renseigner' }).click()
      await expect(page).toHaveURL(/\/portail\/liaisons\/LIA-2025-088\/fiche-suivi/)
      await expect(page.getByText('Service par service, indiquez ce qui est effectivement ouvert')).toBeVisible()
    })
    await test.step('Quand il transmet la fiche incomplète', async () => {
      await page.getByRole('button', { name: 'Transmettre la fiche' }).click()
      await expect(page.getByText('3 services restent à renseigner')).toBeVisible()
      await expect(page.getByText('Indiquez si le service est ouvert.')).toHaveCount(2)
    })
    await test.step('Et qu’il indique pour chaque service s’il est ouvert, avec un commentaire', async () => {
      await page.getByRole('radio', { name: 'Non, pas encore' }).nth(1).check()
      await page.getByRole('radio', { name: 'Oui, ouvert' }).nth(2).check()
      const commentaires = page.getByRole('textbox')
      const n = await commentaires.count()
      for (let i = 0; i < n; i++) await commentaires.nth(i).fill(`Constat du service ${i + 1}`)
      await page.getByRole('button', { name: 'Transmettre la fiche' }).click()
      await expect(page.getByRole('alertdialog').getByText(/2 service\(s\) déclaré\(s\) ouvert\(s\) sur 3/)).toBeVisible()
      await confirmerAction(page, 'Transmettre la fiche')
    })
    await test.step('Alors la fiche transmise se relit, le constat du GIMAC étant attendu', async () => {
      await expect(page.getByText('Fiche transmise')).toBeVisible()
      await expect(page.getByText('Non ouvert')).toBeVisible()
      await expect(page.getByText('Constat du service 2')).toBeVisible()
      await expect(page.getByText('en attente')).toBeVisible()
    })
    await test.step('Et ses attestations, vérifiées par la sécurité, se consultent sans dépôt', async () => {
      await page.goto(`${P}/portail/liaisons/LIA-2025-088/attestations`)
      await expect(page.getByText(/Exigées pour votre activité : ISO 27001, GSMA/)).toBeVisible()
      await expect(page.getByText('vérifiée').first()).toBeVisible()
      await expect(page.getByRole('button', { name: 'Déposer l\'attestation' })).toHaveCount(0)
    })
    await test.step('Et un livrable qui ne lui est pas demandé est expliqué', async () => {
      await page.goto(`${P}/portail/liaisons/LIA-2025-088/livrables/${encodeURIComponent('LIA-2025-088:15')}`)
      await expect(page.getByText('Ce livrable ne vous est pas demandé')).toBeVisible()
      await page.getByRole('link', { name: 'Suivi de LIA-2025-088' }).last().click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Suivi de mon projet LIA-2025-088')
    })
  })
})
