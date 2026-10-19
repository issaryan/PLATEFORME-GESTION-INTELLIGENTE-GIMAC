// Tableau Kanban (D-113) : glisser-déposer, déplacement au clavier, ouverture et ajout.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { KanbanBoard, type KanbanColumn } from '../src'

afterEach(cleanup)

const carte = (id: string) => ({ id, reference: `REF-${id}`, title: `Tâche ${id}`, institution: 'Personne fictive', status: 'info' as const, statusLabel: 'en cours', due: '20/10/2026' })
const colonnes = (onAdd?: () => void): KanbanColumn[] => [
  { key: 'OUVERTE', label: 'Ouverte', cards: [carte('a')], onAdd },
  { key: 'EN_COURS', label: 'En cours', cards: [] },
  { key: 'TERMINEE', label: 'Terminée', cards: [carte('b')] },
]

describe('KanbanBoard D-113', () => {
  it('déplace au clavier vers la colonne permise suivante et l’annonce', () => {
    const deplacer = vi.fn()
    render(<KanbanBoard columns={colonnes()} onMove={deplacer} canMove={(_, vers) => vers !== 'EN_COURS'} />)
    const a = screen.getByRole('article', { name: 'REF-a Tâche a' })
    fireEvent.keyDown(a, { key: 'ArrowRight', altKey: true })
    expect(deplacer).toHaveBeenCalledWith('a', 'TERMINEE')
    expect(screen.getByRole('status').textContent).toBe('a déplacée vers « Terminée »')
    fireEvent.keyDown(a, { key: 'ArrowLeft', altKey: true })
    expect(deplacer).toHaveBeenCalledTimes(1)
  })
  it('glisse une carte dans une colonne permise, pas dans une autre', () => {
    const deplacer = vi.fn()
    const { container } = render(<KanbanBoard columns={colonnes()} onMove={deplacer} canMove={(_, vers) => vers === 'EN_COURS'} />)
    const [porteur] = container.querySelectorAll('.gm-kanban-carte')
    const donnees = { types: ['text/plain'], setData: () => {}, effectAllowed: '' }
    fireEvent.dragStart(porteur, { dataTransfer: donnees })
    const enCours = screen.getByRole('region', { name: 'En cours' })
    fireEvent.dragOver(enCours, { dataTransfer: donnees })
    expect(enCours.className).toContain('gm-kanban-cible')
    fireEvent.dragLeave(enCours)
    fireEvent.drop(enCours, { dataTransfer: donnees })
    expect(deplacer).toHaveBeenCalledWith('a', 'EN_COURS')
    fireEvent.dragStart(porteur, { dataTransfer: donnees })
    const terminee = screen.getByRole('region', { name: 'Terminée' })
    fireEvent.dragOver(terminee, { dataTransfer: donnees })
    expect(terminee.className).not.toContain('gm-kanban-cible')
    fireEvent.dragEnd(porteur)
  })
  it('ouvre une carte au clic et à Entrée, ajoute dans une colonne, masque les ajouts inertes', () => {
    const ouvrir = vi.fn()
    const ajouter = vi.fn()
    render(<KanbanBoard columns={colonnes(ajouter)} onOpen={ouvrir} masquerAjout />)
    fireEvent.click(screen.getByRole('article', { name: 'REF-b Tâche b' }))
    fireEvent.keyDown(screen.getByRole('article', { name: 'REF-a Tâche a' }), { key: 'Enter' })
    expect(ouvrir.mock.calls).toEqual([['b'], ['a']])
    const boutons = screen.getAllByRole('button', { name: 'Ajouter une tâche' })
    expect(boutons).toHaveLength(1)
    fireEvent.click(boutons[0])
    expect(ajouter).toHaveBeenCalledOnce()
  })
})

describe('KanbanBoard D-113 : pagination des colonnes', () => {
  it('pagine une colonne au-delà de la taille de page', () => {
    const cartes = Array.from({ length: 5 }, (_, i) => ({ id: `c${i}`, title: `Tâche ${i}`, institution: 'x', status: 'info' as const, statusLabel: 'en cours', due: '20/10/2026' }))
    render(<KanbanBoard pageSize={2} columns={[{ key: 'A', label: 'À faire', cards: cartes }, { key: 'B', label: 'Fait', cards: [] }]} />)
    expect(screen.getAllByRole('article')).toHaveLength(2)
    expect(screen.getByText('Page 1 sur 3')).toBeTruthy()
    expect((screen.getByRole('button', { name: 'Page précédente' }) as HTMLButtonElement).disabled).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: 'Page suivante' }))
    fireEvent.click(screen.getByRole('button', { name: 'Page suivante' }))
    expect(screen.getByText('Page 3 sur 3')).toBeTruthy()
    expect(screen.getAllByRole('article')).toHaveLength(1)
    fireEvent.click(screen.getByRole('button', { name: 'Page précédente' }))
    expect(screen.getByText('Page 2 sur 3')).toBeTruthy()
    expect(screen.getAllByRole('navigation')).toHaveLength(1)
  })
})
