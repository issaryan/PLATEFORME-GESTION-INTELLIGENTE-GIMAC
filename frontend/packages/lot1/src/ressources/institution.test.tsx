import { describe, expect, it, vi } from 'vitest'
import { render } from '@testing-library/react'
import type { ActionContext } from '@gimac/socle'
import type { Institution, PieceAdhesion, TransitionStatut } from '../modele/participant'
import type { Services } from '../services/ports'
import { creerInstitutionDescriptor, creerPieceDescriptor, creerTransitionDescriptor } from './institution'

// Descripteurs de la DDRM : disponibilité des actions (reflet de l'état, jamais une règle métier),
// appels du port et rendus des champs.
const t = (k: string) => k
const inst = (s: Partial<Institution> = {}): Institution => ({
  id: 'i1', nom: 'Banque B', sigle: null, pays: 'CM', categorie: 'BANQUE', statut: 'CANDIDAT', codeParticipant: null, codeIdentification: null,
  codeBanqueBeac: null, modeAdhesion: null, datePriseDeContact: null, originePriseDeContact: null, projetEnvisage: null, chargeDdrm: null,
  conformite: 'A_REGULARISER', creeLe: '2026-09-29T08:00:00Z', ...s,
})
const piece = (s: Partial<PieceAdhesion> = {}): PieceAdhesion => ({
  id: 'p1', code: 'STATUTS', libelle: 'Statuts', obligatoire: true, etat: 'RECUE', documentId: 'd1', documentSha256: 'a'.repeat(64), nomFichier: 's.pdf',
  deposeeLe: null, examineeLe: null, motifRejet: null, institutionId: 'i1', ...s,
})
const transition = (s: Partial<TransitionStatut> = {}): TransitionStatut => ({
  id: 't1', de: 'CANDIDAT', vers: 'ADHERENT', motif: 'm', etat: 'PROPOSEE', demandeurId: 'u', demandeeLe: '', approbationsRequises: 1, approbateurId: null,
  secondApprobateurId: null, decideeLe: null, motifRejet: null, pieceDecisionId: null, institutionId: 'i1', ...s,
})
function contexte<T>(row: T, extra: Partial<ActionContext<T, Services>> = {}) {
  const participant = {
    proposerTransition: vi.fn(async () => transition()), validerPiece: vi.fn(async () => piece()), rejeterPiece: vi.fn(async () => piece()),
    approuverTransition: vi.fn(async () => transition()), rejeterTransition: vi.fn(async () => transition()),
  }
  const ctx = {
    row, selection: [row], services: { participant } as unknown as Services, navigate: vi.fn(), openForm: vi.fn(), openTab: vi.fn(), invalidate: vi.fn(), ...extra,
  } as ActionContext<T, Services>
  return { ctx, participant }
}
const action = <T,>(d: { actions: { key: string }[] }, cle: string) => d.actions.find((a) => a.key === cle) as unknown as {
  isAllowed?: (r: T) => boolean; run: (c: ActionContext<T, Services>) => unknown; consequences?: (r: T) => unknown; sign?: { empreinte: (r: T) => string }
}

describe('descripteur institution', () => {
  const d = creerInstitutionDescriptor({ t })
  it('actions permises selon le statut', () => {
    expect(action<Institution>(d, 'proposerAdhesion').isAllowed!(inst())).toBe(true)
    expect(action<Institution>(d, 'proposerAdhesion').isAllowed!(inst({ statut: 'ADHERENT' }))).toBe(false)
    expect(action<Institution>(d, 'proposerCloture').isAllowed!(inst({ statut: 'ADHERENT' }))).toBe(true)
    expect(action<Institution>(d, 'proposerCloture').isAllowed!(inst({ statut: 'ACTIF' }))).toBe(false)
    expect(action<Institution>(d, 'modifier').isAllowed!(inst({ statut: 'CLOS_SANS_SUITE' }))).toBe(false)
    expect(action<Institution>(d, 'modifier').isAllowed!(inst({ statut: 'ACTIF' }))).toBe(true)
  })
  it('actions : propositions envoyées au service avec le motif, formulaires ouverts', async () => {
    const { ctx, participant } = contexte(inst(), { motif: 'Dossier complet' })
    await action<Institution>(d, 'proposerAdhesion').run(ctx)
    await action<Institution>(d, 'proposerCloture').run(ctx)
    expect(participant.proposerTransition).toHaveBeenCalledWith('i1', 'ADHERENT', 'Dossier complet')
    expect(participant.proposerTransition).toHaveBeenCalledWith('i1', 'CLOS_SANS_SUITE', 'Dossier complet')
    action<Institution>(d, 'enregistrer').run(ctx)
    action<Institution>(d, 'modifier').run(ctx)
    expect(ctx.openForm).toHaveBeenCalledWith('create')
    expect(ctx.openForm).toHaveBeenCalledWith('edit')
    const sansMotif = contexte(inst())
    await action<Institution>(d, 'proposerAdhesion').run(sansMotif.ctx)
    expect(sansMotif.participant.proposerTransition).toHaveBeenCalledWith('i1', 'ADHERENT', '')
  })
  it('rendus : lien, libellés, chargé, conformité et conséquences', () => {
    const champ = (n: string) => d.fields.find((f) => f.name === n && f.render)!.render!
    const { container } = render(
      <div>
        {champ('nom')(null, inst())}
        {champ('categorie')(null, inst())}
        {champ('pays')(null, inst())}
        {champ('chargeDdrm')(null, inst())}
        {champ('chargeDdrm')(null, inst({ chargeDdrm: { id: 'c', nom: 'Chargé' } }))}
        {champ('conformite')(null, inst())}
        {champ('conformite')(null, inst({ conformite: 'INCONNU' }))}
        {action<Institution>(d, 'proposerAdhesion').consequences!(inst()) as never}
        {action<Institution>(d, 'proposerCloture').consequences!(inst()) as never}
      </div>,
    )
    expect(container.querySelector('a')?.getAttribute('href')).toBe('/des/institutions/i1')
    expect(container.textContent).toContain('institution.categorie.BANQUE')
    expect(container.textContent).toContain('Chargé')
    expect(container.textContent).toContain('institution.chargeDdrm.aucun')
    expect(container.textContent).toContain('institution.adhesion.consequence')
  })
})

