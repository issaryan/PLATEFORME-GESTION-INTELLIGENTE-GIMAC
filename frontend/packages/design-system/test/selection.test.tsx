// Listes de choix interactives (§7.2) : choix à la souris et au clavier, fermeture par Échap.
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MultiSelect, SearchSelect, Select } from '../src'

afterEach(cleanup)
const OPTIONS = [
  { value: 'u1', label: 'Aïcha Mahamat Nour', code: 'COND' },
  { value: 'u2', label: 'Rachel Biyong Mengue', code: 'SNA' },
  { value: 'u3', label: 'Hervé Kamga Tchoupo', code: 'RSSI' },
]

describe('SearchSelect', () => {
  it('s’ouvre à la saisie, choix au clavier, fermeture au choix', () => {
    const choisir = vi.fn()
    const requete = vi.fn()
    render(<SearchSelect label="Liaison" options={OPTIONS} onChange={choisir} onQuery={requete} />)
    const champ = screen.getByRole('combobox', { name: 'Liaison' })
    expect(champ.getAttribute('aria-expanded')).toBe('false')
    fireEvent.change(champ, { target: { value: 'a' } })
    expect(requete).toHaveBeenCalledWith('a')
    expect(champ.getAttribute('aria-expanded')).toBe('true')
    fireEvent.keyDown(champ, { key: 'ArrowDown' })
    fireEvent.keyDown(champ, { key: 'ArrowDown' })
    fireEvent.keyDown(champ, { key: 'ArrowUp' })
    expect(champ.getAttribute('aria-activedescendant')).toMatch(/-opt-1$/)
    fireEvent.keyDown(champ, { key: 'Enter' })
    expect(choisir).toHaveBeenCalledWith('u2')
    expect(champ.getAttribute('aria-expanded')).toBe('false')
  })
  it('choix à la souris ; Échap et perte du focus ferment ; liste vide annoncée', () => {
    const choisir = vi.fn()
    const { rerender } = render(<SearchSelect label="Liaison" options={OPTIONS} onChange={choisir} />)
    const champ = screen.getByRole('combobox')
    fireEvent.change(champ, { target: { value: 'x' } })
    fireEvent.mouseDown(screen.getByRole('option', { name: /Hervé/ }))
    expect(choisir).toHaveBeenCalledWith('u3')
    fireEvent.change(champ, { target: { value: 'y' } })
    fireEvent.keyDown(champ, { key: 'Escape' })
    expect(screen.queryByRole('listbox')).toBeNull()
    fireEvent.change(champ, { target: { value: 'z' } })
    fireEvent.blur(champ)
    expect(screen.queryByRole('listbox')).toBeNull()
    rerender(<SearchSelect label="Liaison" options={[]} open />)
    expect(screen.getByRole('listbox').textContent).toBeTruthy()
  })
})

function Destinataires({ onChange }: { onChange: (v: string[]) => void }) {
  const [v, setV] = useState<string[]>(['u9'])
  const [q, setQ] = useState('')
  return <MultiSelect label="Destinataires" suggestions labels={{ u9: 'Personne hors page' }} value={v} onChange={(x) => { setV(x); onChange(x) }} query={q} onQuery={setQ} options={OPTIONS} />
}

describe('MultiSelect avec suggestions', () => {
  it('ajout à la souris et au clavier, libellé d’une valeur hors options, retrait, Échap', () => {
    const changer = vi.fn()
    render(<Destinataires onChange={changer} />)
    expect(screen.getByText('Personne hors page')).toBeTruthy()
    const champ = screen.getByRole('combobox', { name: 'Destinataires' })
    fireEvent.focus(champ)
    fireEvent.mouseDown(screen.getByRole('option', { name: 'Rachel Biyong Mengue' }))
    expect(changer).toHaveBeenLastCalledWith(['u9', 'u2'])
    // L'option choisie ne figure plus parmi les suggestions.
    expect(screen.queryByRole('option', { name: 'Rachel Biyong Mengue' })).toBeNull()
    fireEvent.change(champ, { target: { value: 'h' } })
    fireEvent.keyDown(champ, { key: 'ArrowDown' })
    fireEvent.keyDown(champ, { key: 'Enter' })
    expect(changer).toHaveBeenLastCalledWith(['u9', 'u2', 'u3'])
    fireEvent.keyDown(champ, { key: 'ArrowUp' })
    fireEvent.keyDown(champ, { key: 'Escape' })
    expect(screen.queryByRole('listbox')).toBeNull()
    fireEvent.keyDown(champ, { key: 'Backspace' })
    expect(changer).toHaveBeenLastCalledWith(['u9', 'u2'])
    fireEvent.blur(champ)
  })
  it('toutes les options choisies : liste vide annoncée ; sans suggestions, pas de liste', () => {
    const { rerender } = render(<MultiSelect label="D" suggestions value={['u1', 'u2', 'u3']} options={OPTIONS} />)
    fireEvent.focus(screen.getByRole('combobox'))
    expect(screen.getByRole('listbox')).toBeTruthy()
    rerender(<MultiSelect label="D" value={['u1']} options={OPTIONS} />)
    fireEvent.focus(screen.getByLabelText('D'))
    expect(screen.queryByRole('listbox')).toBeNull()
  })
})

describe('Select groupé', () => {
  it('réunit les options consécutives d’un même groupe sous son titre, les autres restent seules', () => {
    const choisir = vi.fn()
    render(<Select label="Personne" value="" onChange={choisir} options={[
      { value: '', label: 'Choisir' },
      { value: 'u1', label: 'Aïcha', group: 'Dans l’unité' },
      { value: 'u2', label: 'Rachel', group: 'Ailleurs' },
      { value: 'u3', label: 'Hervé', group: 'Ailleurs' },
    ]} />)
    const liste = screen.getByRole('combobox', { name: 'Personne' })
    const groupes = liste.querySelectorAll('optgroup')
    expect([...groupes].map((g) => g.label)).toEqual(['Dans l’unité', 'Ailleurs'])
    expect(groupes[1].querySelectorAll('option')).toHaveLength(2)
    expect(liste.querySelectorAll('option')).toHaveLength(4)
    fireEvent.change(liste, { target: { value: 'u3' } })
    expect(choisir).toHaveBeenCalledWith('u3')
  })
})
