import { describe, expect, it } from 'vitest'
import { servicesSimules } from './index'
import type { ParticipantSimule } from './participant'

// Serveur de substitution de MS-PARTICIPANT : mêmes formes et mêmes refus que le service réel.
const svc = () => servicesSimules({ latenceMs: 0 }).participant
const idDe = async (s: ReturnType<typeof svc>, nom: string) => (await s.listerInstitutions({ page: 1, taille: 50 })).contenu.find((i) => i.nom === nom)!.id
const refus = (code: string) => expect.objectContaining({ code })

describe('participantSimule', () => {
  it('liste filtrée par statut, recherche, mes dossiers et paginée', async () => {
    const s = svc()
    expect((await s.listerInstitutions({ page: 1, taille: 50 })).total).toBe(9)
    expect((await s.listerInstitutions({ page: 1, taille: 50, statut: ['ADHERENT'] })).contenu.map((i) => i.nom)).toEqual(['Mobile Money Sanaga', 'Crédit rural du Woleu'])
    expect((await s.listerInstitutions({ page: 1, taille: 50, statut: ['EN_HOMOLOGATION'] })).contenu.map((i) => i.id)).toEqual(['INST-0042', 'INST-0088'])
    expect((await s.listerInstitutions({ page: 1, taille: 50, recherche: 'bdl' })).total).toBe(1)
    expect((await s.listerInstitutions({ page: 1, taille: 50, mesDossiers: true })).total).toBe(9)
    expect((await s.listerInstitutions({ page: 2, taille: 5 })).contenu).toHaveLength(4)
  })
  it('indicateurs de la DDRM', async () => {
    expect(await svc().indicateurs()).toEqual({ candidats: 3, piecesAExaminer: 1, propositionsEnAttente: 0, adherents: 2, enHomologation: 2, closSansSuite: 0 })
  })
  it('institutions engagées : historique des transitions, bascule automatique à l’ouverture d’une liaison', async () => {
    const s = svc()
    const d = await s.dossier('INST-0061')
    expect(d.institution.statut).toBe('SUSPENDU')
    expect(d.transitions.map((t) => `${t.de}>${t.vers}${t.automatique ? ' (auto)' : ''}`)).toEqual([
      'ACTIF>SUSPENDU', 'EN_HOMOLOGATION>ACTIF', 'ADHERENT>EN_HOMOLOGATION (auto)', 'CANDIDAT>ADHERENT',
    ])
    expect((await s.dossier('INST-0105')).transitions).toHaveLength(1)
  })
  it('pont : homologation.liaison-opened.v1 fait passer un adhérent en homologation, et lui seul', async () => {
    const s = servicesSimules({ latenceMs: 0 }).participant as ParticipantSimule
    expect(s.pont.institution('INST-0105')?.statut).toBe('ADHERENT')
    expect(s.pont.institution('inconnue')).toBeUndefined()
    s.pont.liaisonOuverte('INST-0105', 'LIA-2026-041')
    const d = await s.dossier('INST-0105')
    expect(d.institution.statut).toBe('EN_HOMOLOGATION')
    expect(d.transitions[0]).toMatchObject({ de: 'ADHERENT', vers: 'EN_HOMOLOGATION', etat: 'APPROUVEE', automatique: true, motif: 'Ouverture de la liaison LIA-2026-041' })
    s.pont.liaisonOuverte('INST-0105', 'LIA-2026-042')
    s.pont.liaisonOuverte('inconnue', 'LIA-2026-043')
    expect((await s.dossier('INST-0105')).transitions).toHaveLength(2)
  })
  it('prise de contact, doublon de code refusé, modification', async () => {
    const s = svc()
    const i = await s.enregistrer({ nom: 'Banque Z', pays: 'CM', categorie: 'BANQUE', codeIdentification: 'BZZZCMCX' })
    expect(i.statut).toBe('CANDIDAT')
    await expect(s.enregistrer({ nom: 'Autre', pays: 'CM', categorie: 'BANQUE', codeIdentification: 'BZZZCMCX' })).rejects.toEqual(refus('PAR_CODE_ALREADY_USED'))
    expect((await s.modifier(i.id, { nom: 'Banque Z2', pays: 'GA', categorie: 'BANQUE' })).nom).toBe('Banque Z2')
    await expect(s.dossier('inconnue')).rejects.toEqual(refus('PAR_INSTITUTION_NOT_FOUND'))
  })
  it('dossier d’adhésion : dépôt, validation, rejet et refus', async () => {
    const s = svc()
    const id = await idDe(s, 'Fintech Kribi Pay')
    const p = await s.deposerPiece(id, 'STATUTS', new File(['x'], 'statuts.pdf'))
    expect(p).toMatchObject({ etat: 'RECUE', nomFichier: 'statuts.pdf', institutionId: id })
    expect((await s.validerPiece(id, 'STATUTS')).etat).toBe('VALIDEE')
    await expect(s.deposerPiece(id, 'STATUTS', new File(['x'], 'b.pdf'))).rejects.toEqual(refus('PAR_PIECE_ALREADY_VALIDATED'))
    await expect(s.validerPiece(id, 'AGREMENT')).rejects.toEqual(refus('PAR_PIECE_NOT_RECEIVED'))
    await s.deposerPiece(id, 'AGREMENT', new File(['x'], 'a.pdf'))
    expect((await s.rejeterPiece(id, 'AGREMENT', 'illisible')).motifRejet).toBe('illisible')
    await expect(s.rejeterPiece(id, 'AGREMENT', 'x')).rejects.toEqual(refus('PAR_PIECE_NOT_RECEIVED'))
    await expect(s.deposerPiece(id, 'INCONNUE', new File(['x'], 'a.pdf'))).rejects.toEqual(refus('PAR_PIECE_NOT_FOUND'))
  })
  it('prérequis, conditions, proposition, approbation et rejet', async () => {
    const s = svc()
    const id = await idDe(s, 'Banque de Libreville')
    expect((await s.prerequis(id, 'ADHERENT')).every((p) => p.satisfait)).toBe(true)
    expect(await s.prerequis(id, 'SUSPENDU')).toEqual([])
    await s.definirConditions(id, { codeParticipant: 'BQ-BDL-002', mode: 'DIRECT' })
    await expect(s.proposerTransition(id, 'ACTIF', 'x')).rejects.toEqual(refus('PAR_TRANSITION_NOT_ALLOWED'))
    const t = await s.proposerTransition(id, 'ADHERENT', 'Dossier complet')
    await expect(s.proposerTransition(id, 'CLOS_SANS_SUITE', 'x')).rejects.toEqual(refus('PAR_TRANSITION_PENDING'))
    expect((await s.indicateurs()).propositionsEnAttente).toBe(1)
    expect((await s.approuverTransition(id, t.id, 'a')).etat).toBe('APPROUVEE')
    await expect(s.approuverTransition(id, t.id, 'a')).rejects.toEqual(refus('PAR_TRANSITION_NOT_PENDING'))
    expect((await s.dossier(id)).institution.statut).toBe('ADHERENT')
    await expect(s.definirConditions(id, { codeParticipant: 'X', mode: 'DIRECT' })).rejects.toEqual(refus('PAR_INSTITUTION_STATUS'))
    const c = await s.proposerTransition(id, 'CLOS_SANS_SUITE', 'Abandon')
    expect((await s.rejeterTransition(id, c.id, 'Relance')).etat).toBe('REJETEE')
    await expect(s.rejeterTransition(id, c.id, 'x')).rejects.toEqual(refus('PAR_TRANSITION_NOT_PENDING'))
    const c2 = await s.proposerTransition(id, 'CLOS_SANS_SUITE', 'Abandon')
    await s.approuverTransition(id, c2.id, 'a')
    expect((await s.dossier(id)).adhesion.motifCloture).toBe('Abandon')
  })
  it('proposition d’adhésion refusée sur un dossier incomplet', async () => {
    const s = svc()
    await expect(s.proposerTransition(await idDe(s, 'Fintech Kribi Pay'), 'ADHERENT', 'x')).rejects.toEqual(refus('PAR_PREREQUISITES_MISSING'))
  })
})
