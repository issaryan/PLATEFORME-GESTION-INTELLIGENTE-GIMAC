/// <reference types="vite/client" />
// Chaque entrée du catalogue vivant (apps/catalogue/src/apercus) est un état documenté d'un composant :
// le système de conception les rend toutes, en clair et en sombre, dans sa propre suite (D-61, D-63).
// L'audit axe de ces aperçus reste dans la suite du catalogue.
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render } from '@testing-library/react'
import type { ComponentType } from 'react'

const apercus = import.meta.glob<{ default: ComponentType }>('../../../apps/catalogue/src/apercus/*.tsx', { eager: true })

afterEach(cleanup)

describe.each(['light', 'dark'])('catalogue vivant en thème %s', (theme) => {
  it.each(Object.entries(apercus).map(([chemin, m]) => [chemin.replace(/^.*\/(.*)\.tsx$/, '$1'), m.default] as const))('%s : rendu', (_nom, Apercu) => {
    const { container } = render(<div className="gm-root" data-theme={theme}><Apercu /></div>)
    expect(container.firstElementChild?.childElementCount).toBeGreaterThan(0)
  })
})
