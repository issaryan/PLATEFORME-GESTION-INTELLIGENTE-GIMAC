import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, screen } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import TableauDeBordPage from './TableauDeBordPage'
import { rendrePage, sessionTest } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'

afterEach(cleanup)
const LECTURE = ['PAR_INSTITUTION_READ']
const CHARGE = [...LECTURE, 'PAR_INSTITUTION_CREATE', 'PAR_ADHESION_INSTRUCT', 'PAR_TRANSITION_REQUEST']
const CHEF = [...CHARGE, 'PAR_TRANSITION_APPROVE']
const INGENIEUR = [...LECTURE, 'HOM_LIAISON_LIST', 'HOM_DASHBOARD_READ', 'HOM_REFERENTIEL_READ']
const page = (permissions: string[], roles: string[] = [], o: { langue?: 'fr' | 'en' | 'es'; services?: Services } = {}) =>
  rendrePage(<TableauDeBordPage />, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', session: sessionTest(permissions, roles), ...o })

// D-58, D-61 : chaque acteur voit son tableau de bord, composé selon la matrice des rôles du jeton.
describe('DES-01 Tableau de bord par acteur', () => {
  it('chef de la DDRM : propositions à trancher, compteurs du registre et profil affiché', async () => {
    page(CHEF, ['ROLE_CHEF_DDRM', 'ROLE_USER'])
    expect(await screen.findByText('Propositions à trancher')).toBeTruthy()
    expect(screen.getByText('Candidats')).toBeTruthy()
    expect(screen.getByText('Pièces à examiner')).toBeTruthy()
    expect(screen.getByText('Chef de la DDRM')).toBeTruthy()
    expect(screen.getByText('Bienvenue, Agent de test')).toBeTruthy()
  })
  it('chargé de la DDRM : ses dossiers en cours, sans propositions à trancher', async () => {
    page(CHARGE, ['ROLE_CHARGE_DDRM'])
    expect(await screen.findByText('Mes dossiers en cours')).toBeTruthy()
    expect(await screen.findByText('Fintech Kribi Pay')).toBeTruthy()
    expect(screen.queryByText('Propositions à trancher')).toBeNull()
  })
  it('ingénieur d’homologation : « Mon travail » de Normes et Agréments, sans section DDRM ni raccourcis', async () => {
    page(INGENIEUR, ['ROLE_INGENIEUR_HOMOLOGATION'])
    expect(await screen.findByRole('region', { name: 'Mon travail' })).toBeTruthy()
    expect(screen.queryByText('Relations avec les membres (DDRM)')).toBeNull()
    expect(screen.queryByRole('region', { name: 'Mes accès' })).toBeNull()
  })
  it('lecture seule du registre : raccourcis vers les écrans ouverts par la matrice', async () => {
    page(LECTURE, ['ROLE_LECTEUR'])
    expect(await screen.findByRole('region', { name: 'Mes accès' })).toBeTruthy()
    expect(screen.getByText('Institutions membres')).toBeTruthy()
  })
  it('profil sans section : état vide explicite, aucun accès proposé', async () => {
    page([], ['ROLE_USER'])
    expect(await screen.findByText('Aucun tableau de bord pour votre profil')).toBeTruthy()
    expect(screen.queryByText('Mes accès')).toBeNull()
  })
  it('compteurs indisponibles : erreur signalée sans masquer le reste', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const services = { ...s, participant: { ...s.participant, indicateurs: async () => { throw new ApiError(503, 'X', 'x', 'c') } } }
    page(CHEF, [], { services })
    expect(await screen.findByText('Le chargement a échoué.')).toBeTruthy()
    expect(screen.getByText('Mes accès')).toBeTruthy()
  })
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    const { container } = page(CHEF, ['ROLE_CHEF_DDRM'], { langue })
    await screen.findByText(langue === 'en' ? 'Proposals to decide' : 'Propuestas por decidir')
    expect(container.textContent).not.toMatch(/\b(tableau|profil|nav)\.[a-zA-Z]/)
  })
})
