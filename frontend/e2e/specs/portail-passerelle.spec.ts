import { ADRESSES, expect, test } from '../fixtures'
import { confirmerAction, joindre } from './outils'
import { PasserellePortail, PERMISSIONS_CORRESPONDANT } from './passerelle-portail'

// Fonctionnalité : le portail des participants sur la passerelle (VITE_SERVICES=http), dont Playwright
// simule les réponses : fourniture des attestations de sécurité une fois le procès-verbal signé des deux
// parties (rangs 21 et 22 : PDF seulement, attestation rejetée redéposée), dépôt du paramétrage d'essai
// (rang 15, multipart), gestes réservés aux permissions du jeton, refus du service expliqué.

const P = ADRESSES.portailHttp

test.describe('Fonctionnalité : portail des participants sur la passerelle', () => {
  test('Scénario : fournir les attestations de sécurité, dont une rejetée par la sécurité des SI', async ({ page }) => {
    const gw = new PasserellePortail()
    await gw.brancher(page)
    await test.step('Étant donné la tâche de fourniture des attestations de LIA-2026-060', async () => {
      await page.goto(`${P}/portail/tableau-de-bord`)
      await expect(page.locator('.gm-pagehead-nom', { hasText: 'Brice Nzeng Mba' })).toBeVisible()
      const taches = page.getByRole('region', { name: 'Ce que le GIMAC attend de vous' })
      await expect(taches.getByText(/^Page 1 sur 1 · 2 éléments$/)).toBeVisible()
      await taches.getByRole('row', { name: /LIA-2026-060/ }).getByRole('button', { name: 'Fournir' }).click()
    })
    await test.step('Alors les attestations exigées sont présentées avec leur état', async () => {
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Attestations de sécurité')
      await expect(page.getByText('Exigées pour votre activité : ISO 27001, GSMA, Conformité COBAC')).toBeVisible()
      await expect(page.getByText(/En tant que fintech/)).toBeVisible()
      await expect(page.getByText(/par la Sécurité des systèmes d'information/).first()).toBeVisible()
      await expect(page.getByText('Attestation expirée depuis le 30 juin 2026')).toBeVisible()
    })
    await test.step('Quand il redépose l’attestation GSMA, d’abord sans fichier puis dans un format refusé', async () => {
      await page.getByRole('button', { name: 'Déposer une nouvelle attestation' }).click()
      await expect(page.getByText('Choisissez l\'attestation à déposer.')).toBeVisible()
      await page.locator('input[type="file"]').first().setInputFiles({ name: 'gsma.docx', mimeType: 'application/octet-stream', buffer: Buffer.from('x') })
      await expect(page.getByText('L\'attestation doit être un fichier PDF.')).toBeVisible()
    })
    await test.step('Et qu’il dépose le PDF et confirme', async () => {
      await joindre(page, 'gsma-2026.pdf', '%PDF-1.7 attestation GSMA')
      await page.getByRole('button', { name: 'Déposer une nouvelle attestation' }).click()
      await expect(page.getByRole('alertdialog').getByText('gsma-2026.pdf')).toBeVisible()
      await confirmerAction(page, 'Déposer l\'attestation')
    })
    await test.step('Alors l’attestation part en multipart au service et passe en vérification', async () => {
      const appel = gw.appels.find((a) => a.methode === 'POST' && a.chemin.endsWith('/attestations/GSMA'))!
      expect(appel.chemin).toBe('/bff/portail/certification/liaisons/LIA-2026-060/attestations/GSMA')
      expect(appel.entetes['content-type']).toMatch(/^multipart\/form-data/)
      expect(appel.corps).toContain('filename="gsma-2026.pdf"')
      await expect(page.getByText('gsma-2026.pdf')).toBeVisible()
      await expect(page.getByText('Attestation expirée depuis le 30 juin 2026')).toHaveCount(0)
    })
    await test.step('Et il fournit la justification de la conformité COBAC', async () => {
      await joindre(page, 'cobac.pdf', '%PDF-1.7 COBAC')
      await page.getByRole('button', { name: 'Déposer l\'attestation' }).click()
      await confirmerAction(page, 'Déposer l\'attestation')
      await expect(page.getByText('cobac.pdf')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Déposer l\'attestation' })).toHaveCount(0)
    })
  })

  test('Scénario : déposer le paramétrage d’essai, puis suivre son projet', async ({ page }) => {
    const gw = new PasserellePortail()
    await gw.brancher(page)
    await page.goto(`${P}/portail/tableau-de-bord`)
    await test.step('Étant donné le paramétrage d’essai de LIA-2026-061 attendu de lui', async () => {
      await page.getByRole('region', { name: 'Ce que le GIMAC attend de vous' }).getByRole('row', { name: /LIA-2026-061/ }).getByRole('button', { name: 'Déposer' }).click()
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Dépôt d\'un livrable')
      await expect(page.getByText('Paramétrage d\'essai', { exact: true })).toBeVisible()
      await expect(page.getByText('Document de paramétrage, livrable unique et imposé de la phase')).toBeVisible()
      await expect(page.getByText(/Opération facile à omettre/)).toBeVisible()
    })
    await test.step('Quand il dépose sans fichier, puis avec le document et un commentaire', async () => {
      await page.getByRole('button', { name: 'Déposer le livrable' }).click()
      await expect(page.getByText('Choisissez le fichier à déposer.')).toBeVisible()
      await joindre(page, 'parametrage-essai.pdf')
      await page.locator('[name="commentaire"]').fill('Version validée en atelier')
      await page.getByRole('button', { name: 'Déposer le livrable' }).click()
      await expect(page.getByRole('alertdialog').getByText('parametrage-essai.pdf')).toBeVisible()
      await confirmerAction(page, 'Déposer le livrable')
    })
    await test.step('Alors le livrable part au service et il revient au suivi de son projet', async () => {
      const appel = gw.appels.find((a) => a.methode === 'POST' && a.chemin.includes('/operations/'))!
      expect(decodeURIComponent(appel.chemin)).toBe('/bff/portail/homologation/operations/LIA-2026-061:15/livrable')
      expect(appel.corps).toContain('Version validée en atelier')
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Suivi de mon projet LIA-2026-061')
      await expect(page.getByText('pas encore attendues')).toBeVisible()
      await expect(page.getByText('pas encore remise')).toBeVisible()
      await expect(page.getByText('Non communiquées')).toBeVisible()
    })
    await test.step('Et le livrable déjà déposé ne lui est plus demandé ; ses procès-verbaux se lisent', async () => {
      await page.goto(`${P}/portail/liaisons/LIA-2026-061/livrables/${encodeURIComponent('LIA-2026-061:15')}`)
      await expect(page.getByText('Ce livrable ne vous est pas demandé')).toBeVisible()
      await page.goto(`${P}/portail/liaisons/LIA-2026-061/proces-verbal`)
      await expect(page.getByText('Le procès-verbal n\'est pas encore établi')).toBeVisible()
      await page.goto(`${P}/portail/liaisons/LIA-2026-060/proces-verbal`)
      await expect(page.getByText('Signé des deux parties')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Signer le procès-verbal' })).toHaveCount(0)
    })
  })

  test('Scénario : sans droit de dépôt, les tâches restent lisibles, les gestes réservés', async ({ page }) => {
    const gw = new PasserellePortail({ permissions: PERMISSIONS_CORRESPONDANT.filter((p) => p !== 'HOM_PORTAL_SUBMIT' && p !== 'CER_PORTAL_READ') })
    await gw.brancher(page)
    await test.step('Étant donné un correspondant sans HOM_PORTAL_SUBMIT ni CER_PORTAL_READ', async () => {
      await page.goto(`${P}/portail/tableau-de-bord`)
    })
    await test.step('Alors ses tâches sont au tour du GIMAC, sans geste', async () => {
      const taches = page.getByRole('region', { name: 'Ce que le GIMAC attend de vous' })
      await expect(taches.getByText('Au tour du GIMAC').first()).toBeVisible()
      await expect(taches.getByRole('button', { name: 'Déposer' })).toHaveCount(0)
    })
    await test.step('Et les pièces de certification ne lui sont pas ouvertes', async () => {
      await page.goto(`${P}/portail/liaisons/LIA-2026-060/attestations`)
      await expect(page.getByText('Votre profil ne donne pas accès aux pièces de certification.')).toBeVisible()
      await page.goto(`${P}/portail/liaisons/LIA-2026-061/livrables/${encodeURIComponent('LIA-2026-061:15')}`)
      await expect(page.getByText(/pas de l'accomplir/)).toBeVisible()
    })
  })

  test('Scénario : tâches indisponibles, erreur expliquée avec son identifiant de corrélation', async ({ page }) => {
    const gw = new PasserellePortail({ tachesEnErreur: true })
    await gw.brancher(page)
    await page.goto(`${P}/portail/tableau-de-bord`)
    await expect(page.getByText('e2e-HOM_UNAVAILABLE').first()).toBeVisible()
    await expect(page.getByRole('button', { name: 'Réessayer' }).first()).toBeVisible()
    await page.goto(`${P}/portail/liaisons/LIA-2026-099`)
    await expect(page.getByText('Ce parcours est introuvable.')).toBeVisible()
  })
})
