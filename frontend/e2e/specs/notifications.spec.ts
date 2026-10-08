import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { Passerelle, PERMISSIONS_SUPER_ADMIN } from './passerelle'

// Fonctionnalité : notifications de la plateforme (D-80, docs/lot1/notifications.md §4). Cloche de la barre du
// haut (pastille des non-lues, panneau, « Tout marquer comme lu », « Voir tout »), fenêtre surgissante à
// l'arrivée d'un message (jamais pour la conversation ouverte), ouverture d'une conversation qui marque ses
// notifications lues, page des notifications, préférences dans « Mon compte ». Services simulés (réception
// simulée d'un message), puis console sur la passerelle (MS-NOTIFICATION et canal poussé simulés par Playwright).
// Captures clair et sombre dans e2e/captures/notifications (ou $GIMAC_CAPTURES/notifications).

const ICI = dirname(fileURLToPath(import.meta.url))
const CAPTURES = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'notifications') : resolve(ICI, '..', 'captures', 'notifications')
async function capturer(page: Page, nom: string) {
  mkdirSync(CAPTURES, { recursive: true })
  await page.evaluate(() => Promise.all(document.getAnimations().map((a) => a.finished.catch(() => undefined))))
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}

const cloche = (page: Page) => page.getByRole('banner').getByRole('button', { name: /^Notifications/ })
const pastille = (page: Page) => page.locator('.gm-topbar-pastille')
const panneau = (page: Page) => page.getByRole('dialog', { name: 'Centre de notifications' })
const surgissantes = (page: Page) => page.getByRole('region', { name: 'Notifications reçues' })
const recevoir = (page: Page, o: { conversationId?: string; contenu?: string } = {}) =>
  page.evaluate((x) => (window as unknown as { gmDemoNotifications: { recevoirMessage(o: unknown): string } }).gmDemoNotifications.recevoirMessage(x), o)

