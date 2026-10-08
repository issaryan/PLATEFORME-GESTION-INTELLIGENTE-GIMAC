import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import LiaisonPage from './LiaisonPage'
import LiaisonsPage from './LiaisonsPage'
import InstitutionsPage from './InstitutionsPage'
import TableauDeBordPage from './TableauDeBordPage'
import { rendrePage, sessionTest } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'
import { mesProjetsParDefaut } from './liaisons/communs'

// Cadrage (D-57, D-58) : résumé sous l'en-tête de la liaison, désignation motivée du conducteur de
// projet (HOM_PROJECT_DESIGNATE et actionsPermises), « Mes projets » et tableaux de bord par acteur.
afterEach(cleanup)
const LECTURE = ['PAR_INSTITUTION_READ', 'HOM_LIAISON_LIST', 'HOM_LIAISON_READ', 'HOM_DASHBOARD_READ', 'HOM_REFERENTIEL_READ', 'CER_DOSSIER_READ']
const CHEF_DES = [...LECTURE, 'HOM_LIAISON_OPEN', 'HOM_PROJECT_DESIGNATE', 'HOM_LIAISON_UNFREEZE', 'HOM_EVIDENCE_FILE_EXPORT']
const CHEF_DDRM = [...LECTURE, 'PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', 'PAR_ADHESION_INSTRUCT', 'PAR_TRANSITION_REQUEST', 'PAR_TRANSITION_APPROVE', 'HOM_LIAISON_OPEN', 'HOM_PROJECT_DESIGNATE']
const CONDUCTEUR = [...LECTURE, 'HOM_PROJECT_CONDUCT', 'HOM_OPERATION_ASSIGN', 'HOM_EVIDENCE_SUBMIT', 'HOM_EVIDENCE_FILE_EXPORT']
const SNA = [...LECTURE, 'HOM_EVIDENCE_SUBMIT', 'HOM_OPERATION_VALIDATE']

const avec = (surcharge: Partial<Services['homologation']>): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, homologation: { ...s.homologation, ...surcharge } }
}
const liaison = (id: string, o: { permissions?: string[]; roles?: string[]; suffixe?: string; services?: Services } = {}) =>
  rendrePage(<LiaisonPage />, {
    chemin: `/des/liaisons/${id}${o.suffixe ?? ''}`, motif: '/des/liaisons/:id', services: o.services ?? servicesSimules({ latenceMs: 0 }),
    session: sessionTest(o.permissions ?? CHEF_DES, o.roles ?? ['ROLE_CHEF_DES']),
  })
const liste = (o: { permissions?: string[]; roles?: string[]; chemin?: string; services?: Services } = {}) =>
  rendrePage(<LiaisonsPage />, {
    chemin: o.chemin ?? '/des/liaisons', motif: '/des/liaisons', services: o.services, session: sessionTest(o.permissions ?? CHEF_DES, o.roles ?? ['ROLE_CHEF_DES']),
    autres: [{ motif: '/des/institutions', element: <InstitutionsPage /> }],
  })
const tableau = (permissions: string[], roles: string[], services?: Services) =>
  rendrePage(<TableauDeBordPage />, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', services: services ?? servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-TEST', nom: 'Agent de test', profils: roles } }), session: sessionTest(permissions, roles) })
const resume = () => screen.getByRole('heading', { name: 'Cadrage de la liaison' }).closest('section')!

