// Grille d'agenda (D-113) : placement des événements, chevauchements, vues jour, semaine et mois, création.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { AgendaGrid, placer, type AgendaEvent } from '../src'

afterEach(cleanup)

const jourDe = (iso: string) => new Date(new Date(iso).getTime() + 3_600_000).toISOString().slice(0, 10)
const minutesDe = (iso: string) => {
  const d = new Date(new Date(iso).getTime() + 3_600_000)
  return d.getUTCHours() * 60 + d.getUTCMinutes()
}
const heure = (iso: string) => new Date(new Date(iso).getTime() + 3_600_000).toISOString().slice(11, 16)
const ev = (id: string, debut: string, fin: string, plus: Partial<AgendaEvent> = {}): AgendaEvent => ({ id, titre: `Événement ${id}`, debut, fin, categorie: 'Réunion', couleur: 1, ...plus })
const base = { aujourdhui: '2026-10-19', label: 'Agenda', libelleJour: (j: string) => `jour ${j}`, heure, jourDe, minutesDe, onOuvrir: () => {} }

describe('placer', () => {
  it('place côte à côte les événements qui se chevauchent', () => {
    const l = placer([
      ev('a', '2026-10-19T09:00:00+01:00', '2026-10-19T10:00:00+01:00'),
      ev('b', '2026-10-19T09:30:00+01:00', '2026-10-19T11:00:00+01:00'),
      ev('c', '2026-10-19T12:00:00+01:00', '2026-10-19T13:00:00+01:00'),
      ev('j', '2026-10-19T00:00:00+01:00', '2026-10-20T00:00:00+01:00', { journeeEntiere: true }),
    ], '2026-10-19', jourDe, minutesDe)
    expect(l.map((p) => [p.e.id, p.colonne, p.colonnes])).toEqual([['a', 0, 2], ['b', 1, 2], ['c', 0, 1]])
    expect(l[0].debut).toBe(540)
  })
  it('borne un événement sur plusieurs jours au jour affiché', () => {
    const [p] = placer([ev('m', '2026-10-18T22:00:00+01:00', '2026-10-20T02:00:00+01:00')], '2026-10-19', jourDe, minutesDe)
    expect([p.debut, p.fin]).toEqual([0, 1440])
  })
})

describe('AgendaGrid', () => {
  it('vue semaine : événements, journée entière, heure actuelle et création à une heure', () => {
    const creer = vi.fn()
    const ouvrir = vi.fn()
    render(<AgendaGrid {...base} vue="semaine" jours={['2026-10-19', '2026-10-20']} maintenant="2026-10-19T14:30:00+01:00" onOuvrir={ouvrir} onCreer={creer}
      evenements={[ev('a', '2026-10-19T09:00:00+01:00', '2026-10-19T11:00:00+01:00', { lieu: 'Salle 2', enAttente: true }), ev('j', '2026-10-20T00:00:00+01:00', '2026-10-21T00:00:00+01:00', { journeeEntiere: true })]} />)
    expect(screen.getByText('Toute la journée')).toBeTruthy()
    expect(screen.getByRole('img', { name: 'Heure actuelle' })).toBeTruthy()
    expect(screen.getByText('Salle 2')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: '09:00 à 11:00 · Événement a · Réunion · invitation en attente de réponse' }))
    expect(ouvrir).toHaveBeenCalledWith('a')
    fireEvent.click(screen.getByRole('button', { name: 'Créer un événement le jour 2026-10-20 à 08:00' }))
    expect(creer).toHaveBeenCalledWith('2026-10-20', 8)
    fireEvent.click(screen.getByRole('button', { name: 'Événement j · Réunion' }))
    expect(ouvrir).toHaveBeenCalledWith('j')
  })
  it('lecture seule et événement hors de la plage horaire', () => {
    render(<AgendaGrid {...base} vue="jour" jours={['2026-10-19']} heureDebut={8} heureFin={18} evenements={[ev('t', '2026-10-19T06:00:00+01:00', '2026-10-19T07:00:00+01:00')]} />)
    expect(screen.queryByRole('button', { name: /Créer un événement/ })).toBeNull()
    expect(screen.queryByText('Événement t')).toBeNull()
  })
  it('vue mois : trois événements par jour puis « + n autres », jours hors du mois atténués', () => {
    const voir = vi.fn()
    const creer = vi.fn()
    const jours = Array.from({ length: 42 }, (_, i) => new Date(Date.UTC(2026, 8, 28 + i)).toISOString().slice(0, 10))
    const quatre = ['08', '10', '12', '14'].map((h, i) => ev(`m${i}`, `2026-10-19T${h}:00:00+01:00`, `2026-10-19T${h}:30:00+01:00`))
    const { container } = render(<AgendaGrid {...base} vue="mois" mois="2026-10" jours={jours} evenements={[...quatre, ev('jj', '2026-10-19T00:00:00+01:00', '2026-10-20T00:00:00+01:00', { journeeEntiere: true })]} onVoirJour={voir} onCreer={creer} />)
    fireEvent.click(screen.getByRole('button', { name: '+ 2 autres' }))
    expect(voir).toHaveBeenCalledWith('2026-10-19')
    expect(container.querySelectorAll('.gm-agenda-hors').length).toBeGreaterThan(0)
    fireEvent.click(screen.getByRole('button', { name: 'Créer un événement le jour 2026-10-05' }))
    expect(creer).toHaveBeenCalledWith('2026-10-05')
  })
  it('vue mois en lecture seule', () => {
    const jours = Array.from({ length: 42 }, (_, i) => new Date(Date.UTC(2026, 8, 28 + i)).toISOString().slice(0, 10))
    render(<AgendaGrid {...base} vue="mois" jours={jours} evenements={[]} />)
    expect(screen.queryByRole('button')).toBeNull()
  })
})
