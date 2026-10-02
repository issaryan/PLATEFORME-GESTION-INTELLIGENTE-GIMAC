import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest'
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
  it('menu utilisateur de la barre du haut : photo, nom, fonction, Mon compte et déconnexion', () => {
    render(
      <MemoryRouter initialEntries={['/des/tableau-de-bord']}>
        <ThemeProvider theme="light">
          <Enveloppe couche="des" navigation={[]} espacePersonnel="/des" utilisateur="Paul Mbarga" fonction="Chef du département DES" avatar="data:image/png;base64,AAAA">
            <Routes><Route path="*" element={<Ici />} /></Routes>
          </Enveloppe>
        </ThemeProvider>
      </MemoryRouter>,
    )
    const bouton = screen.getByRole('button', { name: 'Menu utilisateur : Paul Mbarga' })
    expect(bouton.querySelector('img')!.getAttribute('src')).toBe('data:image/png;base64,AAAA')
    fireEvent.click(bouton)
    expect(screen.getByText('Chef du département DES')).toBeTruthy()
    fireEvent.click(screen.getByRole('menuitem', { name: 'Mon compte' }))
    expect(screen.getByTestId('ici').textContent).toBe('/des/mon-compte')
    fireEvent.click(bouton)
    fireEvent.click(screen.getByRole('menuitem', { name: 'Se déconnecter' }))
    expect(screen.getByTestId('ici').textContent).toBe('/des/deconnexion')
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
  it('navigation sur toute la hauteur ; à sa droite, barre supérieure puis zone de contenu et pied de page', () => {
    const { container } = coquille()
    const racine = container.querySelector('.gm-shell')!
    expect(racine.classList.contains('gm-root')).toBe(true)
    // La navigation est un enfant direct de la coquille, à côté de la colonne (barre du haut, contenu).
    expect(racine.querySelector(':scope > .gm-shell-nav > nav.gm-sidenav')).toBeTruthy()
    const colonne = racine.querySelector(':scope > .gm-shell-colonne')!
    expect(colonne.querySelector(':scope > .gm-shell-top .gm-topbar')).toBeTruthy()
    expect(colonne.querySelector(':scope > .gm-shell-corps > [data-testid="gm-shell-contenu"]')).toBeTruthy()
    expect(racine.firstElementChild?.classList.contains('gm-shell-nav')).toBe(true)
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

// Recherche globale (Ctrl+K) : écrans permis de la navigation et objets des sources ; portail sans recherche.
describe('Enveloppe : recherche globale', () => {
  const SOURCES = [
    {
      cle: 'liaisons',
      chercher: async (q: string) => (q.toLowerCase().startsWith('ba') ? [{ cle: 'L1', type: 'liaison' as const, code: 'LIA-2026-030', libelle: 'Banque C', detail: 'Certification', href: '/des/parcours/L1' }] : []),
    },
    { cle: 'panne', chercher: async () => Promise.reject(new Error('503')) },
    { cle: 'externe', chercher: async () => [{ cle: 'X', type: 'compte' as const, libelle: 'Banque externe', href: 'http://localhost:4202/console/comptes/X' }] },
  ]
  const avecRecherche = (couche: 'des' | 'portail' = 'des') =>
    render(
      <MemoryRouter initialEntries={['/des/tableau-de-bord']}>
        <ThemeProvider theme="light">
          <Enveloppe couche={couche} navigation={[{ entrees: [
            { cle: 'tb', libelleKey: 'x.tableau', icone: 'gauge', chemin: '/des/tableau-de-bord' },
            { cle: 'li', libelleKey: 'x.liaisons', icone: 'route', chemin: '/des/parcours' },
            { cle: 'adm', libelleKey: 'x.admin', icone: 'users', chemin: '/des/administration/comptes', permission: 'IAM_ACCOUNT_READ' },
          ] }]} permissions={[]} espacePersonnel="/des" recherche={SOURCES}>
            <Routes><Route path="*" element={<Ici />} /></Routes>
          </Enveloppe>
        </ThemeProvider>
      </MemoryRouter>,
    )
  it('Ctrl+K ouvre la palette : écrans permis, objets des sources (une source en échec ignorée), ouverture au clavier', async () => {
    avecRecherche()
    fireEvent.keyDown(document.body, { key: 'k', ctrlKey: true })
    const champ = screen.getByRole('combobox')
    // Écrans permis seulement (sans l'administration ni la déconnexion).
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['x.tableau', 'x.liaisons', 'Mon compte'])
    // Écrans filtrés sur place ; objets cherchés dans les sources, recherche en cours annoncée.
    fireEvent.change(champ, { target: { value: 'liai' } })
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['x.liaisons'])
    expect(await screen.findByRole('status')).toBeTruthy()
    fireEvent.change(champ, { target: { value: 'ba' } })
    expect(await screen.findByText('Banque C', {}, { timeout: 2000 })).toBeTruthy()
    expect(screen.getByText('Banque externe')).toBeTruthy()
    fireEvent.keyDown(champ, { key: 'Enter' })
    expect(screen.getByTestId('ici').textContent).toBe('/des/parcours/L1')
    expect(screen.queryByRole('combobox')).toBeNull()
  })
  it('bouton de la barre, « / » hors saisie, Échap et clic sur le voile ; lien absolu ouvert hors du routeur', async () => {
    const assign = vi.fn()
    vi.stubGlobal('location', { ...window.location, assign })
    try {
      avecRecherche()
      fireEvent.click(screen.getByRole('button', { name: 'Recherche globale (Ctrl+K)' }))
      fireEvent.keyDown(screen.getByRole('combobox'), { key: 'Escape' })
      expect(screen.queryByRole('combobox')).toBeNull()
      fireEvent.keyDown(document.body, { key: '/' })
      const voile = document.querySelector('.gm-recherche-voile') as HTMLElement
      fireEvent.mouseDown(voile)
      expect(screen.queryByRole('combobox')).toBeNull()
      fireEvent.keyDown(document.body, { key: 'K', metaKey: true })
      fireEvent.change(screen.getByRole('combobox'), { target: { value: 'externe' } })
      fireEvent.mouseDown(await screen.findByText('Banque externe', {}, { timeout: 2000 }))
      expect(assign).toHaveBeenCalledWith('http://localhost:4202/console/comptes/X')
    } finally {
      vi.unstubAllGlobals()
    }
  })
  it('portail : ni bouton ni raccourci', () => {
    avecRecherche('portail')
    expect(screen.queryByRole('button', { name: 'Recherche globale (Ctrl+K)' })).toBeNull()
    fireEvent.keyDown(document.body, { key: 'k', ctrlKey: true })
    expect(screen.queryByRole('combobox')).toBeNull()
  })
})

describe('Enveloppe : sans sources de recherche', () => {
  it('premier accès ou couche sans recherche : ni bouton ni raccourci', () => {
    monter('/console')
    expect(screen.queryByRole('button', { name: 'Recherche globale (Ctrl+K)' })).toBeNull()
    fireEvent.keyDown(document.body, { key: 'k', ctrlKey: true })
    expect(screen.queryByRole('combobox')).toBeNull()
  })
})
