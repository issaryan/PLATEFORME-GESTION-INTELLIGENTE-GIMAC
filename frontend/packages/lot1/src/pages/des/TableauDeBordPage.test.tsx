import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, screen, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import TableauDeBordPage from './TableauDeBordPage'
import { rendrePage, sessionTest } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'
import { PROFILS } from '../../permissions'

afterEach(cleanup)
const LECTURE = ['PAR_INSTITUTION_READ']
const CHARGE = [...LECTURE, 'PAR_INSTITUTION_CREATE', 'PAR_ADHESION_INSTRUCT', 'PAR_TRANSITION_REQUEST']
const CHEF = [...CHARGE, 'PAR_TRANSITION_APPROVE']
const INGENIEUR = [...LECTURE, 'HOM_LIAISON_LIST', 'HOM_DASHBOARD_READ', 'HOM_REFERENTIEL_READ', 'HOM_EVIDENCE_SUBMIT']
const page = (permissions: string[], roles: string[] = [], o: { langue?: 'fr' | 'en' | 'es'; services?: Services } = {}) =>
  rendrePage(<TableauDeBordPage />, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', session: sessionTest(permissions, roles), ...o })

// D-58, D-61 : chaque acteur voit son tableau de bord, composé selon la matrice des rôles du jeton.
describe('DES-01 Tableau de bord par acteur', () => {
  it('chef de la DDRM : propositions à trancher, compteurs du registre et profil affiché', async () => {
    page(CHEF, ['ROLE_CHEF_DDRM', 'ROLE_USER'])
    expect(await screen.findByText('Propositions à trancher')).toBeTruthy()
    const compteurs = screen.getByRole('group', { name: 'Relations avec les membres (DDRM)' })
    expect(within(compteurs).getByText('Candidats')).toBeTruthy()
    expect(within(compteurs).getByText('Pièces à examiner')).toBeTruthy()
    // Graphiques : répartition par statut dans l'ordre du cycle de vie (barres cliquables), flux sur 12 mois.
    const repartition = screen.getByRole('region', { name: 'Institutions par statut' })
    expect(within(repartition).getAllByRole('listitem').map((l) => l.getAttribute('aria-label')?.split(' : ')[0])).toEqual(['Candidats', 'Adhérents sans liaison', 'En homologation', 'Clos sans suite'])
    const flux = screen.getByRole('region', { name: 'Flux des 12 derniers mois' })
    expect(within(flux).getByText('Prises de contact')).toBeTruthy()
    expect(within(flux).getByText('Adhésions')).toBeTruthy()
    expect(within(repartition).getByRole('button', { name: 'En homologation' })).toBeTruthy()
    // Personne connectée et profil en texte simple, à droite du titre.
    expect(screen.getByText('Agent de test').parentElement?.textContent).toBe('Agent de test · Chef de la DDRM')
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
    // Le reste du tableau de bord demeure ; les raccourcis « Mes accès » sont réservés aux profils sans section.
    expect(screen.getByRole('heading', { level: 1, name: 'Tableau de bord' })).toBeTruthy()
    expect(screen.queryByText('Mes accès')).toBeNull()
  })
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    const { container } = page(CHEF, ['ROLE_CHEF_DDRM'], { langue })
    await screen.findByText(langue === 'en' ? 'Proposals to decide' : 'Propuestas por decidir')
    expect(container.textContent).not.toMatch(/\b(tableau|profil|nav)\.[a-zA-Z]/)
  })

  // D-79 : widgets dérivés du registre des fonctionnalités et des permissions, jamais du nom du rôle.
  it('« Mon travail » ouvert par les permissions seules, sans rôle de fonction ; la lecture seule n’y suffit pas', async () => {
    page(INGENIEUR, [])
    expect(await screen.findByRole('region', { name: 'Mon travail' })).toBeTruthy()
    cleanup()
    page([...LECTURE, 'HOM_LIAISON_LIST', 'HOM_DASHBOARD_READ'], ['ROLE_CHEF_DES'])
    expect(await screen.findByRole('region', { name: 'Mes accès' })).toBeTruthy()
    expect(screen.queryByRole('region', { name: 'Mon travail' })).toBeNull()
  })
  it('administrateur système : « Ma journée », supervision et infrastructure (raccourcis sans widget fourni), sans « Mes accès »', async () => {
    page(PROFILS.ROLE_ADMIN_SYSTEME, ['ROLE_ADMIN_SYSTEME'])
    expect(await screen.findByRole('region', { name: 'Ma journée' })).toBeTruthy()
    expect(screen.getByRole('region', { name: 'Supervision' })).toBeTruthy()
    expect(screen.getByRole('region', { name: 'Infrastructure' })).toBeTruthy()
    expect(screen.getAllByRole('link', { name: 'Ouvrir' }).map((l) => l.getAttribute('href'))).toEqual(['/des/plateforme/supervision', '/des/plateforme/infrastructure'])
    expect(screen.queryByRole('region', { name: 'Mes accès' })).toBeNull()
  })
  it('collaborateur : « Ma journée » puis les raccourcis de ses fonctionnalités (messagerie, Mon activité)', async () => {
    page(PROFILS.ROLE_COLLABORATEUR, ['ROLE_COLLABORATEUR'])
    const acces = await screen.findByRole('region', { name: 'Mes accès' })
    expect(acces.textContent).toContain('Messagerie')
    expect(acces.textContent).toContain('Agenda')
    expect(screen.getByRole('region', { name: 'Ma journée' })).toBeTruthy()
  })
})