describe('descripteur pièce', () => {
  const d = creerPieceDescriptor({ t })
  it('valider et rejeter une pièce reçue, déposer tant qu’elle n’est pas validée', async () => {
    expect(action<PieceAdhesion>(d, 'valider').isAllowed!(piece())).toBe(true)
    expect(action<PieceAdhesion>(d, 'valider').isAllowed!(piece({ etat: 'ATTENDUE' }))).toBe(false)
    expect(action<PieceAdhesion>(d, 'rejeter').isAllowed!(piece({ etat: 'VALIDEE' }))).toBe(false)
    expect(action<PieceAdhesion>(d, 'deposer').isAllowed!(piece({ etat: 'REJETEE' }))).toBe(true)
    expect(action<PieceAdhesion>(d, 'deposer').isAllowed!(piece({ etat: 'VALIDEE' }))).toBe(false)
    const { ctx, participant } = contexte(piece(), { motif: 'illisible' })
    await action<PieceAdhesion>(d, 'valider').run(ctx)
    await action<PieceAdhesion>(d, 'rejeter').run(ctx)
    action<PieceAdhesion>(d, 'deposer').run(ctx)
    expect(participant.validerPiece).toHaveBeenCalledWith('i1', 'STATUTS')
    expect(participant.rejeterPiece).toHaveBeenCalledWith('i1', 'STATUTS', 'illisible')
    expect(ctx.openForm).toHaveBeenCalledWith('edit')
    const orpheline = contexte(piece({ institutionId: undefined }))
    await action<PieceAdhesion>(d, 'valider').run(orpheline.ctx)
    await action<PieceAdhesion>(d, 'rejeter').run(orpheline.ctx)
    expect(orpheline.participant.validerPiece).toHaveBeenCalledWith('', 'STATUTS')
    expect(orpheline.participant.rejeterPiece).toHaveBeenCalledWith('', 'STATUTS', '')
  })
  it('rendus : exigence et empreinte', () => {
    const champ = (n: string) => d.fields.find((f) => f.name === n)!.render!
    const { container } = render(
      <div>
        {champ('obligatoire')(null, piece())}
        {champ('obligatoire')(null, piece({ obligatoire: false }))}
        {champ('documentSha256')(null, piece())}
        {champ('documentSha256')(null, piece({ documentSha256: null }))}
      </div>,
    )
    expect(container.textContent).toContain('piece.obligatoire.oui')
    expect(container.textContent).toContain('piece.obligatoire.non')
  })
})

describe('descripteur transition', () => {
  const d = creerTransitionDescriptor({ t })
  it('approbation signée et rejet d’une proposition en attente seulement', async () => {
    expect(action<TransitionStatut>(d, 'approuver').isAllowed!(transition())).toBe(true)
    expect(action<TransitionStatut>(d, 'approuver').isAllowed!(transition({ etat: 'APPROUVEE' }))).toBe(false)
    expect(action<TransitionStatut>(d, 'rejeter').isAllowed!(transition({ etat: 'REJETEE' }))).toBe(false)
    expect(action<TransitionStatut>(d, 'approuver').sign!.empreinte(transition())).toBe('t1')
    const { ctx, participant } = contexte(transition(), { assertion: 'a', motif: 'non' })
    await action<TransitionStatut>(d, 'approuver').run(ctx)
    await action<TransitionStatut>(d, 'rejeter').run(ctx)
    expect(participant.approuverTransition).toHaveBeenCalledWith('i1', 't1', 'a')
    expect(participant.rejeterTransition).toHaveBeenCalledWith('i1', 't1', 'non')
    const nu = contexte(transition({ institutionId: undefined }))
    await action<TransitionStatut>(d, 'approuver').run(nu.ctx)
    await action<TransitionStatut>(d, 'rejeter').run(nu.ctx)
    expect(nu.participant.approuverTransition).toHaveBeenCalledWith('', 't1', '')
    expect(nu.participant.rejeterTransition).toHaveBeenCalledWith('', 't1', '')
  })
  it('rendus des états d’arrivée et de départ', () => {
    const champ = (n: string) => d.fields.find((f) => f.name === n)!.render!
    const { container } = render(<div>{champ('de')(null, transition())}{champ('vers')(null, transition({ vers: 'INCONNU' as never }))}{action<TransitionStatut>(d, 'approuver').consequences!(transition()) as never}</div>)
    expect(container.textContent).toContain('institution.statut.CANDIDAT')
    expect(container.textContent).toContain('transition.approbation.consequence')
  })
})
