import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { SideNav, type SideNavGroup } from '../src'

// Sections repliables de la barre latérale (application du DES, 05/11/2026) : accordéons intégrés, section de
// l'écran courant ouverte, état mémorisé dans le stockage local, pastille d'écran courant, mode réduit déplié.
const GROUPES: SideNavGroup[] = [
  { items: [{ key: 'accueil', label: 'Tableau de bord', icon: 'gauge', href: '/des/tableau-de-bord' }] },
  { cle: 'nav.groupe.monActivite', label: 'Mon activité', items: [{ key: 'agenda', label: 'Agenda', icon: 'calendar', href: '/des/agenda' }] },
  { cle: 'nav.groupe.sandbox', label: 'Sandbox', items: [{ key: 'activite', label: 'Activité des tests', icon: 'network', href: '/des/sandbox/activite' }] },
]
const MEMOIRE = 'gm.nav.sections.test'
const section = (nom: string) => screen.getByRole('button', { name: new RegExp(nom) })

describe('SideNav, sections repliables', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
  })

  it('sans option : titres simples, toutes les entrées présentes', () => {
    render(<SideNav groups={GROUPES} current="agenda" />)
    expect(screen.queryAllByRole('button')).toHaveLength(0)
    expect(screen.getByRole('link', { name: 'Activité des tests' })).toBeTruthy()
  })

  it('ouvre la section de l’écran courant, replie les autres, et mémorise chaque bascule', () => {
    const { unmount } = render(<SideNav groups={GROUPES} current="activite" repliable memoire={MEMOIRE} />)
    expect(screen.getByRole('link', { name: 'Tableau de bord' })).toBeTruthy()
    expect(section('Sandbox').getAttribute('aria-expanded')).toBe('true')
    expect(section('Mon activité').getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByRole('link', { name: 'Agenda' })).toBeNull()
    fireEvent.click(section('Mon activité'))
    expect(screen.getByRole('link', { name: 'Agenda' })).toBeTruthy()
    expect(document.getElementById(section('Mon activité').getAttribute('aria-controls') as string)).toBeTruthy()
    expect(JSON.parse(localStorage.getItem(MEMOIRE) as string)).toEqual(['nav.groupe.sandbox', 'nav.groupe.monActivite'])
    unmount()
    render(<SideNav groups={GROUPES} current="accueil" repliable memoire={MEMOIRE} />)
    expect(section('Mon activité').getAttribute('aria-expanded')).toBe('true')
  })

  it('section courante repliée par l’utilisateur : pastille « écran courant » sur son titre', () => {
    render(<SideNav groups={GROUPES} current="activite" repliable memoire={MEMOIRE} />)
    fireEvent.click(section('Sandbox'))
    expect(section('Sandbox').getAttribute('aria-expanded')).toBe('false')
    expect(screen.getByRole('img', { name: 'Écran courant dans cette section' })).toBeTruthy()
    expect(screen.queryByRole('link', { name: 'Activité des tests' })).toBeNull()
  })

  it('stockage illisible, mal formé ou refusé : état de la page seulement, sans erreur', () => {
    localStorage.setItem(MEMOIRE, '{pas du json')
    render(<SideNav groups={GROUPES} current="activite" repliable memoire={MEMOIRE} />)
    expect(section('Sandbox').getAttribute('aria-expanded')).toBe('true')
    cleanup()
    localStorage.setItem(MEMOIRE, JSON.stringify({ ouverts: true }))
    render(<SideNav groups={GROUPES} current="agenda" repliable memoire={MEMOIRE} />)
    expect(section('Sandbox').getAttribute('aria-expanded')).toBe('false')
    cleanup()
    localStorage.clear()
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('quota') })
    render(<SideNav groups={GROUPES} current="activite" repliable memoire={MEMOIRE} />)
    fireEvent.click(section('Mon activité'))
    expect(screen.getByRole('link', { name: 'Agenda' })).toBeTruthy()
  })

  it('sans mémoire ni écran courant connu, et en mode réduit : rien de replié', () => {
    render(<SideNav groups={GROUPES} current="inconnu" repliable />)
    expect(section('Sandbox').getAttribute('aria-expanded')).toBe('false')
    fireEvent.click(section('Sandbox'))
    expect(screen.getByRole('link', { name: 'Activité des tests' })).toBeTruthy()
    cleanup()
    render(<SideNav groups={GROUPES} current="activite" repliable memoire={MEMOIRE} reduite />)
    expect(screen.queryAllByRole('button')).toHaveLength(0)
    expect(screen.getByRole('link', { name: 'Agenda' })).toBeTruthy()
  })
})
