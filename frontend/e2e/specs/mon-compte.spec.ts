import { mkdirSync } from 'node:fs'
import { createRequire } from 'node:module'
import { join } from 'node:path'
import { deflateSync } from 'node:zlib'
import type { Page } from '@playwright/test'
import { ADRESSES, demo, expect, test } from '../fixtures'
import { naviguer } from './outils'
import { CONNEXION_E2E, Passerelle, PERMISSIONS_SUPER_ADMIN } from './passerelle'

// Fonctionnalité : « Mon compte », photo de profil, appareils connectés et historique des connexions
// (docs/lot1/mon-compte.md §1 à §4). Photo choisie, contrôlée, recadrée au clavier, envoyée, affichée dans le
// menu utilisateur et la messagerie ; sessions fermées en modale CRUD standard ; historique paginé ; contrôles
// axe en clair et en sombre ; routes du contrat sur la passerelle simulée. GIMAC_CAPTURES : captures 1440 × 900.

const CAPTURES = process.env.GIMAC_CAPTURES
const axe = createRequire(import.meta.url).resolve('axe-core/axe.min.js')

async function capturer(page: Page, nom: string, complet = false) {
  if (!CAPTURES) return
  mkdirSync(CAPTURES, { recursive: true })
  if (!complet) {
    await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
    return
  }
  // Coquille fixe : seule la zone de contenu défile ; la page entière est capturée en agrandissant la fenêtre.
  const hauteur = await page.evaluate(() => document.querySelector('.gm-shell-contenu')!.scrollHeight + 56)
  await page.setViewportSize({ width: 1440, height: Math.max(900, hauteur) })
  await page.screenshot({ path: join(CAPTURES, `${nom}.png`) })
  await page.setViewportSize({ width: 1440, height: 900 })
}

async function auditer(page: Page, portee?: string) {
  // Animations d'apparition (fondu des modales) terminées : sinon le contraste est mesuré en cours de fondu.
  await page.evaluate(() => Promise.all(document.getAnimations().filter((a) => a.effect?.getComputedTiming().iterations !== Infinity).map((a) => a.finished.catch(() => undefined))))
  await page.addScriptTag({ path: axe })
  const violations = await page.evaluate(async (portee) => {
    const w = window as unknown as { axe: { run: (c: unknown, o: unknown) => Promise<{ violations: { id: string; nodes: { html: string }[] }[] }> } }
    const r = await w.axe.run({ include: portee ? [[portee]] : [document], exclude: [['.gm-filter-label']] }, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'] } })
    return r.violations.map((v) => `${v.id} : ${v.nodes.slice(0, 3).map((n) => n.html.slice(0, 140)).join(' | ')}`)
  }, portee)
  expect(violations).toEqual([])
}

/** Image PNG réelle (paysage : ciel, colline), décodable par le navigateur. */
function png(largeur: number, hauteur: number): Buffer {
  const crc = (b: Buffer) => {
    let c = ~0
    for (const o of b) {
      c ^= o
      for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1))
    }
    return ~c >>> 0
  }
  const bloc = (type: string, donnees: Buffer) => {
    const t = Buffer.concat([Buffer.from(type), donnees])
    const l = Buffer.alloc(4)
    l.writeUInt32BE(donnees.length)
    const c = Buffer.alloc(4)
    c.writeUInt32BE(crc(t))
    return Buffer.concat([l, t, c])
  }
  const lignes = Buffer.alloc((largeur * 3 + 1) * hauteur)
  for (let y = 0; y < hauteur; y++) {
    const o = y * (largeur * 3 + 1)
    for (let x = 0; x < largeur; x++) {
      const colline = y > hauteur * 0.55 + Math.sin(x / 40) * 25
      const soleil = (x - largeur * 0.3) ** 2 + (y - hauteur * 0.3) ** 2 < (hauteur * 0.12) ** 2
      const [r, g, b] = soleil ? [243, 210, 122] : colline ? [79, 122 + (y % 20), 74] : [156, 195, 213 - Math.floor(y / 8)]
      lignes.set([r, g, b], o + 1 + x * 3)
    }
  }
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(largeur, 0)
  ihdr.writeUInt32BE(hauteur, 4)
  ihdr.set([8, 2, 0, 0, 0], 8)
  return Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]), bloc('IHDR', ihdr), bloc('IDAT', deflateSync(lignes)), bloc('IEND', Buffer.alloc(0))])
}

