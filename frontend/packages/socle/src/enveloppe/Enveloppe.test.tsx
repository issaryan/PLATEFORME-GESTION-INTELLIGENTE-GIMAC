import { afterEach, beforeAll, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { ThemeProvider } from '@gimac/design-system'
import { initI18n } from '../i18n/i18n'
import { Enveloppe } from './Enveloppe'

// D-62 : l'espace personnel (Mon compte, Se déconnecter) est présent dans les trois couches.
beforeAll(() => {
  initI18n([], 'fr')
  Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
})
afterEach(cleanup)

function Ici() {
  return <span data-testid="ici">{useLocation().pathname}</span>
}
const monter = (espacePersonnel?: string) =>
  render(
    <MemoryRouter initialEntries={['/console/tableau-de-bord']}>
      <ThemeProvider theme="light">
        <Enveloppe couche="console" navigation={[{ entrees: [{ cle: 'tb', libelleKey: 'x.tb', icone: 'gauge', chemin: '/console/tableau-de-bord' }] }]} espacePersonnel={espacePersonnel}>
          <Routes><Route path="*" element={<Ici />} /></Routes>
        </Enveloppe>
      </ThemeProvider>
    </MemoryRouter>,
  )

describe('Enveloppe : espace personnel', () => {
  it('groupe « Mon espace » : Mon compte et Se déconnecter, navigation sans rechargement', () => {
    monter('/console')
    expect(screen.getByText('Mon espace')).toBeTruthy()
    fireEvent.click(screen.getByRole('link', { name: 'Mon compte' }))
    expect(screen.getByTestId('ici').textContent).toBe('/console/mon-compte')
    expect(screen.getByRole('link', { name: 'Se déconnecter' }).getAttribute('href')).toBe('/console/deconnexion')
    expect(document.querySelector('.gm-footer')).toBeTruthy()
  })
  it('sans espace personnel : groupe absent', () => {
    monter()
    expect(screen.queryByText('Mon espace')).toBeNull()
  })
})

// Coquille fixe : la page ne défile pas ; seule la zone de contenu défile et change au clic d'une entrée
// de la navigation, son défilement revenant en haut ; le pied de page est en bas de cette zone.
const NAV = [
  {
    entrees: [
      { cle: 'tb', libelleKey: 'x.tb', icone: 'gauge' as const, chemin: '/console/tableau-de-bord' },
      { cle: 'cp', libelleKey: 'x.cp', icone: 'users' as const, chemin: '/console/comptes' },
    ],
  },
]
const coquille = () =>
  render(
    <MemoryRouter initialEntries={['/console/tableau-de-bord']}>
      <ThemeProvider theme="light">
        <Enveloppe couche="console" navigation={NAV} version="1.0.0">
          <Routes><Route path="*" element={<Ici />} /></Routes>
        </Enveloppe>
      </ThemeProvider>
    </MemoryRouter>,
  )

describe('Enveloppe : coquille fixe', () => {
  it('barre supérieure, navigation et zone de contenu séparées ; pied de page dans la zone de contenu', () => {
    const { container } = coquille()
    const racine = container.querySelector('.gm-shell')!
    expect(racine.classList.contains('gm-root')).toBe(true)
    expect(racine.querySelector(':scope > .gm-shell-top .gm-topbar')).toBeTruthy()
    const corps = racine.querySelector(':scope > .gm-shell-corps')!
    expect(corps.querySelector(':scope > .gm-shell-nav > nav.gm-sidenav')).toBeTruthy()
    const contenu = screen.getByTestId('gm-shell-contenu')
    expect(contenu.querySelector(':scope > main.gm-shell-main')).toBeTruthy()
    expect(contenu.querySelector(':scope > footer.gm-footer')).toBeTruthy()
    expect((racine.querySelector('nav') as HTMLElement).style.height).toBe('100%')
  })
  it('clic sur une entrée : seule la zone de contenu change et son défilement revient en haut', () => {
    coquille()
    const contenu = screen.getByTestId('gm-shell-contenu')
    const nav = screen.getByRole('navigation')
    contenu.scrollTop = 640
    fireEvent.click(screen.getByRole('link', { name: 'x.cp' }))
    expect(screen.getByTestId('ici').textContent).toBe('/console/comptes')
    expect(contenu.scrollTop).toBe(0)
    expect(screen.getByRole('navigation')).toBe(nav)
    expect(screen.getByRole('link', { name: 'x.cp' }).getAttribute('aria-current')).toBe('page')
  })
})

describe('Enveloppe : tiroir de navigation sous 1 280 px', () => {
  it('menu, voile confiné à la zone de contenu, fermeture par le voile, Échap et le changement de page', () => {
    Object.defineProperty(window, 'innerWidth', { value: 800, configurable: true })
    try {
      const { container } = coquille()
      expect(container.querySelector('.gm-shell-nav')).toBeNull()
      const menu = screen.getByRole('button', { name: /navigation|menu|Replier/i })
      fireEvent.click(menu)
      let voile = container.querySelector('.gm-shell-tiroir') as HTMLElement
      expect(voile.classList.contains('gm-overlay-inline')).toBe(true)
      expect(voile.parentElement?.classList.contains('gm-shell-corps')).toBe(true)
      fireEvent.click(voile)
      expect(container.querySelector('.gm-shell-tiroir')).toBeNull()
      fireEvent.click(menu)
      voile = container.querySelector('.gm-shell-tiroir') as HTMLElement
      fireEvent.click(voile.querySelector('nav')!)
      expect(container.querySelector('.gm-shell-tiroir')).toBeTruthy()
      fireEvent.keyDown(voile, { key: 'Escape' })
      expect(container.querySelector('.gm-shell-tiroir')).toBeNull()
      fireEvent.click(menu)
      fireEvent.click(screen.getByRole('link', { name: 'x.cp' }))
      expect(container.querySelector('.gm-shell-tiroir')).toBeNull()
      Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
      fireEvent(window, new Event('resize'))
      expect(container.querySelector('.gm-shell-nav')).toBeTruthy()
    } finally {
      Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true })
    }
  })
})
