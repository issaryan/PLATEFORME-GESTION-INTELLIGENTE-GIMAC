import { mkdirSync } from 'node:fs'
import { join } from 'node:path'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { naviguer } from './outils'

// Fonctionnalité : coquille applicative fixe (DS-GIMAC-01 §6.2, demande du 30/09/2026). La page ne défile
// pas ; la barre supérieure et la navigation latérale restent en place, la navigation occupe toute la
// hauteur de la fenêtre, du haut jusqu'en bas, sans jamais s'étirer avec le contenu ; la barre supérieure
// et le pied de page sont à sa droite, sur la largeur du contenu (02/10/2026) ; seule la zone de contenu défile, le pied de
// page en bas de cette zone ; au clic d'une entrée, seule la zone de contenu change et revient en haut.
// GIMAC_CAPTURES=<dossier> enregistre les captures de chaque état.

const CAPTURES = process.env.GIMAC_CAPTURES
async function capturer(page: Page, nom: string) {
  if (!CAPTURES) return
  mkdirSync(CAPTURES, { recursive: true })
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
}

/** Mesures de la coquille : défilement de la page et de la zone de contenu, boîtes de la navigation et du pied. */
async function mesurer(page: Page) {
  return page.evaluate(() => {
    const zone = document.querySelector<HTMLElement>('.gm-shell-contenu')!
    const nav = document.querySelector('nav.gm-sidenav')?.getBoundingClientRect()
    const barre = document.querySelector('.gm-topbar')!.getBoundingClientRect()
    const pied = document.querySelector('footer.gm-footer')!.getBoundingClientRect()
    const entrees = document.querySelector<HTMLElement>('.gm-sidenav-entrees')
    return {
      pageDefile: document.scrollingElement!.scrollHeight - window.innerHeight,
      hauteur: window.innerHeight,
      zoneDefilable: zone.scrollHeight - zone.clientHeight,
      zoneHaut: zone.scrollTop,
      nav: nav ? { haut: Math.round(nav.top), bas: Math.round(nav.bottom), hauteur: Math.round(nav.height), droite: Math.round(nav.right) } : null,
      barre: { haut: Math.round(barre.top), bas: Math.round(barre.bottom), gauche: Math.round(barre.left) },
      pied: { haut: Math.round(pied.top), bas: Math.round(pied.bottom), gauche: Math.round(pied.left) },
      piedDansZone: zone.contains(document.querySelector('footer.gm-footer')),
      entreesDefilables: entrees ? entrees.scrollHeight - entrees.clientHeight : 0,
    }
  })
}

