import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { ChartCard, FilterBar } from '../src'

// ChartCard et FilterBar : le bouton d'export, la recherche, la réinitialisation et la vue enregistrée n'existe que s'il agit (demande de l'utilisateur du 05/11/2026, aucun bouton sans effet).
describe('ChartCard, export', () => {
  afterEach(cleanup)

  it('sans action d’export : aucun bouton', () => {
    render(<ChartCard title="Jauge" type="gauge" max={100} series={[{ name: 'Avancement', data: [40] }]} />)
    expect(screen.queryByRole('button', { name: 'Exporter (contrôlé)' })).toBeNull()
  })

  it('avec action d’export : bouton qui l’appelle', () => {
    const exporter = vi.fn()
    render(<ChartCard title="Barres" type="bar" labels={['A', 'B']} series={[{ name: 'S', data: [1, 2] }]} onExporter={exporter} />)
    fireEvent.click(screen.getByRole('button', { name: 'Exporter (contrôlé)' }))
    expect(exporter).toHaveBeenCalledTimes(1)
  })
})

describe('FilterBar, contrôles qui agissent seuls', () => {
  afterEach(cleanup)

  it('sans recherche, réinitialisation ni enregistrement : aucun de ces contrôles ; « Tous » retiré à la demande', () => {
    render(<FilterBar filters={[{ key: 'p', label: 'Période', value: '7', tous: false, options: [{ value: '7', label: '7 jours' }] }, { key: 'i', label: 'Interface', options: [{ value: 'A', label: 'A' }] }]} />)
    expect(screen.queryByRole('searchbox')).toBeNull()
    expect(screen.queryAllByRole('button')).toHaveLength(0)
    expect(Array.from((screen.getByLabelText('Période') as HTMLSelectElement).options).map((o) => o.value)).toEqual(['7'])
    expect(Array.from((screen.getByLabelText('Interface') as HTMLSelectElement).options).map((o) => o.value)).toEqual(['', 'A'])
  })

  it('avec leurs actions : recherche, réinitialisation et vue enregistrée appelées', () => {
    const q = vi.fn()
    const r = vi.fn()
    const e = vi.fn()
    render(<FilterBar filters={[]} onQuery={q} onReset={r} onEnregistrer={e} savedLabel="Ma vue" />)
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'x' } })
    fireEvent.click(screen.getByRole('button', { name: /Réinitialiser/ }))
    fireEvent.click(screen.getByRole('button', { name: /Ma vue/ }))
    expect([q.mock.calls.length > 0, r.mock.calls.length, e.mock.calls.length]).toEqual([true, 1, 1])
  })
})
