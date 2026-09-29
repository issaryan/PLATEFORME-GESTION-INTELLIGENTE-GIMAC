import axe from 'axe-core'
import { expect } from 'vitest'

// Audit axe des écrans du parcours (WCAG 2.0 à 2.2, A et AA), comme les aperçus du catalogue : le contraste
// est vérifié depuis les jetons (jsdom ne calcule pas les couleurs) ; les règles de page entière sont hors
// sujet pour une page montée sans l'enveloppe.
const HORS_SUJET = ['color-contrast', 'region', 'landmark-one-main', 'page-has-heading-one', 'landmark-unique', 'landmark-complementary-is-top-level', 'heading-order']

export async function axeSansViolation(racine: Element) {
  const res = await axe.run(racine, {
    runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'] },
    rules: Object.fromEntries(HORS_SUJET.map((r) => [r, { enabled: false }])),
  })
  expect(res.violations.map((v) => `${v.id} (${v.nodes.length}) : ${v.nodes.slice(0, 2).map((n) => n.html.slice(0, 140)).join(' | ')}`)).toEqual([])
}
