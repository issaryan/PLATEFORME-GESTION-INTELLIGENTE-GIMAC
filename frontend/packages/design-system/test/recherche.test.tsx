import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { CommandPalette, TopBar, type CommandResult } from '../src'

// Recherche globale (§7.4) : palette interactive (flèches, Entrée, clic, Échap) et déclencheur de la barre supérieure.
afterEach(cleanup)

const RESULTATS: CommandResult[] = [
  { key: 'e1', group: 'ecrans', kind: 'ecran', label: 'Liaisons et parcours' },
  { key: 'l1', group: 'objets', kind: 'liaison', code: 'LIA-2026-030', label: 'Banque C', detail: 'Certification' },
  { key: 'c1', group: 'objets', kind: 'compte', label: 'Rachel Biyong Mengue' },
]

describe('CommandPalette interactive', () => {
  it('objets puis écrans ; flèches, Entrée, clic et Échap', () => {
    const choisir = vi.fn()
    const fermer = vi.fn()
    render(<CommandPalette query="ba" results={RESULTATS} onSelect={choisir} onClose={fermer} />)
    const champ = screen.getByRole('combobox')
    expect(document.activeElement).toBe(champ)
    const options = screen.getAllByRole('option')
    expect(options.map((o) => o.textContent)).toEqual(['LIA-2026-030Banque CCertification', 'Rachel Biyong Mengue', 'Liaisons et parcours'])
    expect(champ.getAttribute('aria-activedescendant')).toBe(options[0].id)
    fireEvent.keyDown(champ, { key: 'ArrowDown' })
    expect(options[1].getAttribute('aria-selected')).toBe('true')
    fireEvent.keyDown(champ, { key: 'ArrowUp' })
    fireEvent.keyDown(champ, { key: 'ArrowUp' })
    expect(options[2].getAttribute('aria-selected')).toBe('true')
    fireEvent.keyDown(champ, { key: 'Enter' })
    expect(choisir).toHaveBeenLastCalledWith(RESULTATS[0])
    fireEvent.mouseEnter(options[1])
    fireEvent.mouseDown(options[1])
    expect(choisir).toHaveBeenLastCalledWith(RESULTATS[2])
    fireEvent.keyDown(champ, { key: 'Escape' })
    expect(fermer).toHaveBeenCalled()
  })
  it('aucun résultat, recherche en cours, palette statique sans sélection', () => {
    const { rerender } = render(<CommandPalette query="zz" results={[]} onSelect={() => undefined} />)
    expect(screen.getByText('Aucun résultat pour « zz »')).toBeTruthy()
    fireEvent.keyDown(screen.getByRole('combobox'), { key: 'ArrowDown' })
    fireEvent.keyDown(screen.getByRole('combobox'), { key: 'Enter' })
    rerender(<CommandPalette query="zz" results={[]} chargement onSelect={() => undefined} />)
    expect(screen.getByRole('status').textContent).toBe('Recherche en cours…')
    expect(screen.queryByText('Aucun résultat pour « zz »')).toBeNull()
    rerender(<CommandPalette query="ba" results={RESULTATS} active={1} />)
    fireEvent.mouseDown(screen.getAllByRole('option')[0])
    expect(screen.getAllByRole('option')[1].getAttribute('aria-selected')).toBe('true')
  })
})

describe('TopBar : déclencheur de la recherche', () => {
  it('bouton « Rechercher » avec Ctrl K quand onRecherche est fourni, rien sinon', () => {
    const ouvrir = vi.fn()
    const { rerender } = render(<TopBar onRecherche={ouvrir} />)
    const bouton = screen.getByRole('button', { name: 'Recherche globale (Ctrl+K)' })
    expect(bouton.getAttribute('aria-keyshortcuts')).toBe('Control+K')
    fireEvent.click(bouton)
    expect(ouvrir).toHaveBeenCalled()
    rerender(<TopBar />)
    expect(screen.queryByRole('button', { name: 'Recherche globale (Ctrl+K)' })).toBeNull()
    expect(screen.queryByRole('searchbox')).toBeNull()
  })
})
