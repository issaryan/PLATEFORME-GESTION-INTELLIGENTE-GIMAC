// Interactions des fondations et des champs avancés : thème et densité (ThemeProvider), piège de focus
// des dialogues (Tab, Maj+Tab, Échap, retour au déclencheur), sélection multiple par jetons.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { useRef, useState } from 'react'
import { Dialog, MultiSelect, ThemeProvider, useFocusTrap, useTheme } from '../src'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

function Bascule() {
  const { theme, density, setTheme, setDensity } = useTheme()
  return (
    <div>
      <span data-testid="etat">{`${theme}/${density}`}</span>
      <button type="button" onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}>thème</button>
      <button type="button" onClick={() => setDensity(density === 'normal' ? 'compact' : 'normal')}>densité</button>
    </div>
  )
}

describe('ThemeProvider', () => {
  it('préférence du profil, changements notifiés, attributs posés sur la cible', () => {
    const cible = document.createElement('div')
    const changer = vi.fn()
    const { rerender } = render(<ThemeProvider theme="dark" density="compact" target={cible} onChange={changer}><Bascule /></ThemeProvider>)
    expect(cible.getAttribute('data-theme')).toBe('dark')
    expect(cible.getAttribute('data-density')).toBe('compact')
    fireEvent.click(screen.getByRole('button', { name: 'thème' }))
    expect(changer).toHaveBeenLastCalledWith({ theme: 'light', density: 'compact' })
    fireEvent.click(screen.getByRole('button', { name: 'densité' }))
    expect(changer).toHaveBeenLastCalledWith({ theme: 'light', density: 'normal' })
    rerender(<ThemeProvider theme="light" density="normal" target={cible} onChange={changer}><Bascule /></ThemeProvider>)
    rerender(<ThemeProvider theme="dark" density="normal" target={cible} onChange={changer}><Bascule /></ThemeProvider>)
    expect(screen.getByTestId('etat').textContent).toBe('dark/normal')
  })
  it('sans préférence : thème du système, racine du document', () => {
    Object.defineProperty(window, 'matchMedia', { configurable: true, value: (q: string) => ({ matches: q.includes('dark'), media: q }) })
    render(<ThemeProvider><Bascule /></ThemeProvider>)
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    fireEvent.click(screen.getByRole('button', { name: 'thème' }))
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
  })
  it('useTheme hors fournisseur : erreur explicite', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined)
    expect(() => render(<Bascule />)).toThrow(/ThemeProvider/)
  })
})

function Piege({ onClose }: { onClose: () => void }) {
  const [ouvert, setOuvert] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  useFocusTrap(ref, ouvert, () => { onClose(); setOuvert(false) })
  return (
    <div>
      <button type="button" onClick={() => setOuvert(true)}>ouvrir</button>
      {ouvert ? (
        <div ref={ref}>
          <button type="button">premier</button>
          <button type="button">dernier</button>
        </div>
      ) : null}
    </div>
  )
}

describe('useFocusTrap', () => {
  it('focus sur le premier élément, boucle Tab et Maj+Tab, Échap ferme et rend le focus', () => {
    const fermer = vi.fn()
    render(<Piege onClose={fermer} />)
    const ouvrir = screen.getByRole('button', { name: 'ouvrir' })
    ouvrir.focus()
    fireEvent.click(ouvrir)
    const premier = screen.getByRole('button', { name: 'premier' })
    const dernier = screen.getByRole('button', { name: 'dernier' })
    expect(document.activeElement).toBe(premier)
    fireEvent.keyDown(premier, { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(dernier)
    fireEvent.keyDown(dernier, { key: 'Tab' })
    expect(document.activeElement).toBe(premier)
    fireEvent.keyDown(premier, { key: 'Tab' })
    fireEvent.keyDown(premier, { key: 'Escape' })
    expect(fermer).toHaveBeenCalled()
    expect(document.activeElement).toBe(ouvrir)
  })
  it('dialogue sans élément focalisable : Tab sans effet', () => {
    const { container } = render(<Dialog open title="Information" onClose={() => undefined}><p>Texte</p></Dialog>)
    fireEvent.keyDown(container.querySelector('[role="dialog"]') ?? document.body, { key: 'Tab' })
    expect(screen.getByRole('dialog')).toBeTruthy()
  })
})

describe('MultiSelect', () => {
  it('jetons retirés à la souris et au clavier, saisie de recherche', () => {
    const changer = vi.fn()
    const chercher = vi.fn()
    const opts = [{ value: 'RETRAIT', label: 'Retrait' }, { value: 'PAIEMENT', label: 'Paiement' }]
    render(<MultiSelect label="Services" value={['RETRAIT', 'INCONNU']} options={opts} onChange={changer} onQuery={chercher} required help="Au moins un" />)
    expect(screen.getByText('INCONNU')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /Retirer Retrait/ }))
    expect(changer).toHaveBeenLastCalledWith(['INCONNU'])
    const champ = screen.getByRole('textbox', { name: 'Services' })
    fireEvent.keyDown(champ, { key: 'Backspace' })
    expect(changer).toHaveBeenLastCalledWith(['RETRAIT'])
    fireEvent.change(champ, { target: { value: 'pai' } })
    expect(chercher).toHaveBeenCalledWith('pai')
  })
  it('sans valeur ni gestionnaire : rendu seul', () => {
    render(<MultiSelect label="Services" />)
    fireEvent.keyDown(screen.getByRole('textbox', { name: 'Services' }), { key: 'Backspace' })
    fireEvent.change(screen.getByRole('textbox', { name: 'Services' }), { target: { value: 'x' } })
    expect(screen.queryByRole('button')).toBeNull()
  })
})