for (const theme of ['light', 'dark'] as const) {
  const suffixe = theme === 'light' ? 'clair' : 'sombre'
  test.describe(`Fonctionnalité : notifications de la plateforme, services simulés (${suffixe})`, () => {
    test('Scénario : cloche, réception simulée, conversation ouverte, tout marquer comme lu, page et préférences', async ({ page, context }) => {
      await context.grantPermissions(['notifications'])
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)

      await test.step('Étant donné deux conversations et une tâche non lues, la cloche porte la pastille 3', async () => {
        await expect(pastille(page)).toHaveText('3')
        await expect(cloche(page)).toHaveAccessibleName('Notifications, 3 non lues')
        await expect(page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: 'Notifications', exact: true })).toBeVisible()
      })

      await test.step('Quand il ouvre la cloche : type, titre, sous-titre, aperçu du dernier message, nombre regroupé', async () => {
        await cloche(page).click()
        const p = panneau(page)
        await expect(p.getByText('3 non lues')).toBeVisible()
        const groupe = p.getByRole('button', { name: 'Message : Rachel Biyong Mengue, non lue' })
        await expect(groupe.getByText('Essais de LIA-2026-030')).toBeVisible()
        await expect(groupe.getByText('2 messages')).toBeVisible()
        await expect(groupe.getByText('Le blocage est déclaré sur la liaison ; échéance de levée au 27 septembre.')).toBeVisible()
        await expect(p.getByRole('button', { name: 'Message : Aïcha Mahamat Nour, non lue' }).getByText('Merci. Le participant signera ensuite depuis son portail.')).toBeVisible()
        await expect(p.getByRole('button', { name: 'Tâche : Tâche confiée par Bertrand Nguema Obiang, non lue' })).toBeVisible()
        await expect(p.getByRole('button', { name: 'Rapport : Rapport de Joël Ekotto Mballa' })).toBeVisible()
        await capturer(page, `cloche-ouverte-${suffixe}`)
      })

      await test.step('Et « Tout marquer comme lu » retire la pastille', async () => {
        await panneau(page).getByRole('button', { name: 'Tout marquer comme lu' }).click()
        await expect(pastille(page)).toHaveCount(0)
        await expect(panneau(page).getByText('0 non lue')).toBeVisible()
        await page.keyboard.press('Escape')
        await expect(panneau(page)).toHaveCount(0)
      })

      let conversation = ''
      await test.step('Quand un message arrive : fenêtre surgissante (titre, aperçu, « Ouvrir ») et pastille 1', async () => {
        conversation = await recevoir(page, { contenu: 'Le procès-verbal est prêt à signer, merci de le valider avant 17 h.' })
        const toast = surgissantes(page).locator('.gm-toast')
        await expect(toast).toHaveCount(1)
        await expect(toast.getByText('Aïcha Mahamat Nour')).toBeVisible()
        await expect(toast.getByText('Le procès-verbal est prêt à signer, merci de le valider avant 17 h.')).toBeVisible()
        await expect(pastille(page)).toHaveText('1')
        await capturer(page, `fenetre-surgissante-${suffixe}`)
      })

      await test.step('Et « Ouvrir » ouvre la conversation : ses notifications sont lues', async () => {
        await surgissantes(page).getByRole('button', { name: 'Ouvrir' }).click()
        await expect(page).toHaveURL(new RegExp(`/des/messagerie\\?conversation=${conversation}$`))
        await expect(page.getByRole('region', { name: 'Aïcha Mahamat Nour' }).getByText('Le procès-verbal est prêt à signer, merci de le valider avant 17 h.')).toBeVisible()
        await expect(pastille(page)).toHaveCount(0)
        await expect(surgissantes(page)).toHaveCount(0)
      })

      await test.step('Et un message dans la conversation ouverte ne surgit pas et reste lu', async () => {
        await recevoir(page, { conversationId: conversation, contenu: 'Je reste disponible jusqu’à 18 h.' })
        await expect(page.getByRole('region', { name: 'Aïcha Mahamat Nour' }).getByText('Je reste disponible jusqu’à 18 h.')).toBeVisible({ timeout: 15_000 })
        await page.waitForTimeout(800)
        await expect(surgissantes(page)).toHaveCount(0)
        await expect(pastille(page)).toHaveCount(0)
      })

      await test.step('Mais un message d’une autre conversation surgit pendant qu’un autre fil est ouvert', async () => {
        await page.getByRole('list', { name: 'Conversations' }).getByRole('button', { name: /Essais de LIA-2026-030/ }).click()
        await expect(page).not.toHaveURL(new RegExp(`conversation=${conversation}$`))
        await recevoir(page, { conversationId: conversation, contenu: 'Une dernière question sur le procès-verbal.' })
        await expect(surgissantes(page).getByText('Une dernière question sur le procès-verbal.')).toBeVisible()
        await expect(pastille(page)).toHaveText('1')
      })

      await test.step('Alors « Voir tout » mène à la page : non lues, puis toutes', async () => {
        await cloche(page).click()
        await panneau(page).getByRole('button', { name: 'Voir tout' }).click()
        await expect(page).toHaveURL(/\/des\/notifications$/)
        await expect(page.getByRole('heading', { level: 1 })).toHaveText('Notifications')
        const liste = page.getByRole('list', { name: 'Notifications', exact: true })
        await expect(liste.getByRole('button')).toHaveCount(1)
        await page.getByRole('tab', { name: 'Toutes' }).click()
        // Sept notifications de la plateforme et une d'homologation (MS-NOTIFICATION §11), lue, avec son pictogramme.
        await expect(liste.getByRole('button')).toHaveCount(8)
        await expect(liste.getByRole('button', { name: /^Homologation : Action attendue : Atelier comptabilisation/ })).toBeVisible()
        await liste.getByRole('button', { name: /^Message : Aïcha Mahamat Nour, non lue/ }).click()
        await expect(page).toHaveURL(new RegExp(`/des/messagerie\\?conversation=${conversation}$`))
        await expect(pastille(page)).toHaveCount(0)
      })

      await test.step('Et les préférences de Mon compte : types, notification du système autorisée, son', async () => {
        await cloche(page).click()
        await panneau(page).getByRole('button', { name: 'Préférences' }).click()
        await expect(page).toHaveURL(/\/des\/mon-compte#notifications$/)
        const section = page.getByRole('region', { name: 'Notifications', exact: true })
        await expect(section.getByText('Autorisées par le navigateur')).toBeVisible()
        // Bascule enregistrée aussitôt (PUT /preferences) ; l'état suit la réponse du service.
        await section.getByRole('switch', { name: 'Son discret à l’arrivée d’une notification' }).click()
        await expect(section.getByRole('switch', { name: 'Son discret à l’arrivée d’une notification' })).toBeChecked()
        await expect(section.getByText('Préférences de notification enregistrées.')).toBeVisible()
        await section.getByRole('switch', { name: 'Tâches qui me sont confiées' }).click()
        await expect(section.getByRole('switch', { name: 'Tâches qui me sont confiées' })).not.toBeChecked()
        // Homologation (MS-NOTIFICATION §11) : types.HOMOLOGATION, actif par défaut.
        await expect(section.getByRole('switch', { name: 'Homologation : opérations des liaisons à mener, valider ou signer' })).toBeChecked()
        await section.getByRole('switch', { name: 'Homologation : opérations des liaisons à mener, valider ou signer' }).click()
        await expect(section.getByRole('switch', { name: 'Homologation : opérations des liaisons à mener, valider ou signer' })).not.toBeChecked()
        await section.scrollIntoViewIfNeeded()
        await section.screenshot({ path: join(CAPTURES, `preferences-${suffixe}.png`) })
      })
    })
  })
}

