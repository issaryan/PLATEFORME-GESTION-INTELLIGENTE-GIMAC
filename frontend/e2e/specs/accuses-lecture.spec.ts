import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Locator, Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { PasserelleAccusesLecture } from './passerelle-accuses-lecture'

// Fonctionnalité D-88 : accusés de lecture de la messagerie (docs/lot1/accuses-lecture.md). Sous mes messages,
// une coche grise (envoyé), deux coches grises (lu par certains), deux coches d'accent (lu) ; détail « Lu par » d'un
// message de groupe avec l'heure (« heure inconnue » avant D-88) ; passage à « Lu » en direct à l'accusé poussé.
// Services simulés (thèmes clair et sombre, captures dans e2e/captures/d88-d89 ou $GIMAC_CAPTURES/d88-d89), puis
// passerelle (VITE_SERVICES=http) : routes, réponses et événement collaboration.conversation-read.v1 exacts.

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'd88-d89') : resolve(ICI, '..', 'captures', 'd88-d89')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().filter((a) => a.effect?.getComputedTiming().iterations !== Infinity).map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}
const conversations = (page: Page) => page.getByRole('list', { name: 'Conversations' })
/** Bulle désignée par son propre texte. */
const bulle = (fil: Locator, texte: string | RegExp) => fil.locator('li.gm-msg').filter({ has: fil.page().locator('.gm-msg-texte', { hasText: texte }) })
const lire = (page: Page, o: Record<string, unknown>) =>
  page.evaluate((x) => (window as unknown as { gmDemoNotifications: { lireMessages(o: unknown): boolean } }).gmDemoNotifications.lireMessages(x), o)

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'

  test.describe(`Fonctionnalité : accusés de lecture (D-88, ${suffixe})`, () => {
    test('Scénario : coches d’un échange direct, envoi puis lecture en direct ; « Lu par » d’un groupe', async ({ page }) => {
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/messagerie`)

      await test.step('Étant donné l’échange direct avec Aïcha : mon message est lu (deux coches d’accent)', async () => {
        await conversations(page).getByRole('button', { name: /Aïcha Mahamat Nour/ }).click()
        const fil = page.getByRole('region', { name: 'Aïcha Mahamat Nour' })
        await expect(bulle(fil, 'Bien reçu, je le signe').getByRole('img', { name: 'Lu' })).toBeVisible()
      })

      await test.step('Quand j’envoie un message, une coche grise « Envoyé », aussi dans la liste', async () => {
        const fil = page.getByRole('region', { name: 'Aïcha Mahamat Nour' })
        await fil.getByLabel('Votre message').fill('Je vous renvoie le procès-verbal signé.')
        await fil.getByRole('button', { name: 'Envoyer' }).click()
        const envoye = bulle(fil, 'Je vous renvoie le procès-verbal signé.')
        await expect(envoye.getByRole('img', { name: 'Envoyé' })).toBeVisible()
        await expect(envoye.locator('.gm-coches')).not.toHaveClass(/gm-coches-lu/)
        await expect(conversations(page).getByRole('button', { name: /Aïcha Mahamat Nour/ }).getByRole('img', { name: 'Envoyé' })).toBeVisible()
        await capturer(page, `direct-envoye-${suffixe}`)
      })

      await test.step('Alors, quand Aïcha lit la conversation, mon message passe à « Lu » sans relire le fil', async () => {
        expect(await lire(page, {})).toBe(true)
        const fil = page.getByRole('region', { name: 'Aïcha Mahamat Nour' })
        const envoye = bulle(fil, 'Je vous renvoie le procès-verbal signé.')
        await expect(envoye.getByRole('img', { name: 'Lu' })).toBeVisible()
        await expect(envoye.locator('.gm-coches')).toHaveClass(/gm-coches-lu/)
        await expect(conversations(page).getByRole('button', { name: /Aïcha Mahamat Nour/ }).getByRole('img', { name: 'Lu' })).toBeVisible()
        await capturer(page, `direct-lu-${suffixe}`)
      })

      await test.step('Et dans le groupe des essais : « Lu par 2 sur 3 », détail au survol avec l’heure, heure inconnue', async () => {
        await conversations(page).getByRole('button', { name: /Essais de LIA-2026-030/ }).click()
        const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
        const mien = bulle(fil, 'tenez-nous informés de la date')
        const bouton = mien.getByRole('button', { name: 'Lu par 2 sur 3, voir qui l’a lu' })
        await expect(bouton).toHaveText('2/3')
        await capturer(page, `groupe-coches-${suffixe}`)
        await bouton.hover()
        const detail = mien.getByRole('dialog', { name: 'Lu par' })
        await expect(detail.getByRole('list', { name: 'Membres qui ont lu le message' }).getByRole('listitem')).toHaveCount(2)
        await expect(detail.getByText('heure inconnue')).toBeVisible()
        await expect(detail.getByRole('list', { name: 'Pas encore lu' })).toContainText('Aïcha Mahamat Nour')
        await capturer(page, `groupe-lu-par-${suffixe}`)
        // Le clic épingle le détail ; Échap le ferme.
        await bouton.click()
        await page.mouse.move(10, 10)
        await expect(detail).toBeVisible()
        await bouton.press('Escape')
        await expect(detail).toHaveCount(0)
      })

      await test.step('Et quand Aïcha lit le groupe, le fil est relu : « Lu » ; le détail ouvert est relu aussi', async () => {
        const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
        const mien = bulle(fil, 'tenez-nous informés de la date')
        await mien.getByRole('button', { name: /^Lu par 2 sur 3/ }).click()
        expect(await lire(page, { conversationId: (await page.evaluate(() => new URLSearchParams(location.search).get('conversation'))) })).toBe(true)
        await expect(mien.getByRole('button', { name: 'Lu, voir qui l’a lu' })).toBeVisible()
        await expect(mien.getByRole('dialog', { name: 'Lu par' }).getByRole('list', { name: 'Membres qui ont lu le message' }).getByRole('listitem')).toHaveCount(3)
      })
    })
  })
}

test.describe('Fonctionnalité : accusés de lecture par la passerelle (D-88)', () => {
  test('Scénario : coches servies, détail « Lu par » (GET …/lectures), accusés poussés en direct et en groupe', async ({ page }) => {
    const gw = new PasserelleAccusesLecture()
    await gw.brancher(page)
    await page.goto(`${ADRESSES.departementHttp}/des/messagerie?conversation=c-direct`)
    const direct = page.getByRole('region', { name: 'Aïcha Mahamat Nour' })

    await test.step('lecture.ENVOYE servie : une coche grise, dans le fil et devant l’extrait de la liste', async () => {
      await expect(bulle(direct, 'je le signe ce soir').getByRole('img', { name: 'Envoyé' })).toBeVisible()
      await expect(conversations(page).getByRole('button', { name: /Aïcha Mahamat Nour/ }).getByRole('img', { name: 'Envoyé' })).toBeVisible()
    })

    await test.step('collaboration.conversation-read.v1 poussé (acteur, luJusquA) : le message passe à « Lu »', async () => {
      gw.lire('c-direct', 'u-aicha')
      await expect(bulle(direct, 'je le signe ce soir').getByRole('img', { name: 'Lu' })).toBeVisible({ timeout: 15_000 })
      // Aucun détail « Lu par » en conversation directe.
      expect(gw.appels.filter((a) => a.chemin.endsWith('/lectures'))).toEqual([])
    })

    await test.step('Groupe : détail lu sur GET …/conversations/c-groupe/messages/g-2/lectures', async () => {
      await conversations(page).getByRole('button', { name: /Essais de LIA-2026-030/ }).click()
      const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
      const mien = bulle(fil, 'tenez-nous informés')
      await mien.getByRole('button', { name: 'Lu par 2 sur 3, voir qui l’a lu' }).click()
      const detail = mien.getByRole('dialog', { name: 'Lu par' })
      await expect(detail.getByText('Rachel Biyong Mengue')).toBeVisible()
      await expect(detail.getByText('heure inconnue')).toBeVisible()
      await expect(detail.getByRole('list', { name: 'Pas encore lu' })).toContainText('Aïcha Mahamat Nour')
      expect(gw.appels.filter((a) => a.chemin.endsWith('/lectures')).map((a) => `${a.methode} ${a.chemin}`))
        .toEqual(['GET /bff/des/collaboration/messagerie/conversations/c-groupe/messages/g-2/lectures'])
    })

    await test.step('Accusé de groupe : la page du fil et le détail ouvert sont relus, « Lu » par les trois', async () => {
      const lectures = gw.appels.filter((a) => a.chemin.endsWith('/lectures')).length
      gw.lire('c-groupe', 'u-aicha')
      const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
      const mien = bulle(fil, 'tenez-nous informés')
      await expect(mien.getByRole('button', { name: 'Lu, voir qui l’a lu' })).toBeVisible({ timeout: 15_000 })
      await expect(mien.getByRole('dialog', { name: 'Lu par' }).getByRole('list', { name: 'Pas encore lu' })).toHaveCount(0)
      expect(gw.appels.filter((a) => a.chemin.endsWith('/lectures')).length).toBeGreaterThan(lectures)
      // Aucun compteur de non-lus relu pour un accusé.
      expect(gw.appels.filter((a) => a.chemin.endsWith('/non-lus')).length).toBeLessThanOrEqual(2)
    })
  })
})
