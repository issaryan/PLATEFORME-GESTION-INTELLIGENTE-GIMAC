// Chaque aperçu du catalogue se rend sans erreur et passe l'audit d'accessibilité axe
// (WCAG 2.0, 2.1 et 2.2 A et AA). Le contraste est vérifié à part, depuis les jetons, car
// jsdom ne calcule pas les couleurs ; les règles propres à une page entière sont hors sujet
// pour un composant isolé.
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render } from '@testing-library/react'
import axe from 'axe-core'
import type { ComponentType } from 'react'

const apercus = import.meta.glob<{ default: ComponentType }>('./apercus/*.tsx', { eager: true })
const HORS_SUJET = ['color-contrast', 'region', 'landmark-one-main', 'page-has-heading-one', 'landmark-unique', 'landmark-complementary-is-top-level', 'heading-order']

afterEach(cleanup)

describe.each(['light', 'dark'])('aperçus en thème %s', (theme) => {
  it.each(Object.entries(apercus).map(([chemin, m]) => [chemin.replace(/^.*\/(.*)\.tsx$/, '$1'), m.default] as const))(
    '%s : rendu et axe sans violation',
    async (_nom, Apercu) => {
      const { container } = render(<div className="gm-root gm-pad" data-theme={theme}><Apercu /></div>)
      expect(container.firstElementChild?.childElementCount).toBeGreaterThan(0)
      const res = await axe.run(container, {
        runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'] },
        rules: Object.fromEntries(HORS_SUJET.map((r) => [r, { enabled: false }])),
      })
      const violations = res.violations.map((v) => `${v.id} (${v.nodes.length}) : ${v.nodes.slice(0, 2).map((n) => n.html.slice(0, 120)).join(' | ')}`)
      expect(violations).toEqual([])
    },
  )
})
