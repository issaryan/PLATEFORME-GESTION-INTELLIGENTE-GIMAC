// Tableau mural et Gantt (D-114, demande du 19/10/2026) : pastille portant l'état réel, colonne « Tenue » absente
// quand aucun jalon n'a de probabilité.
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen, within } from '@testing-library/react'
import { GanttView, WallBoard } from '../src'

afterEach(cleanup)

describe('WallBoard', () => {
  it('la pastille porte le libellé fourni ; à défaut, celui de l’état', () => {
    render(
      <WallBoard
        tiles={[
          { id: 'LIA-1', name: 'Liaison critique', state: 'crit', label: 'Critique', value: '12', unit: 'minutes dans cet état', metric: 'mesure' },
          { id: 'LIA-2', name: 'Liaison injoignable', state: 'crit', value: '3', metric: 'mesure' },
        ]}
      />,
    )
    const tuiles = screen.getAllByText(/^LIA-/).map((e) => e.closest('.gm-wall-tile') as HTMLElement)
    expect(within(tuiles[0]).getByText('Critique')).toBeTruthy()
    expect(within(tuiles[0]).getByText('minutes dans cet état')).toBeTruthy()
    expect(within(tuiles[1]).getByText('injoignable')).toBeTruthy()
    expect(within(tuiles[0]).getByText('Liaison critique').getAttribute('title')).toBe('Liaison critique')
  })
})

describe('GanttView', () => {
  const jalon = { key: 'a', label: 'Cadrage', owner: 'Personne fictive', start: 0, duration: 5, done: 40 }

  it('sans probabilité : pas de colonne « Tenue »', () => {
    render(<GanttView milestones={[jalon]} />)
    expect(screen.queryByRole('columnheader', { name: 'Tenue' })).toBeNull()
  })

  it('avec une probabilité : colonne « Tenue », retard sous 50 %', () => {
    render(<GanttView milestones={[{ ...jalon, probability: 30 }, { ...jalon, key: 'b', label: 'Tests' }]} />)
    expect(screen.getByRole('columnheader', { name: 'Tenue' })).toBeTruthy()
    expect(screen.getByText('30 %').className).toContain('gm-late')
  })
})
