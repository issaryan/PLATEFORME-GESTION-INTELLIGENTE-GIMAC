import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test, type ProfilDemo } from '../fixtures'

// Fonctionnalité : le créateur d'un processus définit qui peut le voir et le lancer (D-100,
// docs/lot1/lancement-processus.md §10), services simulés de l'application du département. Le super administrateur
// (REF_METIER_ADMIN) ne voit plus dans « Lancer une demande » la « Création d'une machine virtuelle » du pôle système,
// réservée par son créateur à deux personnes désignées ; il la lit toujours dans « Processus de l'unité », où chaque
// processus affiche son public. L'analyste Normes et Agréments, l'une des personnes désignées, la voit et la lance. Le chef
// du service règle le public d'un brouillon : personnes choisies dans l'annuaire, en puces retirables.
// Captures en thème clair et sombre dans e2e/captures/d100 (ou $GIMAC_CAPTURES/d100).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd100') : resolve(ICI, '..', 'captures', 'd100')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const VM = 'Création d’une machine virtuelle'
const OBJET = 'Serveur de recette du SNA'

/** Change de profil de démonstration (rechargement) en gardant l'état simulé de l'onglet. */
async function devenir(page: Page, profil: string, theme: 'light' | 'dark', chemin: string) {
  await demo(page, { profil: profil as ProfilDemo, theme })
  await page.goto(`${ADRESSES.departement}${chemin}`)
}

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : public d’un processus défini par son créateur, services simulés (${suffixe})`, () => {
    test('Scénario : l’administrateur ne voit pas un processus réservé ; la personne désignée le voit et le lance', async ({ page }) => {
      test.setTimeout(120_000)

      await test.step('Étant donné le super administrateur sur « Lancer une demande » : le processus réservé à deux personnes n’y figure pas', async () => {
        await devenir(page, 'ROLE_SUPER_ADMIN', theme, '/des/demandes/lancer')
        await expect(page.getByRole('heading', { level: 1, name: 'Lancer une demande' })).toBeVisible()
        await expect(page.getByRole('heading', { level: 2, name: 'Service des ressources humaines' })).toBeVisible()
        await expect(page.getByText(VM)).toHaveCount(0)
        await expect(page.getByText('Mise à jour de la plateforme')).toHaveCount(0)
        await expect(page.getByRole('heading', { level: 2, name: 'Pôle système' })).toHaveCount(0)
        await capturer(page, `admin-lancer-demande-${suffixe}`)
      })

      await test.step('Alors il le lit toujours dans « Processus de l’unité », avec son public, sans pouvoir le lancer ; il peut en régler le public (D-103)', async () => {
        await page.goto(`${ADRESSES.departement}/des/processus`)
        const publicVm = page.getByTestId('public-CREATION_MACHINE_VIRTUELLE')
        await expect(publicVm).toHaveText('Visible et lançable par : Personnes désignées (Idriss Mahamat Saleh, Rachel Biyong Mengue)')
        await expect(page.getByTestId('public-MISE_A_JOUR_PLATEFORME')).toHaveText('Visible et lançable par : Profils désignés (Conformité, RSSI)')
        await expect(page.getByRole('button', { name: /^Lancer.*machine virtuelle/ })).toHaveCount(0)
        await publicVm.scrollIntoViewIfNeeded()
        await capturer(page, `admin-catalogue-public-${suffixe}`)
        await page.getByRole('button', { name: VM, exact: true }).click()
        await expect(page.getByRole('heading', { level: 1, name: VM })).toBeVisible()
        await expect(page.getByTestId('public-processus')).toContainText('Personnes désignées (Idriss Mahamat Saleh, Rachel Biyong Mengue)')
        await expect(page.getByRole('button', { name: 'Lancer', exact: true })).toHaveCount(0)
        // D-103 : qui définit les processus de l'unité (ici REF_METIER_ADMIN) règle le public d'une version publiée.
        await page.getByRole('button', { name: `Régler qui peut voir et lancer « ${VM} »` }).click()
        const d = page.getByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
        await expect(d.getByTestId('initiateurs-personnes').getByText('2 personnes choisies')).toBeVisible()
        await expect(d.getByText('Version publiée : seul le public se règle ici')).toBeVisible()
        await capturer(page, `admin-public-lecture-${suffixe}`)
      })

      await test.step('Quand l’analyste Normes et Agréments, personne désignée, ouvre « Lancer une demande » : elle voit le processus', async () => {
        await devenir(page, 'ROLE_SNA', theme, '/des/demandes/lancer')
        await expect(page.getByRole('heading', { level: 2, name: 'Pôle système' })).toBeVisible()
        await expect(page.getByText('Ouvert à des personnes désignées')).toBeVisible()
        await expect(page.getByText('Mise à jour de la plateforme')).toHaveCount(0)
        await capturer(page, `designee-lancer-demande-${suffixe}`)
      })

      await test.step('Alors elle le lance avec son formulaire et suit sa demande', async () => {
        await page.getByRole('button', { name: `Lancer « ${VM} »` }).click()
        const d = page.getByRole('dialog', { name: 'Nouvelle demande' })
        await expect(d.getByText(/adressée à : Pôle système/)).toBeVisible()
        await d.getByLabel(/^Objet de la demande/).fill(OBJET)
        await d.getByLabel(/^Système d’exploitation/).selectOption('Linux')
        await d.getByLabel(/^Nombre de processeurs/).fill('4')
        await d.getByLabel(/^Mémoire/).fill('16')
        await capturer(page, `designee-formulaire-${suffixe}`)
        await d.getByRole('button', { name: 'Envoyer la demande' }).click()
        await expect(page.getByRole('heading', { level: 1, name: OBJET })).toBeVisible()
        await expect(page.getByTestId('valeurs-lancement').getByText('Linux')).toBeVisible()
        await capturer(page, `designee-demande-envoyee-${suffixe}`)
      })

      await test.step('Et le chef du service règle le public d’un brouillon : personnes choisies dans l’annuaire, en puces retirables', async () => {
        await devenir(page, 'ROLE_RESPONSABLE_SERVICE', theme, '/des/processus')
        await page.getByRole('button', { name: 'Régler qui peut voir et lancer « Accès VPN d’un participant »' }).click()
        const d = page.getByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
        await expect(d.getByText(/Seules ces personnes voient le processus dans « Lancer une demande »/)).toBeVisible()
        await d.getByLabel('Personnes désignées').check()
        const zone = d.getByTestId('initiateurs-personnes')
        await zone.getByRole('button', { name: /Rachel Biyong Mengue/ }).click()
        await zone.getByRole('button', { name: /Chantal Ngo Bassong/ }).click()
        await expect(zone.getByText('2 personnes choisies')).toBeVisible()
        await d.getByTestId('initiateurs-personnes').scrollIntoViewIfNeeded()
        await capturer(page, `reglage-personnes-${suffixe}`)
        await d.getByLabel('Membres et responsables de l’unité').uncheck()
        await d.getByRole('button', { name: 'Enregistrer' }).click()
        await expect(d).toHaveCount(0)
        await expect(page.getByTestId('public-processus')).toContainText('Personnes désignées (Rachel Biyong Mengue, Chantal Ngo Bassong)')
        await capturer(page, `modelisateur-public-${suffixe}`)
      })
    })
  })
}
