import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import ParcoursPage from './ParcoursPage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import type { FiltreLiaisons } from '../../../services/ports'
import { PROFILS } from '../../../permissions'
import { axeSansViolation } from './axe'

afterEach(cleanup)
const cleBrute = /\b(parcours[A-Z][a-zA-Z]*|parcours|nav|erreur)\.[a-zA-Z]/
const avec = (surcharge: Partial<Services['parcours']>, participant: Partial<Services['participant']> = {}): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, parcours: { ...s.parcours, ...surcharge }, participant: { ...s.participant, ...participant } }
}
const page = (o: { services?: Services; langue?: 'fr' | 'en' | 'es'; chemin?: string; role?: string } = {}) =>
  rendrePage(<ParcoursPage />, {
    chemin: o.chemin ?? '/des/parcours', motif: '/des/parcours', langue: o.langue, services: o.services,
    session: sessionTest(PROFILS[o.role ?? 'ROLE_CHEF_DDRM'], [o.role ?? 'ROLE_CHEF_DDRM']),
  })

describe('Étape 3 : liste des liaisons du parcours', () => {
  it('nominal : liaisons, statut, étape, conducteur, couvertures et situation servis', async () => {
    page()
    expect(await screen.findByText('LIA-2026-030')).toBeTruthy()
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Liaisons')
    expect(screen.getAllByText('Établissement de paiement A').length).toBeGreaterThan(0)
    expect(screen.getAllByText('En intégration').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Bloquée').length).toBeGreaterThan(0)
    expect(screen.getAllByText('À désigner').length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: 'LIA-2026-030' }).getAttribute('href')).toBe('/des/parcours/LIA-2026-030')
    expect(screen.getByText(/\d+ liaisons/)).toBeTruthy()
  })
  it('chargement, vide et erreur avec identifiant de corrélation et réessai', async () => {
    const { container } = page({ services: avec({ listerLiaisons: () => new Promise(() => undefined) }) })
    expect(container.querySelectorAll('.gm-skeleton').length).toBeGreaterThan(0)
    cleanup()
    page({ services: avec({ listerLiaisons: async () => ({ contenu: [], total: 0 }) }) })
    expect(await screen.findByText('Aucune liaison ouverte')).toBeTruthy()
    expect(screen.getByText(/Ouvrez la première liaison/)).toBeTruthy()
    cleanup()
    let appels = 0
    page({ services: avec({ listerLiaisons: async () => { appels++; throw new ApiError(503, 'HOM_INDISPONIBLE', 'indisponible', 'RQ-HOM-503') } }) })
    expect(await screen.findByText('RQ-HOM-503')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /Réessayer/ }))
    await waitFor(() => expect(appels).toBeGreaterThan(1))
  })
  it('filtres lus dans l’adresse (compteurs du tableau de bord), puces retirables et réinitialisation', async () => {
    const filtres: FiltreLiaisons[] = []
    const s = servicesSimules({ latenceMs: 0 })
    const services = avec({ listerLiaisons: (f) => { filtres.push(f); return s.parcours.listerLiaisons(f) } })
    page({ services, chemin: '/des/parcours?etape=CADRAGE,LANCEMENT,ATELIERS&sansConducteur=1&statut=CADREE&type=CARTE_INTERBANCAIRE&q=banque&page=1' })
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ etape: 'CADRAGE,LANCEMENT,ATELIERS', sansConducteur: true, statut: 'CADREE', typeProjet: 'CARTE_INTERBANCAIRE', recherche: 'banque' }))
    expect(screen.getByText('Étape courante : Cadrage, Lancement, Ateliers')).toBeTruthy()
    expect(screen.getAllByText('Sans conducteur désigné').length).toBeGreaterThan(0)
    fireEvent.click(within(screen.getByText('Statut : Cadrée').closest('.gm-tag') as HTMLElement).getByRole('button'))
    await waitFor(() => expect(filtres.at(-1)?.statut).toBeUndefined())
    fireEvent.click(screen.getByRole('button', { name: /Réinitialiser/ }))
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ etape: undefined, sansConducteur: undefined, recherche: undefined, typeProjet: undefined }))
  })
  it('filtres par sélection : statut, étape, situation bloquée ; recherche différée', async () => {
    const filtres: FiltreLiaisons[] = []
    const s = servicesSimules({ latenceMs: 0 })
    page({ services: avec({ listerLiaisons: (f) => { filtres.push(f); return s.parcours.listerLiaisons(f) } }) })
    await screen.findByText('LIA-2026-030')
    fireEvent.change(screen.getByLabelText('Situation'), { target: { value: 'bloquees' } })
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ bloquees: true }))
    expect(await screen.findByText('LIA-2026-030')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Situation'), { target: { value: 'sansConducteur' } })
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ bloquees: undefined, sansConducteur: true }))
    fireEvent.change(screen.getByLabelText('Statut'), { target: { value: 'EN_TEST' } })
    await waitFor(() => expect(filtres.at(-1)?.statut).toBe('EN_TEST'))
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'Banque B' } })
    await waitFor(() => expect(filtres.at(-1)?.recherche).toBe('Banque B'), { timeout: 3000 })
    expect(await screen.findByText('Aucune liaison ne correspond')).toBeTruthy()
  })
  it('pagination côté serveur : page et taille portées par l’adresse', async () => {
    const filtres: FiltreLiaisons[] = []
    const s = servicesSimules({ latenceMs: 0 })
    page({ services: avec({ listerLiaisons: (f) => { filtres.push(f); return s.parcours.listerLiaisons(f) } }), chemin: '/des/parcours?taille=10' })
    await screen.findByText('LIA-2026-030')
    expect(filtres.at(-1)).toMatchObject({ page: 1, taille: 10 })
    fireEvent.click(screen.getByRole('button', { name: /Suivante/ }))
    await waitFor(() => expect(filtres.at(-1)).toMatchObject({ page: 2, taille: 10 }))
  })
  it('conducteur : onglet « Mes projets » restreint aux liaisons confiées ; pas d’ouverture', async () => {
    const filtres: FiltreLiaisons[] = []
    const s = servicesSimules({ latenceMs: 0 })
    page({ role: 'ROLE_CONDUCTEUR_PROJET', services: avec({ listerLiaisons: (f) => { filtres.push(f); return s.parcours.listerLiaisons(f) } }), chemin: '/des/parcours?mesProjets=1' })
    expect((await screen.findByRole('heading', { level: 1 })).textContent).toBe('Mes projets')
    await waitFor(() => expect(filtres.at(-1)?.mesProjets).toBe(true))
    expect(screen.queryByRole('button', { name: 'Ouvrir une liaison' })).toBeNull()
    fireEvent.click(screen.getByRole('tab', { name: 'Toutes les liaisons' }))
    await waitFor(() => expect(filtres.at(-1)?.mesProjets).toBeUndefined())
  })
  it('matrice des rôles : « Mes projets » ignoré hors conducteur ; ouverture réservée aux chefs de département', async () => {
    const filtres: FiltreLiaisons[] = []
    const s = servicesSimules({ latenceMs: 0 })
    page({ role: 'ROLE_SNA', services: avec({ listerLiaisons: (f) => { filtres.push(f); return s.parcours.listerLiaisons(f) } }), chemin: '/des/parcours?mesProjets=1&formulaire=ouvrir' })
    await screen.findByText('LIA-2026-030')
    expect(filtres.at(-1)?.mesProjets).toBeUndefined()
    expect(screen.queryByRole('tab', { name: 'Mes projets' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Ouvrir une liaison' })).toBeNull()
    expect(screen.queryByRole('dialog')).toBeNull()
  })
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    const { container } = page({ langue })
    await screen.findByText('LIA-2026-030')
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe(langue === 'en' ? 'Links' : 'Enlaces')
    expect(container.textContent).not.toMatch(cleBrute)
  })
  it('accessibilité : axe sans violation, en clair et en sombre', async () => {
    const { container } = page()
    await screen.findByText('LIA-2026-030')
    await axeSansViolation(container)
    document.documentElement.setAttribute('data-theme', 'dark')
    await axeSansViolation(container)
    // Deux audits axe complets d'une liste chargée : délai élargi sous charge (suite complète en parallèle).
  }, 60_000)
})