const carte = (page: Page, titre: string) => page.locator('section.gm-card').filter({ has: page.getByRole('heading', { name: titre, exact: true }) })

test.describe('Fonctionnalité : photo de profil, appareils connectés et historique (Mon compte)', () => {
  for (const [theme, suffixe] of [['light', 'clair'], ['dark', 'sombre']] as const) {
    test(`Scénario : le chef du DES gère sa photo, ses appareils et consulte son historique (thème ${theme})`, async ({ page }) => {
      await demo(page, { profil: 'ROLE_CHEF_DES', theme })
      await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
      await naviguer(page, 'Mon compte')
      await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
      await expect(carte(page, 'Appareils connectés').getByText('Cet appareil')).toBeVisible()
      await expect(carte(page, 'Historique des connexions').getByText('Réussie').first()).toBeVisible()
      await capturer(page, `mon-compte-complet-${suffixe}`, true)
      await auditer(page)

      await test.step('Quand il choisit une image qui n’en est pas une, le refus est expliqué', async () => {
        await carte(page, 'Photo de profil').getByRole('button', { name: /Ajouter une photo|Changer ma photo/ }).click()
        const d = page.getByRole('dialog')
        await expect(d.getByRole('button', { name: 'Enregistrer la photo' })).toBeDisabled()
        await capturer(page, `photo-choix-${suffixe}`)
        await d.locator('input[type="file"]').setInputFiles({ name: 'photo.png', mimeType: 'image/png', buffer: Buffer.from('%PDF-1.7 pas une image') })
        await expect(d.getByText(/Format refusé : seules les images PNG, JPEG et WebP/)).toBeVisible()
      })

      await test.step('Puis il choisit une photo, la cadre au clavier et l’enregistre', async () => {
        const d = page.getByRole('dialog')
        await d.locator('input[type="file"]').setInputFiles({ name: 'paysage.png', mimeType: 'image/png', buffer: png(480, 320) })
        const cadre = d.getByRole('group', { name: 'Cadrer la photo de profil' })
        await expect(cadre).toBeVisible()
        await expect(d.getByText('480 × 320 px', { exact: false })).toBeVisible()
        await cadre.focus()
        await page.keyboard.press('+')
        await page.keyboard.press('+')
        await page.keyboard.press('ArrowLeft')
        await expect(d.getByLabel('Zoom')).toHaveValue('1.2')
        await auditer(page, '[role="dialog"]')
        await capturer(page, `photo-recadrage-${suffixe}`)
        await d.getByRole('button', { name: 'Enregistrer la photo' }).click()
        await expect(page.getByText('Votre photo de profil est enregistrée.')).toBeVisible()
        await expect(carte(page, 'Photo de profil').locator('.gm-avatar-xl img')).toBeVisible()
      })

      await test.step('Alors sa photo apparaît dans le menu utilisateur de la barre du haut', async () => {
        const menu = page.getByRole('button', { name: /^Menu utilisateur :/ })
        await expect(menu.locator('img')).toBeVisible()
        await menu.click()
        await expect(page.getByRole('menuitem', { name: 'Mon compte' })).toBeVisible()
        await expect(page.locator('.gm-topbar-menu .gm-avatar-lg img')).toBeVisible()
        await capturer(page, `menu-utilisateur-${suffixe}`)
        await page.keyboard.press('Escape')
        await expect(page.getByRole('menu')).toHaveCount(0)
      })

      await test.step('Et il ferme une session d’un autre appareil, après confirmation', async () => {
        const appareils = carte(page, 'Appareils connectés')
        await appareils.scrollIntoViewIfNeeded()
        await expect(appareils.getByRole('row', { name: /Cet appareil/ }).getByRole('button')).toHaveCount(0)
        await capturer(page, `appareils-${suffixe}`)
        await appareils.getByRole('row', { name: /Firefox 131/ }).getByRole('button', { name: 'Fermer la session' }).click()
        const d = page.getByRole('alertdialog')
        await expect(d.getByText('Ordinateur · Firefox 131 · Ubuntu 24.04')).toBeVisible()
        await auditer(page, '[role="alertdialog"]')
        await capturer(page, `fermeture-session-${suffixe}`)
        await d.getByRole('button', { name: 'Fermer la session' }).click()
        await expect(page.getByText('La session a été fermée : l’appareil est déconnecté.')).toBeVisible()
        await expect(appareils.getByRole('row', { name: /Firefox 131/ })).toHaveCount(0)
        await appareils.getByRole('button', { name: 'Fermer les autres sessions' }).click()
        await expect(page.getByRole('alertdialog').getByText('3 autres sessions seront fermées. Cet appareil reste connecté.')).toBeVisible()
        await capturer(page, `fermeture-autres-sessions-${suffixe}`)
        await page.getByRole('alertdialog').getByRole('button', { name: 'Fermer les autres sessions' }).click()
        await expect(page.getByText('Vos autres sessions ont été fermées. Cet appareil reste connecté.')).toBeVisible()
        await expect(appareils.getByRole('button', { name: 'Fermer les autres sessions' })).toBeDisabled()
      })

      await test.step('Et il parcourt l’historique de ses connexions, page par page', async () => {
        const h = carte(page, 'Historique des connexions')
        await h.scrollIntoViewIfNeeded()
        await expect(h.getByText(/Page 1 sur 4/)).toBeVisible()
        await capturer(page, `historique-${suffixe}`)
        await h.getByRole('button', { name: /Suivante/ }).click()
        await expect(h.getByText(/Page 2 sur 4/)).toBeVisible()
        await expect(h.getByText('Bloquée (trop de tentatives)')).toBeVisible()
      })

      await test.step('Et sa photo et celles de ses collègues figurent dans la messagerie', async () => {
        // L'entrée porte la pastille des non-lus : nom accessible « Messagerie » suivi du compteur.
        await page.getByRole('navigation', { name: 'Navigation principale' }).getByRole('link', { name: /^Messagerie/ }).click()
        const liste = page.getByRole('list', { name: 'Conversations' })
        await expect(liste.locator('.gm-avatar img').first()).toBeVisible()
        // État vide sur le fond texturé du fil (motif de trame atténué, D-66), le texte sur fond plein.
        await expect(page.locator('.gm-messagerie-fil.gm-trame-fil').getByText('Choisissez une conversation')).toBeVisible()
        await capturer(page, `messagerie-vide-${suffixe}`)
        await liste.getByRole('button', { name: /Essais de LIA-2026-030/ }).click()
        const fil = page.getByRole('region', { name: 'Essais de LIA-2026-030' })
        await expect(fil.locator('.gm-msg-meta .gm-avatar img').first()).toBeVisible()
        await expect(fil.locator('.gm-fil-fond.gm-trame-fil')).toBeVisible()
        await auditer(page)
        await capturer(page, `messagerie-${suffixe}`)
        await page.getByRole('button', { name: 'Nouveau message' }).click()
        const dest = page.getByRole('combobox', { name: 'Destinataires' })
        await dest.fill('Rachel')
        const option = page.getByRole('option', { name: /Rachel Biyong Mengue/ })
        await expect(option.locator('.gm-avatar img')).toBeVisible()
        await option.click()
        await expect(page.locator('.gm-tag-personne img')).toBeVisible()
        await dest.fill('Serge')
        await expect(page.getByRole('option', { name: /Bertrand Nguema/ })).toHaveCount(0)
        await expect(page.getByRole('option', { name: /Serge Ondoua/ }).locator('.gm-avatar')).toHaveText('SO')
        await capturer(page, `messagerie-destinataires-${suffixe}`)
      })

      await test.step('Enfin il retire sa photo : ses initiales reviennent', async () => {
        await naviguer(page, 'Mon compte')
        await carte(page, 'Photo de profil').getByRole('button', { name: 'Retirer ma photo' }).click()
        await page.getByRole('alertdialog').getByRole('button', { name: 'Retirer ma photo' }).click()
        await expect(page.getByText('Votre photo a été retirée : vos initiales s’affichent à sa place.')).toBeVisible()
        await expect(page.getByRole('button', { name: /^Menu utilisateur :/ }).locator('img')).toHaveCount(0)
      })
    })
  }

  test('Scénario : une photo de téléphone (JPEG, EXIF Orientation = 6) n’arrive jamais tournée', async ({ page }) => {
    await demo(page, { profil: 'ROLE_CHEF_DES' })
    await page.goto(`${ADRESSES.departement}/des/tableau-de-bord`)
    await naviguer(page, 'Mon compte')
    // JPEG paysage 320 × 160 (moitié gauche rouge, droite bleue) marqué « tourner de 90° » : vu droit, 160 × 320,
    // rouge en haut, bleu en bas. Le segment EXIF (APP1) est inséré juste après le marqueur de début d'image.
    const octets = await page.evaluate(async () => {
      const c = document.createElement('canvas')
      c.width = 320
      c.height = 160
      const x = c.getContext('2d')!
      x.fillStyle = 'rgb(220, 30, 30)'
      x.fillRect(0, 0, 160, 160)
      x.fillStyle = 'rgb(30, 30, 220)'
      x.fillRect(160, 0, 160, 160)
      const b = await new Promise<Blob>((r) => c.toBlob((v) => r(v!), 'image/jpeg', 0.95))
      return Array.from(new Uint8Array(await b.arrayBuffer()))
    })
    const exif = [0xff, 0xe1, 0x00, 0x22, 0x45, 0x78, 0x69, 0x66, 0, 0, 0x4d, 0x4d, 0, 0x2a, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, 6, 0, 0, 0, 0, 0, 0]
    const jpeg = Buffer.from([...octets.slice(0, 2), ...exif, ...octets.slice(2)])
    await carte(page, 'Photo de profil').getByRole('button', { name: /Ajouter une photo|Changer ma photo/ }).click()
    const d = page.getByRole('dialog')
    await d.locator('input[type="file"]').setInputFiles({ name: 'telephone.jpg', mimeType: 'image/jpeg', buffer: jpeg })
    await expect(d.getByText(/160 × 320 px/)).toBeVisible()
    await d.getByRole('button', { name: 'Enregistrer la photo' }).click()
    await expect(page.getByText('Votre photo de profil est enregistrée.')).toBeVisible()
    // Image envoyée (recadrée, carré central de l'image redressée) : rouge en haut, bleu en bas.
    const [haut, bas] = await page.locator('.gm-avatar-xl img').evaluate(async (img: HTMLImageElement) => {
      await img.decode()
      const c = document.createElement('canvas')
      c.width = img.naturalWidth
      c.height = img.naturalHeight
      const x = c.getContext('2d')!
      x.drawImage(img, 0, 0)
      const px = (py: number) => Array.from(x.getImageData(c.width / 2, py, 1, 1).data.slice(0, 3))
      return [px(c.height * 0.1), px(c.height * 0.9)]
    })
    expect(haut[0]).toBeGreaterThan(150)
    expect(haut[2]).toBeLessThan(100)
    expect(bas[2]).toBeGreaterThan(150)
    expect(bas[0]).toBeLessThan(100)
  })

  test('Scénario : la console affiche les photos dans la liste et la fiche des comptes', async ({ page }) => {
    await demo(page, { profil: 'ROLE_SUPER_ADMIN' })
    await page.goto(`${ADRESSES.console}/console/comptes`)
    await expect(page.getByRole('row', { name: /Rachel Biyong Mengue/ }).locator('.gm-avatar img')).toBeVisible()
    await expect(page.getByRole('row', { name: /Kevin Ateba Owona/ }).locator('.gm-avatar')).toHaveText('KA')
    await capturer(page, 'console-comptes-clair')
    await page.getByRole('link', { name: 'r.biyong' }).click()
    await expect(page.locator('.gm-objhead .gm-avatar-xl img')).toBeVisible()
    await capturer(page, 'console-compte-clair')
  })

  test('Scénario : routes du contrat sur la passerelle (photo multipart, appareils, historique)', async ({ page }) => {
    const gw = new Passerelle({ session: { permissions: PERMISSIONS_SUPER_ADMIN } })
    await gw.brancher(page)
    await page.goto(`${ADRESSES.consoleHttp}/console/mon-compte`)
    await expect(page.getByRole('heading', { level: 1, name: 'Mon compte' })).toBeVisible()
    await page.getByRole('button', { name: 'Ajouter une photo' }).click()
    await page.getByRole('dialog').locator('input[type="file"]').setInputFiles({ name: 'portrait.png', mimeType: 'image/png', buffer: png(320, 320) })
    await expect(page.getByRole('dialog').getByRole('group', { name: 'Cadrer la photo de profil' })).toBeVisible()
    await page.getByRole('dialog').getByRole('button', { name: 'Enregistrer la photo' }).click()
    await expect(page.getByText('Votre photo de profil est enregistrée.')).toBeVisible()
    const depot = gw.appels.find((a) => a.chemin === '/bff/console/iam/moi/avatar' && a.methode === 'PUT')!
    expect(depot.entetes['content-type']).toMatch(/^multipart\/form-data; boundary=/)
    // Avatar versionné servi par la passerelle, dans la page et le menu utilisateur.
    await expect(page.getByRole('button', { name: /^Menu utilisateur :/ }).locator('img')).toHaveAttribute('src', '/bff/console/iam/avatars/u-super-admin?v=e2e1')
    await page.getByRole('row', { name: /Firefox 131/ }).getByRole('button', { name: 'Fermer la session' }).click()
    await page.getByRole('alertdialog').getByRole('button', { name: 'Fermer la session' }).click()
    await expect(page.getByText('La session a été fermée : l’appareil est déconnecté.')).toBeVisible()
    await page.getByRole('button', { name: 'Fermer les autres sessions' }).click()
    await page.getByRole('alertdialog').getByRole('button', { name: 'Fermer les autres sessions' }).click()
    await expect(page.getByText('Vos autres sessions ont été fermées. Cet appareil reste connecté.')).toBeVisible()
    const h = carte(page, 'Historique des connexions')
    await h.getByRole('button', { name: /Suivante/ }).click()
    await expect(h.getByText(/Page 2 sur 2/)).toBeVisible()
    await page.getByRole('button', { name: 'Changer mon mot de passe' }).click()
    const d = page.getByRole('dialog')
    await d.locator('input[name="motDePasseActuel"]').fill(CONNEXION_E2E.motDePasse)
    await d.locator('input[name="nouveauMotDePasse"]').fill('Nouveau-Secret-2026!')
    await d.locator('input[name="confirmationMotDePasse"]').fill('Nouveau-Secret-2026!')
    await d.getByLabel('Fermer mes autres sessions (tous les appareils sauf celui-ci)').check()
    await d.getByRole('button', { name: 'Enregistrer le mot de passe' }).click()
    await expect(page.getByText('Votre mot de passe a été changé et vos autres sessions ont été fermées.')).toBeVisible()
    await page.getByRole('button', { name: 'Retirer ma photo' }).click()
    await page.getByRole('alertdialog').getByRole('button', { name: 'Retirer ma photo' }).click()
    await expect(page.getByText('Votre photo a été retirée : vos initiales s’affichent à sa place.')).toBeVisible()
    const routes = gw.appels.map((a) => `${a.methode} ${a.chemin}${a.recherche}`)
    expect(routes).toEqual(expect.arrayContaining([
      'GET /api/session/appareils', 'DELETE /api/session/appareils/app-ancienne', 'DELETE /api/session/appareils',
      'GET /api/session/historique?page=0&taille=20', 'GET /api/session/historique?page=1&taille=20', 'DELETE /bff/console/iam/moi/avatar',
    ]))
    const mdp = gw.appels.find((a) => a.chemin === '/bff/console/iam/moi/mot-de-passe')!
    expect(mdp.corps).toMatchObject({ fermerAutresSessions: true })
  })
})
