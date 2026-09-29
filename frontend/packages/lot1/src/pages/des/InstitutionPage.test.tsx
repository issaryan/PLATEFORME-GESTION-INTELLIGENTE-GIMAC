import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import InstitutionPage from './InstitutionPage'
import { rendrePage, sessionTest } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'

afterEach(cleanup)
const cleBrute = /\b(institution|piece|transition|adhesion|prerequis|crud|commun)\.[a-zA-Z]/
const CHARGE = ['PAR_INSTITUTION_READ', 'PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', 'PAR_ADHESION_INSTRUCT', 'PAR_TRANSITION_REQUEST']
const CHEF = [...CHARGE, 'PAR_TRANSITION_APPROVE']

async function monter(nom: string, o: { onglet?: string; permissions?: string[]; langue?: 'fr' | 'en' | 'es'; services?: Services; suffixe?: string } = {}) {
  const services = o.services ?? servicesSimules({ latenceMs: 0 })
  const id = (await services.participant.listerInstitutions({ page: 1, taille: 50 })).contenu.find((i) => i.nom === nom)!.id
  const chemin = `/des/institutions/${id}${o.onglet ? `?onglet=${o.onglet}` : ''}${o.suffixe ?? ''}`
  const r = rendrePage(<InstitutionPage />, { chemin, motif: '/des/institutions/:id', services, langue: o.langue, session: sessionTest(o.permissions ?? CHEF) })
  return { ...r, id, services }
}

