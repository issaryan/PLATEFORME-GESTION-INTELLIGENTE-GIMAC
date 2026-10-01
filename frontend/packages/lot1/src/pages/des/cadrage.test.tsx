import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import i18next from 'i18next'
import { ApiError } from '@gimac/socle'
import InstitutionPage from './InstitutionPage'
import InstitutionsPage from './InstitutionsPage'
import LiaisonPage from './LiaisonPage'
import { rendrePage, sessionTest } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'

// Étape 3 du parcours, cadrage (D-57, D-58) : ouverture d'une liaison depuis la fiche d'une institution
// adhérente ou en homologation par un chef de département (HOM_LIAISON_OPEN et HOM_PROJECT_DESIGNATE),
// refus de MS-HOMOLOGATION traduits, bascule de l'institution en homologation, onglet des liaisons.
afterEach(cleanup)
const LECTURE = ['PAR_INSTITUTION_READ', 'HOM_LIAISON_LIST', 'HOM_LIAISON_READ', 'HOM_DASHBOARD_READ']
const CHEF_DDRM = [...LECTURE, 'PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', 'PAR_ADHESION_INSTRUCT', 'PAR_TRANSITION_REQUEST', 'PAR_TRANSITION_APPROVE', 'HOM_LIAISON_OPEN', 'HOM_PROJECT_DESIGNATE']
const CHEF_DES = [...LECTURE, 'HOM_LIAISON_OPEN', 'HOM_PROJECT_DESIGNATE', 'HOM_LIAISON_UNFREEZE']
const INGENIEUR = [...LECTURE, 'HOM_OPERATION_ASSIGN']
const cleBrute = /\b(cadrage|phase|service|intervenant|institution|transition|erreur|liaison)\.[a-zA-Z]/

const avec = (surcharge: Partial<Services['homologation']>): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, homologation: { ...s.homologation, ...surcharge } }
}
function fiche(id: string, o: { permissions?: string[]; suffixe?: string; services?: Services; langue?: 'fr' | 'en' | 'es' } = {}) {
  const services = o.services ?? servicesSimules({ latenceMs: 0 })
  const r = rendrePage(<InstitutionPage />, {
    chemin: `/des/institutions/${id}${o.suffixe ?? ''}`, motif: '/des/institutions/:id', services, langue: o.langue, session: sessionTest(o.permissions ?? CHEF_DDRM, ['ROLE_CHEF_DDRM']),
    autres: [{ motif: '/des/liaisons/:id', element: <LiaisonPage /> }],
  })
  return { ...r, services }
}
const formulaire = (c: HTMLElement) => waitFor(() => {
  const f = c.querySelector('form')
  if (!f) throw new Error('formulaire absent')
  return f
})
const choisir = async (c: HTMLElement, o: { type?: string; conducteur?: string; banque?: string }) => {
  if (o.type) fireEvent.click(c.querySelector(`input[type="radio"][value="${o.type}"]`)!)
  if (o.conducteur) {
    await waitFor(() => expect(c.querySelector(`select[name="chefDeProjetId"] option[value="${o.conducteur}"]`)).toBeTruthy())
    fireEvent.change(c.querySelector('select[name="chefDeProjetId"]')!, { target: { value: o.conducteur } })
  }
  if (o.banque) {
    await waitFor(() => expect(c.querySelector(`select[name="banquePartenaireId"] option[value="${o.banque}"]`)).toBeTruthy())
    fireEvent.change(c.querySelector('select[name="banquePartenaireId"]')!, { target: { value: o.banque } })
  }
}