test.describe('Fonctionnalité : notifications de la console sur la passerelle (MS-NOTIFICATION)', () => {
  test('Scénario : compteur et liste lus sur /bff/console/notification, événement poussé, lecture, préférences', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/tableau-de-bord`)
    const appels = (prefixe: string) => gw.appels.filter((a) => a.chemin.startsWith(prefixe))

    await test.step('Étant donné un message de la direction générale et une tâche non lus', async () => {
      await expect(pastille(page)).toHaveText('2')
      expect(appels('/bff/console/notification/notifications/compteur').length).toBeGreaterThan(0)
      const liste = appels('/bff/console/notification/notifications').find((a) => a.chemin === '/bff/console/notification/notifications')!
      expect(liste.recherche).toBe('?etat=toutes&page=0&taille=10')
      expect(liste.entetes['x-gimac-fond']).toBe('1')
    })

    await test.step('Quand un nouveau message est regroupé et poussé par le canal (sujet notification)', async () => {
      gw.recevoirMessage('Merci de confirmer la revue des habilitations avant vendredi midi.')
      const toast = surgissantes(page).locator('.gm-toast')
      await expect(toast.getByText('François Tchoumi Kamga (2)')).toBeVisible({ timeout: 15_000 })
      await expect(toast.getByText('Merci de confirmer la revue des habilitations avant vendredi midi.')).toBeVisible()
      expect(appels('/bff/des/')).toEqual([])
    })

    await test.step('Alors la cloche l’affiche et son ouverture mène à la messagerie de la console', async () => {
      await surgissantes(page).getByRole('button', { name: 'Fermer la notification' }).click()
      await cloche(page).click()
      await expect(panneau(page).getByRole('button', { name: 'Tâche : Tâche confiée par Carine Ebongue Ndjock, non lue' }).getByText('Échéance : 2026-10-09')).toBeVisible()
      const message = panneau(page).getByRole('button', { name: 'Message : François Tchoumi Kamga, non lue' })
      await expect(message.getByText('2 messages')).toBeVisible()
      await message.click()
      await expect(page).toHaveURL(/\/console\/messagerie\?conversation=c-dg$/)
      await expect.poll(() => appels('/bff/console/notification/notifications/n-1/lecture').map((a) => a.methode)).toContain('POST')
      await expect.poll(() => appels('/bff/console/notification/notifications/lecture').map((a) => a.corps))
        .toContainEqual({ reference: { service: 'collaboration', type: 'CONVERSATION', id: 'c-dg' } })
      await expect(pastille(page)).toHaveText('1')
    })

    await test.step('Et « Tout marquer comme lu » envoie une lecture sans corps', async () => {
      await cloche(page).click()
      await panneau(page).getByRole('button', { name: 'Tout marquer comme lu' }).click()
      await expect(pastille(page)).toHaveCount(0)
      expect(appels('/bff/console/notification/notifications/lecture').some((a) => a.corps === undefined)).toBe(true)
    })

    await test.step('Et les préférences sont enregistrées par PUT /preferences, champ changé seulement', async () => {
      await page.goto(`${ADRESSES.consoleHttp}/console/mon-compte#notifications`)
      const section = page.getByRole('region', { name: 'Notifications', exact: true })
      await section.getByRole('switch', { name: 'Rapports qui me sont soumis' }).click()
      await expect(section.getByRole('switch', { name: 'Rapports qui me sont soumis' })).not.toBeChecked()
      await expect(section.getByText('Préférences de notification enregistrées.')).toBeVisible()
      const put = appels('/bff/console/notification/preferences').filter((a) => a.methode === 'PUT')
      expect(put.map((a) => a.corps)).toEqual([{ types: { MESSAGE: true, TACHE: true, RAPPORT: false, MENTION: true, REPONSE: true } }])
      expect(gw.preferencesNotifications.types.RAPPORT).toBe(false)
    })
  })
})