test.describe('Fonctionnalité : coquille applicative fixe', () => {
  test('Scénario : page longue, seule la zone de contenu défile, la navigation ne bouge pas', async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DES' })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await expect(page.getByRole('region', { name: 'Mon travail' })).toBeVisible()
    await expect.poll(async () => (await mesurer(page)).zoneDefilable, { timeout: 15_000 }).toBeGreaterThan(200)
    const avant = await mesurer(page)
    await test.step('Alors la page ne défile pas, la zone de contenu défile, le pied de page est dans la zone', async () => {
      expect(avant.pageDefile).toBe(0)
      expect(avant.zoneDefilable).toBeGreaterThan(200)
      expect(avant.piedDansZone).toBe(true)
      expect(avant.barre.haut).toBe(0)
      // Navigation du haut jusqu'en bas ; barre supérieure et pied de page à sa droite.
      expect(avant.nav!.haut).toBe(0)
      expect(avant.nav!.bas).toBe(avant.hauteur)
      expect(avant.barre.gauche).toBe(avant.nav!.droite)
      expect(avant.pied.gauche).toBe(avant.nav!.droite)
    })
    await capturer(page, 'coquille-longue-haut')
    await test.step('Quand il fait défiler le contenu jusqu’en bas', async () => {
      await page.mouse.move(900, 500)
      await page.mouse.wheel(0, 20_000)
      await expect.poll(async () => (await mesurer(page)).zoneHaut).toBeGreaterThan(200)
    })
    await test.step('Alors la barre et la navigation restent en place et le pied de page apparaît en bas de la zone', async () => {
      const apres = await mesurer(page)
      expect(apres.pageDefile).toBe(0)
      expect(apres.nav).toEqual(avant.nav)
      expect(apres.barre).toEqual(avant.barre)
      expect(apres.pied.bas).toBe(apres.hauteur)
      await expect(page.locator('footer.gm-footer')).toBeInViewport()
    })
    await capturer(page, 'coquille-longue-bas')
    await test.step('Quand il choisit une entrée de la navigation, seule la zone de contenu change et revient en haut', async () => {
      const nav = page.getByRole('navigation', { name: 'Navigation principale' })
      const poignee = await nav.elementHandle()
      await naviguer(page, 'Institutions membres')
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Institutions membres')
      const ici = await mesurer(page)
      expect(ici.zoneHaut).toBe(0)
      expect(ici.nav).toEqual(avant.nav)
      expect(await poignee!.evaluate((n) => n.isConnected)).toBe(true)
      await expect(nav.getByRole('link', { name: 'Institutions membres' })).toHaveAttribute('aria-current', 'page')
    })
    await capturer(page, 'coquille-apres-navigation')
  })

  test('Scénario : page courte, le pied de page reste en bas de la zone de contenu', async ({ page }) => {
    await demo(page, { profil: 'ROLE_SUPER_ADMIN' })
    await page.goto(`${ADRESSES.console}/console/plateforme/journalisation`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Journalisation des services')
    const m = await mesurer(page)
    expect(m.pageDefile).toBe(0)
    expect(m.zoneDefilable).toBe(0)
    expect(m.pied.bas).toBe(m.hauteur)
    expect(m.nav!.bas).toBe(m.hauteur)
    await capturer(page, 'coquille-courte-console')
  })

  test('Scénario : navigation plus haute que la fenêtre, elle défile seule sans s’étirer', async ({ page }) => {
    await demo(page, { profil: 'ROLE_RSSI' })
    await page.setViewportSize({ width: 1440, height: 560 })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await expect(page.getByRole('region', { name: 'Administration et sécurité des comptes' })).toBeVisible()
    const m = await mesurer(page)
    expect(m.pageDefile).toBe(0)
    expect(m.nav!.bas).toBe(560)
    expect(m.entreesDefilables).toBeGreaterThan(0)
    const entrees = page.locator('.gm-sidenav-entrees')
    await entrees.hover()
    await page.mouse.wheel(0, 2000)
    await expect.poll(() => entrees.evaluate((e) => e.scrollTop)).toBeGreaterThan(0)
    expect((await mesurer(page)).zoneHaut).toBe(0)
    await expect(page.getByRole('link', { name: 'Se déconnecter' })).toBeInViewport()
    await capturer(page, 'coquille-navigation-defilante')
  })

  test('Scénario : sur mobile, la navigation est un tiroir ouvert par le menu', async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DDRM' })
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tableau de bord')
    await expect(page.getByRole('navigation', { name: 'Navigation principale' })).toHaveCount(0)
    const m = await mesurer(page)
    expect(m.pageDefile).toBe(0)
    await capturer(page, 'coquille-mobile')
    await page.getByRole('button', { name: /Replier/ }).click()
    const nav = page.getByRole('navigation', { name: 'Navigation principale' })
    await expect(nav).toBeVisible()
    await capturer(page, 'coquille-mobile-tiroir')
    await nav.getByRole('link', { name: 'Institutions membres' }).click()
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Institutions membres')
    await expect(nav).toHaveCount(0)
    expect((await mesurer(page)).pageDefile).toBe(0)
  })

  test('Scénario : portail et console partagent la même coquille', async ({ page }) => {
    await page.goto(`${ADRESSES.portail}/portail/tableau-de-bord`)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mon espace de certification')
    const p = await mesurer(page)
    expect(p.pageDefile).toBe(0)
    expect(p.nav!.bas).toBe(p.hauteur)
    await capturer(page, 'coquille-portail')
    await demo(page, { profil: 'ROLE_SUPER_ADMIN' })
    await page.goto(`${ADRESSES.console}/console/tableau-de-bord`)
    await expect(page.getByRole('region', { name: 'Gestion des utilisateurs' })).toBeVisible()
    const c = await mesurer(page)
    expect(c.pageDefile).toBe(0)
    expect(c.zoneDefilable).toBeGreaterThan(0)
    await capturer(page, 'coquille-console')
    // Aucun nom de couche sous le logo de la navigation.
    await expect(page.locator('.gm-sidenav-marque')).not.toContainText(/console|application|portail/i)
  })

  // D-66 : barre latérale sombre en mode sombre, entrée active en texte et pictogramme blancs sur une
  // teinte, sans filet vertical ; fond uni bleu en clair. Contrôle sur le rendu réel des deux thèmes.
  for (const theme of ['light', 'dark'] as const) {
    test(`Scénario : barre latérale en thème ${theme}, entrée active lisible sans filet`, async ({ page }) => {
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      const active = page.locator('nav.gm-sidenav a[aria-current="page"]')
      await expect(active).toBeVisible()
      const m = await page.evaluate(() => {
        const nav = getComputedStyle(document.querySelector('nav.gm-sidenav')!)
        const a = document.querySelector<HTMLElement>('nav.gm-sidenav a[aria-current="page"]')!
        const sa = getComputedStyle(a)
        const icone = a.querySelector('svg') ? getComputedStyle(a.querySelector('svg')!).color : sa.color
        const page = getComputedStyle(document.querySelector('.gm-shell-contenu') ?? document.body).backgroundColor
        return { fond: nav.backgroundColor, image: nav.backgroundImage, texte: sa.color, icone, actif: sa.backgroundColor, filet: sa.borderLeftWidth, page }
      })
      expect(m.image).toBe('none')
      expect(m.texte).toBe('rgb(255, 255, 255)')
      expect(m.icone).toBe('rgb(255, 255, 255)')
      expect(m.filet).toBe('0px')
      expect(m.actif).not.toBe(m.fond)
      if (theme === 'dark') expect(m.fond).toBe('rgb(17, 19, 22)')
      else expect(m.fond).toBe('rgb(15, 61, 112)')
      await capturer(page, `barre-laterale-${theme === 'dark' ? 'sombre' : 'clair'}`)
    })
  }
})