describe('Cadrage : ouverture d’une liaison depuis la fiche de l’institution', () => {
  it('chef de la DDRM : ouvre la liaison, arrive sur sa fiche, l’institution passe en homologation', async () => {
    const { container, services } = fiche('INST-0105')
    fireEvent.click(await screen.findByRole('button', { name: 'Ouvrir une liaison (cadrage)' }))
    expect(await screen.findByRole('dialog', { name: 'Ouvrir une liaison (cadrage) pour Mobile Money Sanaga' })).toBeTruthy()
    expect(screen.getByText(/fera ensuite passer Mobile Money Sanaga du statut « Adhérent » au statut « En homologation »/)).toBeTruthy()
    const f = await formulaire(container)
    await choisir(container, { type: 'PAIEMENT_MOBILE', conducteur: 'U-202', banque: 'INST-0017' })
    fireEvent.submit(f)
    expect(await screen.findByText(/ouverte en phase de cadrage\. Le statut de Mobile Money Sanaga passera à « En homologation »/)).toBeTruthy()
    expect(screen.getByRole('heading', { level: 1, name: 'Mobile Money Sanaga' })).toBeTruthy()
    const resume = screen.getByRole('heading', { name: 'Cadrage de la liaison' }).closest('section')!
    expect(within(resume).getByText('Transverse')).toBeTruthy()
    expect(within(resume).getByText('Brice Ondo Mba')).toBeTruthy()
    expect(within(resume).getByText('Banque B')).toBeTruthy()
    await waitFor(async () => expect((await services.participant.dossier('INST-0105')).institution.statut).toBe('EN_HOMOLOGATION'))
  })
  it('champs requis signalés avant tout appel au service', async () => {
    const appels: string[] = []
    const services = avec({ ouvrirLiaison: async () => { appels.push('ouvrir'); throw new Error('non attendu') } })
    const { container } = fiche('INST-0105', { suffixe: '?formulaire=liaison', services })
    fireEvent.submit(await formulaire(container))
    expect((await screen.findAllByText(/Choisissez le type de projet\./)).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/Désignez le conducteur de projet\./).length).toBeGreaterThan(0)
    expect(appels).toEqual([])
  })
  it('institution déjà en homologation : ouverture d’un autre projet, statut inchangé annoncé', async () => {
    fiche('INST-0042', { suffixe: '?formulaire=liaison', permissions: CHEF_DES })
    expect(await screen.findByText(/est déjà en homologation : la nouvelle liaison s'ajoute/)).toBeTruthy()
  })
  it.each([
    ['HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE', { statut: 'SUSPENDU' }, /son statut est « Suspendu »/],
    ['HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE', { statut: null }, /son statut est « inconnu »/],
    ['HOM_LIAISON_DUPLICATE', {}, /déjà ouverte pour cette institution membre et ce type de projet/],
    ['HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED', {}, /ne détient pas le profil de conducteur de projet/],
    ['HOM_LIAISON_CARRIER_NOT_BANK', {}, /n'est pas une banque/],
    ['HOM_REFERENTIAL_NONE_PUBLISHED', {}, /Aucun référentiel d'opérations n'est publié/],
    ['HOM_PROJECT_DESIGNATION_FORBIDDEN', {}, /droit de désigner un conducteur de projet/],
    ['HOM_PARTICIPANT_UNAVAILABLE', {}, /MS-PARTICIPANT\) ne répond pas/],
    ['HOM_IAM_UNAVAILABLE', {}, /MS-IAM\) ne répond pas/],
  ])('refus %s traduit, avec l’identifiant de corrélation', async (code, details, attendu) => {
    const statut = code.endsWith('UNAVAILABLE') ? 503 : code.endsWith('FORBIDDEN') ? 403 : 409
    const services = avec({ ouvrirLiaison: async () => { throw new ApiError(statut, code, 'refus', `RQ-${code}`, details) } })
    const { container } = fiche('INST-0105', { suffixe: '?formulaire=liaison', services })
    const f = await formulaire(container)
    await choisir(container, { type: 'CARTE_INTERBANCAIRE', conducteur: 'U-201' })
    fireEvent.submit(f)
    expect(await screen.findByText(attendu)).toBeTruthy()
    expect(screen.getByText(`RQ-${code}`)).toBeTruthy()
  })
  it('annuler revient à la fiche', async () => {
    fiche('INST-0105', { suffixe: '?formulaire=liaison' })
    fireEvent.click(await screen.findByRole('button', { name: 'Annuler' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Mobile Money Sanaga' })).toBeTruthy()
  })
  it('matrice des rôles : ni l’ingénieur d’homologation ni une permission seule n’ouvrent', async () => {
    fiche('INST-0105', { permissions: INGENIEUR })
    await screen.findByRole('heading', { level: 1, name: 'Mobile Money Sanaga' })
    expect(screen.queryByRole('button', { name: 'Ouvrir une liaison (cadrage)' })).toBeNull()
    cleanup()
    fiche('INST-0105', { permissions: [...LECTURE, 'HOM_LIAISON_OPEN'], suffixe: '?formulaire=liaison' })
    expect(await screen.findByRole('heading', { level: 1, name: 'Mobile Money Sanaga' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Ouvrir une liaison (cadrage)' })).toBeNull()
  })
  it('candidat : aucune ouverture proposée, même par l’adresse', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const id = (await s.participant.listerInstitutions({ page: 1, taille: 1, statut: ['CANDIDAT'] })).contenu[0].id
    fiche(id, { services: s, suffixe: '?formulaire=liaison' })
    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('button', { name: 'Ouvrir une liaison (cadrage)' })).toBeNull()
    expect(screen.queryByText(/Ouvrir une liaison \(cadrage\) pour/)).toBeNull()
  })
  it('onglet Liaisons : liaisons de l’institution, ou aucune', async () => {
    fiche('INST-0042', { suffixe: '?onglet=liaisons' })
    const l = await screen.findByRole('list', { name: "Liaisons de l'institution" })
    expect(within(l).getByRole('link', { name: 'Carte interbancaire (LIA-2026-017)' }).getAttribute('href')).toBe('/des/liaisons/LIA-2026-017')
    cleanup()
    fiche('INST-0105', { suffixe: '?onglet=liaisons' })
    expect(await screen.findByText("Aucune liaison n'est ouverte pour cette institution.")).toBeTruthy()
  })
  it('propositions de statut : la bascule automatique est distinguée', async () => {
    fiche('INST-0042', { suffixe: '?onglet=transitions' })
    expect(await screen.findByText('automatique')).toBeTruthy()
    expect(screen.getByText('sur proposition')).toBeTruthy()
    expect(screen.getByText('Ouverture de la liaison LIA-2026-017')).toBeTruthy()
  })
  it('lignes du dossier rattachées à l’institution : les vues du service ne la portent pas', async () => {
    const base = servicesSimules({ latenceMs: 0 })
    const id = (await base.participant.listerInstitutions({ page: 1, taille: 50, recherche: 'Libreville' })).contenu[0].id
    const t = await base.participant.proposerTransition(id, 'ADHERENT', 'Dossier complet')
    const approbations: string[] = []
    const services: Services = {
      ...base,
      reauth: { assertion: async () => 'assertion' },
      participant: {
        ...base.participant,
        // Forme de TransitionVue et de PieceVue de MS-PARTICIPANT : sans institutionId.
        dossier: async (i) => { const d = await base.participant.dossier(i); return { ...d, transitions: d.transitions.map(({ institutionId: _i, ...x }) => x), pieces: d.pieces.map(({ institutionId: _i, ...p }) => p) } },
        approuverTransition: async (i, tr, a) => { approbations.push(`${i}/${tr}`); return base.participant.approuverTransition(i, tr, a) },
      },
    }
    fiche(id, { services, suffixe: '?onglet=transitions' })
    fireEvent.click(await screen.findByRole('button', { name: 'Approuver' }))
    const d = await waitFor(() => { const x = screen.getAllByRole('dialog').find((y) => within(y).queryByRole('button', { name: 'Utiliser ma clé de sécurité' })); if (!x) throw new Error('absent'); return x })
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    await waitFor(() => expect(approbations).toEqual([`${id}/${t.id}`]))
  })
  it('registre : l’ouverture est proposée sur la ligne d’un adhérent', async () => {
    rendrePage(<InstitutionsPage />, { chemin: '/des/institutions?statut=ADHERENT', motif: '/des/institutions', session: sessionTest(CHEF_DES) })
    await screen.findByText('Mobile Money Sanaga')
    expect(screen.getAllByRole('button', { name: 'Ouvrir une liaison (cadrage)' }).length).toBeGreaterThan(0)
  })
  it.each(['en', 'es'] as const)('formulaire et fiche en %s sans clé brute', async (langue) => {
    const { container } = fiche('INST-0105', { suffixe: '?formulaire=liaison', langue })
    await formulaire(container)
    await waitFor(() => expect(container.querySelectorAll('select[name="chefDeProjetId"] option').length).toBe(4))
    expect(container.textContent).not.toMatch(cleBrute)
  })
  it('chaque refus du service a son message en français, anglais et espagnol', () => {
    const codes = ['HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE', 'HOM_LIAISON_DUPLICATE', 'HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED', 'HOM_LIAISON_CARRIER_NOT_BANK',
      'HOM_REFERENTIAL_NONE_PUBLISHED', 'HOM_PROJECT_DESIGNATION_FORBIDDEN', 'HOM_PARTICIPANT_UNAVAILABLE', 'HOM_IAM_UNAVAILABLE']
    for (const l of ['fr', 'en', 'es']) for (const c of codes) expect(i18next.exists(`erreur.${c}`, { lng: l }), `${l} ${c}`).toBe(true)
  })
})
