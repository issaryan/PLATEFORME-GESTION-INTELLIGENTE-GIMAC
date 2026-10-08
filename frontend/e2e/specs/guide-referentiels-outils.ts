import { mkdirSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import type { Locator, Page } from '@playwright/test'
import { capturePleinePage } from './outils'

// Aides du guide visuel guide-referentiels.spec.ts : capture numérotée par thème (NN-sujet-clair.png,
// NN-sujet-sombre.png), éléments utiles mis en évidence le temps de la capture, liaison de deux cartes d'un canevas.

export type Theme = 'light' | 'dark'

const ICI = dirname(fileURLToPath(import.meta.url))
export const DOSSIER_GUIDE = process.env.GIMAC_CAPTURES ? join(process.env.GIMAC_CAPTURES, 'guide-referentiels') : resolve(ICI, '..', 'captures', 'guide-referentiels')

/** Contour de mise en évidence : orange franc, lisible sur les deux thèmes ; posé puis retiré autour de la capture. */
const STYLE = `[data-guide-surligne]{outline:3px solid #f08c00 !important;outline-offset:3px !important;box-shadow:0 0 0 6px rgba(240,140,0,.28) !important;border-radius:4px}`

export async function capturerGuide(page: Page, nom: string, theme: Theme, o: { cibles?: Locator[]; pleinePage?: boolean } = {}) {
  mkdirSync(DOSSIER_GUIDE, { recursive: true })
  const chemin = join(DOSSIER_GUIDE, `${nom}-${theme === 'light' ? 'clair' : 'sombre'}.png`)
  await page.evaluate((css) => {
    if (document.getElementById('gm-guide-style')) return
    const s = document.createElement('style')
    s.id = 'gm-guide-style'
    s.textContent = css
    document.head.appendChild(s)
  }, STYLE)
  for (const c of o.cibles ?? []) await c.evaluate((e) => e.setAttribute('data-guide-surligne', ''))
  // Animations finies (ouverture des modales, toasts) ; les animations infinies (indicateur temps réel) sont ignorées.
  await page.evaluate(() => Promise.all(document.getAnimations().filter((a) => a.effect?.getComputedTiming().iterations !== Infinity).map((a) => a.finished.catch(() => undefined))))
  await page.waitForTimeout(150)
  if (o.pleinePage) await capturePleinePage(page, chemin)
  else await page.screenshot({ path: chemin })
  await page.evaluate(() => document.querySelectorAll('[data-guide-surligne]').forEach((e) => e.removeAttribute('data-guide-surligne')))
}

/** Ferme les notifications éphémères (toasts) qui masqueraient une partie de la capture. */
export async function fermerToasts(page: Page) {
  const boutons = page.locator('.gm-toast button, [role="status"] button[aria-label*="Fermer"]')
  for (let i = await boutons.count(); i > 0; i--) await boutons.nth(i - 1).click().catch(() => undefined)
}

const carte = (page: Page, code: string) => page.locator(`.react-flow__node[data-id="${code}"]`)

/** Amène deux cartes au centre du canevas et zoome tant qu'elles y tiennent (poignées assez grandes pour la souris). */
export async function rapprocher(page: Page, testId: string, a: string, b: string, largeurMax = 220) {
  const zone = page.getByTestId(testId)
  await zone.scrollIntoViewIfNeeded()
  const boites = async () => {
    const [z, ba, bb] = await Promise.all([zone.boundingBox(), carte(page, a).boundingBox(), carte(page, b).boundingBox()])
    if (!z || !ba || !bb) throw new Error('cartes introuvables')
    return { z, ba, bb, mx: (ba.x + ba.width / 2 + bb.x + bb.width / 2) / 2, my: (ba.y + ba.height / 2 + bb.y + bb.height / 2) / 2 }
  }
  const r0 = await boites()
  const cx = r0.z.x + r0.z.width / 2
  const cy = r0.z.y + r0.z.height / 2
  await page.mouse.move(r0.z.x + 12, r0.z.y + 12)
  await page.mouse.down()
  await page.mouse.move(r0.z.x + 12 + (cx - r0.mx), r0.z.y + 12 + (cy - r0.my), { steps: 10 })
  await page.mouse.up()
  for (let i = 0; i < 12; i++) {
    const r = await boites()
    if (r.ba.width >= largeurMax) break
    const f = 1.4
    const dedans = [r.ba, r.bb].every((x) => {
      const g = r.mx + (x.x - r.mx) * f
      const d = r.mx + (x.x + x.width - r.mx) * f
      const h = r.my + (x.y - r.my) * f
      const bas = r.my + (x.y + x.height - r.my) * f
      return g > r.z.x + 16 && d < r.z.x + r.z.width - 16 && h > r.z.y + 16 && bas < r.z.y + r.z.height - 16
    })
    if (!dedans) break
    await page.mouse.move(r.mx, r.my)
    await page.mouse.wheel(0, -150)
    await page.waitForTimeout(250)
  }
}

/** Tire un arc de la sortie d'une carte vers l'entrée d'une autre, à la souris. */
export async function relierCartes(page: Page, testId: string, de: string, vers: string) {
  await rapprocher(page, testId, de, vers)
  const depart = await carte(page, de).locator('.react-flow__handle.source').boundingBox()
  const arrivee = await carte(page, vers).locator('.react-flow__handle.target').boundingBox()
  if (!depart || !arrivee) throw new Error('poignées introuvables')
  await page.mouse.move(depart.x + depart.width / 2, depart.y + depart.height / 2)
  await page.mouse.down()
  await page.mouse.move((depart.x + arrivee.x) / 2, (depart.y + arrivee.y) / 2, { steps: 5 })
  await page.mouse.move(arrivee.x + arrivee.width / 2, arrivee.y + arrivee.height / 2, { steps: 8 })
  await page.mouse.up()
}

/** Choix d'une valeur dans une liste à choix multiples avec suggestions (MultiSelect) : saisie, puis option. */
export async function choisirMultiple(champ: Locator, option: string, nom: string | RegExp = option) {
  await champ.click()
  await champ.fill(option.slice(0, 4))
  await champ.page().getByRole('listbox').getByRole('option', { name: nom, exact: typeof nom === 'string' }).first().click()
}

/** Zoome à la molette sur une carte du canevas (la carte reste sous le pointeur) jusqu'à la largeur voulue. */
export async function zoomerSur(page: Page, testId: string, code: string, largeur = 180, position: [number, number] = [0.5, 0.5]) {
  await page.getByTestId(testId).scrollIntoViewIfNeeded()
  for (let i = 0; i < 15; i++) {
    const b = await carte(page, code).boundingBox()
    if (!b || b.width >= largeur) break
    await page.mouse.move(b.x + b.width / 2, b.y + b.height / 2)
    await page.mouse.wheel(0, -200)
    await page.waitForTimeout(200)
  }
  // Panoramique : la carte amenée à la position voulue du canevas (fractions de largeur et de hauteur).
  const z = await page.getByTestId(testId).boundingBox()
  const b = await carte(page, code).boundingBox()
  if (!z || !b) return
  await page.mouse.move(z.x + 12, z.y + 12)
  await page.mouse.down()
  await page.mouse.move(z.x + 12 + (z.x + z.width * position[0] - b.x - b.width / 2), z.y + 12 + (z.y + z.height * position[1] - b.y - b.height / 2), { steps: 10 })
  await page.mouse.up()
  await page.waitForTimeout(300)
}