describe('DES-06 Dossier numérique d’une institution', () => {
  it('nominal : fiche, statut, barre d’états et onglets du dossier', async () => {
    await monter('Fintech Kribi Pay')
    expect(await screen.findByRole('heading', { level: 1, name: 'Fintech Kribi Pay' })).toBeTruthy()
    for (const o of ["Dossier d'adhésion", "Conditions d'adhésion", 'Prérequis', 'Propositions de statut']) expect(screen.getByRole('tab', { name: o })).toBeTruthy()
    expect(screen.getByRole('button', { name: "Proposer l'adhésion" })).toBeTruthy()
  })
  it('introuvable : message d’erreur avec corrélation', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const services = { ...s, participant: { ...s.participant, dossier: async () => { throw new ApiError(404, 'PAR_INSTITUTION_NOT_FOUND', 'introuvable', 'RQ-404') } } }
    rendrePage(<InstitutionPage />, { chemin: '/des/institutions/x', motif: '/des/institutions/:id', services, session: sessionTest(CHEF) })
    expect(await screen.findByText('RQ-404')).toBeTruthy()
  })
  it('dossier d’adhésion : pièces et action « Valider » pour une pièce reçue', async () => {
    await monter('Microfinance du Chari', { onglet: 'pieces' })
    expect(await screen.findByText("Statuts de l'établissement")).toBeTruthy()
    expect(screen.getAllByText('Reçue').length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: 'Valider la pièce' })).toBeTruthy()
  })
  it('matrice des rôles : sans PAR_ADHESION_INSTRUCT, aucune action sur les pièces', async () => {
    await monter('Microfinance du Chari', { onglet: 'pieces', permissions: ['PAR_INSTITUTION_READ'] })
    expect(await screen.findByText("Statuts de l'établissement")).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Valider la pièce' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Déposer la pièce' })).toBeNull()
    expect(screen.queryByRole('button', { name: "Proposer l'adhésion" })).toBeNull()
  })
  it('prérequis : manquants signalés tant que le dossier est incomplet', async () => {
    await monter('Fintech Kribi Pay', { onglet: 'prerequis' })
    expect(await screen.findByText(/prérequis manquants avant de proposer/)).toBeTruthy()
    expect(screen.getAllByText('manquant').length).toBeGreaterThan(0)
  })
  it('prérequis réunis : le dossier complet est annoncé', async () => {
    await monter('Banque de Libreville', { onglet: 'prerequis' })
    expect(await screen.findByText("Tous les prérequis de l'adhésion sont réunis.")).toBeTruthy()
  })
  it('proposition puis approbation signée : l’institution devient adhérente', async () => {
    const { services, id } = await monter('Banque de Libreville', { onglet: 'transitions' })
    await services.participant.proposerTransition(id, 'ADHERENT', 'Dossier complet')
    cleanup()
    await monter('Banque de Libreville', { onglet: 'transitions', services })
    fireEvent.click(await screen.findByRole('button', { name: 'Approuver' }))
    const dialogue = await waitFor(() => { const d = screen.getAllByRole('dialog').find((x) => within(x).queryByRole('button', { name: 'Utiliser ma clé de sécurité' })); if (!d) throw new Error('signature absente'); return d })
    fireEvent.click(within(dialogue).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    await waitFor(async () => expect((await services.participant.dossier(id)).institution.statut).toBe('ADHERENT'), { timeout: 5000 })
  })
  it('matrice des rôles : le chargé ne voit pas l’approbation', async () => {
    const { services, id } = await monter('Banque de Libreville', { onglet: 'transitions', permissions: CHARGE })
    await services.participant.proposerTransition(id, 'ADHERENT', 'Dossier complet')
    cleanup()
    await monter('Banque de Libreville', { onglet: 'transitions', services, permissions: CHARGE })
    expect(await screen.findByText('Dossier complet')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Approuver' })).toBeNull()
  })
  it('conditions d’adhésion : affichées, puis formulaire pour un candidat', async () => {
    await monter('Banque de Libreville', { onglet: 'conditions' })
    expect(await screen.findByText('BQ-BDL-001')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: "Définir les conditions d'adhésion" }))
    expect(await screen.findByText("Conditions d'adhésion de Banque de Libreville")).toBeTruthy()
  })
  it('dépôt d’une pièce : formulaire avec zone de dépôt', async () => {
    await monter('Fintech Kribi Pay', { suffixe: '?onglet=pieces&piece=STATUTS' })
    expect(await screen.findByText("Déposer : Statuts de l'établissement")).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Déposer la pièce' })).toBeTruthy()
  })
  it('modification de la fiche : formulaire pré-rempli', async () => {
    await monter('Fintech Kribi Pay', { suffixe: '?formulaire=modifier' })
    expect(await screen.findByText('Modifier la fiche de Fintech Kribi Pay')).toBeTruthy()
  })
  it('soumissions : conditions d’adhésion, dépôt d’une pièce et modification de la fiche', async () => {
    const base = servicesSimules({ latenceMs: 0 })
    const appels: string[] = []
    const services: Services = {
      ...base,
      participant: {
        ...base.participant,
        definirConditions: async (id, c) => { appels.push(`conditions:${c.codeParticipant}:${c.mode}`); return base.participant.definirConditions(id, c) },
        deposerPiece: async (id, code, f) => { appels.push(`depot:${code}:${f.name}`); return base.participant.deposerPiece(id, code, f) },
        modifier: async (id, c) => { appels.push(`modifier:${c.nom}`); return base.participant.modifier(id, c) },
      },
    }
    const { container } = await monter('Fintech Kribi Pay', { services, suffixe: '?onglet=conditions&formulaire=conditions' })
    await screen.findByText("Conditions d'adhésion de Fintech Kribi Pay")
    fireEvent.change(container.querySelector('input[name="codeParticipant"]')!, { target: { value: 'fk-001' } })
    fireEvent.click(container.querySelector('input[type="radio"][value="INDIRECT"]')!)
    fireEvent.submit(container.querySelector('form')!)
    await waitFor(() => expect(appels).toContain('conditions:FK-001:INDIRECT'))
    cleanup()

    const r = await monter('Fintech Kribi Pay', { services, suffixe: '?onglet=pieces&piece=AGREMENT' })
    await screen.findByText("Déposer : Agrément ou autorisation d'exercer")
    fireEvent.change(r.container.querySelector('input[type="file"]')!, { target: { files: [new File(['x'], 'agrement.pdf', { type: 'application/pdf' })] } })
    fireEvent.click(screen.getByRole('button', { name: 'Déposer la pièce' }))
    await waitFor(() => expect(appels).toContain('depot:AGREMENT:agrement.pdf'))
    cleanup()

    const m = await monter('Fintech Kribi Pay', { services, suffixe: '?formulaire=modifier' })
    await screen.findByText('Modifier la fiche de Fintech Kribi Pay')
    fireEvent.change(m.container.querySelector('input[name="nom"]')!, { target: { value: 'Fintech Kribi Pay SA' } })
    fireEvent.submit(m.container.querySelector('form')!)
    await waitFor(() => expect(appels).toContain('modifier:Fintech Kribi Pay SA'))
  })
  it('navigation : onglets portés par l’adresse, action de modification, annulations', async () => {
    await monter('Fintech Kribi Pay')
    await screen.findByRole('heading', { level: 1, name: 'Fintech Kribi Pay' })
    fireEvent.click(screen.getByRole('tab', { name: 'Prérequis' }))
    expect(await screen.findByText(/prérequis manquants avant de proposer/)).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: "Conditions d'adhésion" }))
    fireEvent.click(await screen.findByRole('button', { name: "Définir les conditions d'adhésion" }))
    fireEvent.click(await screen.findByRole('button', { name: 'Annuler' }))
    expect(await screen.findByRole('button', { name: "Définir les conditions d'adhésion" })).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: "Dossier d'adhésion" }))
    fireEvent.click((await screen.findAllByRole('button', { name: 'Déposer la pièce' }))[0])
    fireEvent.click(await screen.findByRole('button', { name: 'Annuler' }))
    fireEvent.click(await screen.findByRole('tab', { name: 'Détail' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Modifier la fiche' }))
    expect(await screen.findByText('Modifier la fiche de Fintech Kribi Pay')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Annuler' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Fintech Kribi Pay' })).toBeTruthy()
  })
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    const { container } = await monter('Microfinance du Chari', { onglet: 'pieces', langue })
    await screen.findByRole('heading', { level: 1 })
    await waitFor(() => expect(container.textContent).not.toMatch(cleBrute))
  })
})