describe('Liaison au cadrage : résumé et désignation du conducteur', () => {
  it('résumé : phase de cadrage, périmètre avec le socle transverse, intervenants', async () => {
    liaison('LIA-2026-030')
    await screen.findByRole('heading', { level: 1, name: 'Établissement de microfinance C' })
    const r = resume()
    expect(within(r).getByText('Cadrage')).toBeTruthy()
    for (const s of ['Transverse', 'Transfert', 'Paiement']) expect(within(r).getByText(s)).toBeTruthy()
    expect(within(r).getByText('Institution membre')).toBeTruthy()
    expect(within(r).getByText('Aïcha Mahamat Nour')).toBeTruthy()
  })
  it('résumé sans conducteur ni périmètre renvoyés : signalé', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const l = await s.homologation.lireLiaison('LIA-2026-030')
    liaison('LIA-2026-030', { services: avec({ lireLiaison: async () => ({ ...l, intervenants: undefined, perimetre: undefined, phaseCourante: 'Certification' }) }) })
    await screen.findByRole('heading', { level: 1 })
    expect(within(resume()).getByText('aucun conducteur désigné')).toBeTruthy()
    expect(within(resume()).getByText('Certification')).toBeTruthy()
  })
  it('chef du DES : désigne un autre conducteur avec un motif ; les intervenants sont à jour', async () => {
    const appels: unknown[][] = []
    const base = servicesSimules({ latenceMs: 0 })
    const services: Services = { ...base, homologation: { ...base.homologation, designerChefDeProjet: async (...a) => { appels.push(a); return base.homologation.designerChefDeProjet(...a) } } }
    const { container } = liaison('LIA-2026-030', { services })
    fireEvent.click(await screen.findByRole('button', { name: 'Désigner le conducteur' }))
    expect(await screen.findByRole('dialog', { name: 'Désigner le conducteur de projet de la liaison de Établissement de microfinance C' })).toBeTruthy()
    await waitFor(() => expect(container.querySelector('select[name="chefDeProjetId"] option[value="U-203"]')).toBeTruthy())
    fireEvent.change(container.querySelector('select[name="chefDeProjetId"]')!, { target: { value: 'U-203' } })
    fireEvent.change(container.querySelector('textarea[name="motif"]')!, { target: { value: 'Réorganisation du portefeuille' } })
    fireEvent.submit(container.querySelector('form')!)
    await waitFor(() => expect(appels).toEqual([['LIA-2026-030', 'U-203', 'Réorganisation du portefeuille']]))
    expect(await within(await waitFor(resume)).findByText('Estelle Ngo Bassong')).toBeTruthy()
  })
  it('désignation sans motif ; conducteur requis ; refus du service traduit', async () => {
    const services = avec({ designerChefDeProjet: async () => { throw new ApiError(409, 'HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED', 'x', 'RQ-DES-1') } })
    const { container } = liaison('LIA-2026-030', { services, suffixe: '?formulaire=chef' })
    const f = await waitFor(() => { const x = container.querySelector('form'); if (!x) throw new Error('absent'); return x })
    fireEvent.change(container.querySelector('select[name="chefDeProjetId"]')!, { target: { value: '' } })
    fireEvent.submit(f)
    expect((await screen.findAllByText(/Désignez le conducteur de projet\./)).length).toBeGreaterThan(0)
    await waitFor(() => expect(container.querySelector('select[name="chefDeProjetId"] option[value="U-202"]')).toBeTruthy())
    fireEvent.change(container.querySelector('select[name="chefDeProjetId"]')!, { target: { value: 'U-202' } })
    fireEvent.submit(f)
    expect(await screen.findByText('Le collaborateur choisi ne détient pas le profil de conducteur de projet.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Annuler' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Établissement de microfinance C' })).toBeTruthy()
  })
  it('matrice des rôles : ni le conducteur ni l’adresse n’ouvrent la désignation sans HOM_PROJECT_DESIGNATE', async () => {
    liaison('LIA-2026-030', { permissions: CONDUCTEUR, roles: ['ROLE_CONDUCTEUR_PROJET'], suffixe: '?formulaire=chef' })
    await screen.findByRole('heading', { level: 1, name: 'Établissement de microfinance C' })
    expect(screen.queryByRole('button', { name: 'Désigner le conducteur' })).toBeNull()
  })
  it('liaison qui ne permet plus la désignation : action absente', async () => {
    liaison('LIA-2025-102')
    await screen.findByRole('heading', { level: 1, name: 'Banque B' })
    expect(screen.queryByRole('button', { name: 'Désigner le conducteur' })).toBeNull()
  })
})

describe('Liaisons : « Mes projets » et désignation depuis la liste', () => {
  it('règle d’activation par défaut', () => {
    expect(mesProjetsParDefaut(['ROLE_CONDUCTEUR_PROJET'], CONDUCTEUR)).toBe(true)
    expect(mesProjetsParDefaut(['ROLE_CONDUCTEUR_PROJET', 'ROLE_CHEF_DES'], CHEF_DES)).toBe(false)
    expect(mesProjetsParDefaut(['ROLE_INGENIEUR_HOMOLOGATION'], LECTURE)).toBe(false)
    expect(mesProjetsParDefaut(undefined, undefined)).toBe(false)
  })
  it('conducteur : « Mes projets » activé d’emblée, puis désactivé pour tout voir', async () => {
    liste({ permissions: CONDUCTEUR, roles: ['ROLE_CONDUCTEUR_PROJET'] })
    const bascule = await screen.findByRole('switch', { name: 'Mes projets' })
    expect((bascule as HTMLInputElement).checked).toBe(true)
    expect(await screen.findByText('Établissement de microfinance C')).toBeTruthy()
    expect(screen.queryByText('Banque D')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Ouvrir une liaison' })).toBeNull()
    fireEvent.click(bascule)
    expect(await screen.findByText('Banque D')).toBeTruthy()
  })
  it('chef du DES : toutes les liaisons, filtre « Mes projets » à la demande, ouverture renvoyée au registre', async () => {
    liste()
    expect(await screen.findByText('Banque D')).toBeTruthy()
    expect((screen.getByRole('switch', { name: 'Mes projets' }) as HTMLInputElement).checked).toBe(false)
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir une liaison' }))
    expect(await screen.findByText('Mobile Money Sanaga')).toBeTruthy()
  })
  it('filtre d’état porté par l’adresse (tuile « Liaisons en cadrage »)', async () => {
    liste({ chemin: '/des/liaisons?etat=OUVERTE' })
    expect(await screen.findByText('Établissement de microfinance C')).toBeTruthy()
    await waitFor(() => expect(screen.queryByText('Banque D')).toBeNull())
  })
  it('filtres, recherche et pagination transmis au service', async () => {
    const requetes: unknown[] = []
    const base = servicesSimules({ latenceMs: 0 })
    const services: Services = { ...base, homologation: { ...base.homologation, listerLiaisons: (q) => { requetes.push(q); return base.homologation.listerLiaisons(q) } } }
    liste({ services })
    await screen.findByText('Banque D')
    fireEvent.change(screen.getByRole('searchbox', { name: 'Rechercher' }), { target: { value: 'Banque' } })
    await waitFor(() => expect(requetes.some((q) => (q as { recherche?: string }).recherche === 'Banque')).toBe(true))
  })
  it('désignation depuis une ligne de la liste, puis retour à la liste', async () => {
    const { container } = liste({ chemin: '/des/liaisons?mesProjets=0' })
    await screen.findByText('Établissement de microfinance C')
    fireEvent.click(screen.getAllByRole('button', { name: 'Désigner le conducteur' })[0])
    expect(await screen.findByText(/Désigner le conducteur de projet de la liaison de/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Annuler' }))
    expect(await screen.findByRole('switch', { name: 'Mes projets' })).toBeTruthy()
    expect(container.querySelector('form')).toBeNull()
  })
})

describe('Tableaux de bord : « Mon travail » de chaque fonction (D-58)', () => {
  const travail = () => screen.findByRole('region', { name: 'Mon travail' })
  it('chef du DES : liaisons sans conducteur à désigner, ouverture d’une liaison, pilotage', async () => {
    tableau(CHEF_DES, ['ROLE_CHEF_DES'])
    const s = await travail()
    expect(await within(s).findByText('Conducteurs à désigner')).toBeTruthy()
    expect(await within(s).findByRole('heading', { name: /Liaison sans conducteur/ })).toBeTruthy()
    expect(within(s).getAllByRole('button', { name: 'Désigner le conducteur' }).length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: 'Ouvrir une liaison' })).toBeTruthy()
    expect(await screen.findByRole('region', { name: 'Pilotage des projets de certification' })).toBeTruthy()
    // Plus de section héritée du premier modèle ni de raccourcis en double de la navigation.
    expect(screen.queryByRole('region', { name: 'Cadrage des liaisons' })).toBeNull()
    expect(screen.queryByRole('region', { name: 'Mes accès' })).toBeNull()
  })
  it('chef de la DDRM : section DDRM, liaisons en phase amont', async () => {
    tableau(CHEF_DDRM, ['ROLE_CHEF_DDRM'])
    const s = await travail()
    expect(await within(s).findByRole('heading', { name: /Liaisons en phase amont/ })).toBeTruthy()
    expect(screen.getByRole('region', { name: 'Relations avec les membres (DDRM)' })).toBeTruthy()
  })
  it('conducteur de projet : ses projets et les procès-verbaux à établir', async () => {
    tableau(CONDUCTEUR, ['ROLE_CONDUCTEUR_PROJET'])
    const s = await travail()
    expect(await within(s).findByText('Procès-verbaux à établir')).toBeTruthy()
    expect(await within(s).findByRole('heading', { name: /^Mes projets \(/ })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Ouvrir une liaison' })).toBeNull()
  })
  it('conducteur sans projet : état explicite', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const vide = { ...s, parcours: { ...s.parcours, tableauActeur: async () => ({ fonction: 'CONDUCTEUR' as const, compteurs: [], taches: [], liaisons: [], blocages: [] }) } }
    tableau(CONDUCTEUR, ['ROLE_CONDUCTEUR_PROJET'], vide)
    expect(await screen.findByText('Aucune tâche à réaliser')).toBeTruthy()
    expect(screen.getAllByText('Aucune liaison').length).toBeGreaterThan(0)
  })
  it('normes et agréments : ses compteurs et ses tâches, sans pilotage consolidé', async () => {
    tableau(SNA, ['ROLE_SNA'])
    const s = await travail()
    expect(await within(s).findByText('Essais en cours')).toBeTruthy()
    expect(screen.queryByRole('region', { name: 'Pilotage des projets de certification' })).toBeNull()
    expect(screen.queryByRole('region', { name: 'Mes accès' })).toBeNull()
  })
})
