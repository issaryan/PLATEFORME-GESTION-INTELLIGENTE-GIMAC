import { afterEach, describe, expect, it } from 'vitest'
import { servicesSimules } from './index'
import type { ParticipantSimule } from './participant'

// Serveur de substitution de MS-PARTICIPANT : mêmes formes et mêmes refus que le service réel.
const svc = () => servicesSimules({ latenceMs: 0 }).participant
const idDe = async (s: ReturnType<typeof svc>, nom: string) => (await s.listerInstitutions({ page: 1, taille: 50 })).contenu.find((i) => i.nom === nom)!.id
const refus = (code: string) => expect.objectContaining({ code })
const charge = { id: 'sim-charge', nom: 'Fatimé Abakar Hissein', profils: ['ROLE_CHARGE_DDRM'] }
const chef = { id: 'U-020', nom: 'Bertrand Nguema Obiang', profils: ['ROLE_CHEF_DDRM'] }
afterEach(() => Object.keys(localStorage).filter((k) => k.startsWith('gm.demo')).forEach((k) => localStorage.removeItem(k)))

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
  it('D-104 : filiales d’un groupe, filtre par groupe sans égard à la casse, groupes déclarés, saisie du groupe', async () => {
    const s = svc()
    expect(await s.groupes()).toEqual(['Groupe Banque BD'])
    expect((await s.listerInstitutions({ page: 1, taille: 50, groupe: ' groupe  banque bd ' })).contenu.map((i) => i.id)).toEqual(['INST-0017', 'INST-0061'])
    expect((await s.listerInstitutions({ page: 1, taille: 50, recherche: 'banque bd' })).total).toBe(2)
    const f = await s.enregistrer({ nom: 'Filiale Tchad', pays: 'TD', categorie: 'BANQUE', groupe: '  Groupe  Banque BD ' })
    expect(f.groupe).toBe('Groupe Banque BD')
    await s.modifier(f.id, { nom: 'Filiale Tchad', pays: 'TD', categorie: 'BANQUE', groupe: 'Groupe Sahel' })
    expect(await s.groupes()).toEqual(['Groupe Banque BD', 'Groupe Sahel'])
  })
  it('indicateurs de la DDRM', async () => {
    const { fluxParMois, ...compteurs } = await svc().indicateurs()
    expect(compteurs).toEqual({ candidats: 3, piecesAExaminer: 1, propositionsEnAttente: 0, adherents: 2, enHomologation: 2, closSansSuite: 0 })
    // Flux des 12 derniers mois : denses, du plus ancien au mois courant.
    expect(fluxParMois).toHaveLength(12)
    const d = new Date()
    expect(fluxParMois?.[11].mois).toBe(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
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
    // Maker-checker (DDR-01) : le chargé propose, le chef tranche, sur un même registre (démonstration continue).
    localStorage.setItem('gm.demoContinue', '1')
    const s = servicesSimules({ latenceMs: 0, utilisateur: charge }).participant
    const id = await idDe(s, 'Banque de Libreville')
    expect((await s.prerequis(id, 'ADHERENT')).every((p) => p.satisfait)).toBe(true)
    expect(await s.prerequis(id, 'SUSPENDU')).toEqual([])
    await s.definirConditions(id, { codeParticipant: 'BQ-BDL-002', mode: 'DIRECT', compteReglement: 'BEAC-GA-002' })
    await expect(s.proposerTransition(id, 'ACTIF', 'x')).rejects.toEqual(refus('PAR_TRANSITION_NOT_ALLOWED'))
    const t = await s.proposerTransition(id, 'ADHERENT', 'Dossier complet')
    await expect(s.proposerTransition(id, 'CLOS_SANS_SUITE', 'x')).rejects.toEqual(refus('PAR_TRANSITION_PENDING'))
    expect((await s.indicateurs()).propositionsEnAttente).toBe(1)
    const c = servicesSimules({ latenceMs: 0, utilisateur: chef }).participant
    expect((await c.approuverTransition(id, t.id, 'a')).etat).toBe('APPROUVEE')
    await expect(c.approuverTransition(id, t.id, 'a')).rejects.toEqual(refus('PAR_TRANSITION_NOT_PENDING'))
    const d = await c.dossier(id)
    expect(d.institution.statut).toBe('ADHERENT')
    // DDR-29 : conformité documentaire CONFORME au prononcé, comme le service.
    expect(d.institution.conformite).toBe('CONFORME')
    await expect(c.definirConditions(id, { codeParticipant: 'X', mode: 'DIRECT', compteReglement: 'R' })).rejects.toEqual(refus('PAR_INSTITUTION_STATUS'))
    const p = servicesSimules({ latenceMs: 0, utilisateur: charge }).participant
    const clo = await p.proposerTransition(id, 'CLOS_SANS_SUITE', 'Abandon')
    expect((await servicesSimules({ latenceMs: 0, utilisateur: chef }).participant.rejeterTransition(id, clo.id, 'Relance')).etat).toBe('REJETEE')
    await expect(c.rejeterTransition(id, clo.id, 'x')).rejects.toEqual(refus('PAR_TRANSITION_NOT_PENDING'))
    const c2 = await servicesSimules({ latenceMs: 0, utilisateur: charge }).participant.proposerTransition(id, 'CLOS_SANS_SUITE', 'Abandon')
    const chef2 = servicesSimules({ latenceMs: 0, utilisateur: chef }).participant
    await chef2.approuverTransition(id, c2.id, 'a')
    expect((await chef2.dossier(id)).adhesion.motifCloture).toBe('Abandon')
  })
  it('DDR-01 : maker-checker, le proposant ne peut ni prononcer ni refuser sa propre proposition', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: chef }).participant
    const id = await idDe(s, 'Banque de Libreville')
    const t = await s.proposerTransition(id, 'ADHERENT', 'Dossier complet')
    expect(t.demandeurId).toBe(chef.id)
    await expect(s.approuverTransition(id, t.id, 'a')).rejects.toEqual(expect.objectContaining({ code: 'PAR_SEPARATION_OF_DUTIES', status: 403 }))
    await expect(s.rejeterTransition(id, t.id, 'motif')).rejects.toEqual(refus('PAR_SEPARATION_OF_DUTIES'))
    expect((await s.dossier(id)).institution.statut).toBe('CANDIDAT')
  })
  it('DDR-02 : en démonstration continue, un identifiant n’est jamais réattribué après un rechargement', async () => {
    localStorage.setItem('gm.demoContinue', '1')
    const a = await servicesSimules({ latenceMs: 0, utilisateur: charge }).participant.enregistrer({ nom: 'Banque Atlantique du Littoral', pays: 'CM', categorie: 'BANQUE' })
    // Rechargement (nouvelle instance, compteur de module repris) puis seconde création.
    const b = await servicesSimules({ latenceMs: 0, utilisateur: chef }).participant.enregistrer({ nom: 'Trésor public centrafricain', pays: 'CF', categorie: 'TRESOR_PUBLIC' })
    expect(b.id).not.toBe(a.id)
    const s = servicesSimules({ latenceMs: 0, utilisateur: charge }).participant
    expect((await s.dossier(a.id)).institution.nom).toBe('Banque Atlantique du Littoral')
    expect((await s.dossier(b.id)).institution.nom).toBe('Trésor public centrafricain')
    const ids = (await s.listerInstitutions({ page: 1, taille: 100 })).contenu.map((i) => i.id)
    expect(new Set(ids).size).toBe(ids.length)
  })
  it('DDR-03 : conditions d’adhésion contrôlées champ par champ (code, mode, RG-11, RG-12)', async () => {
    const s = svc()
    const id = await idDe(s, 'Fintech Kribi Pay')
    const champs = async (c: Parameters<typeof s.definirConditions>[1]) =>
      s.definirConditions(id, c).then(() => null, (e: { code: string; details?: { champs?: Record<string, string> } }) => [e.code, Object.keys(e.details?.champs ?? {}).sort()])
    expect(await champs({ codeParticipant: '', mode: null as never })).toEqual(['PAR_CONDITIONS_INVALID', ['codeParticipant', 'mode']])
    expect(await champs({ codeParticipant: 'BAL@01 !', mode: 'INDIRECT' })).toEqual(['PAR_CONDITIONS_INVALID', ['banqueMarraineId', 'codeParticipant', 'compteCantonnement']])
    expect(await champs({ codeParticipant: 'BQ-BAL-074', mode: 'DIRECT' })).toEqual(['PAR_CONDITIONS_INVALID', ['compteReglement']])
    expect(await champs({ codeParticipant: 'BQ-BAL-074', mode: 'DIRECT', compteReglement: 'BEAC-CM-074' })).toBeNull()
    expect((await s.dossier(id)).adhesion).toMatchObject({ codeParticipant: 'BQ-BAL-074', mode: 'DIRECT', compteReglement: 'BEAC-CM-074' })
  })
  it('DDR-04 : la modification de la fiche reprend tous les champs, contrôle l’unicité des codes', async () => {
    const s = svc()
    const i = await s.enregistrer({ nom: 'Banque Atlantique du Littoral', pays: 'CM', categorie: 'BANQUE', codeIdentification: 'BALTCMCXXXX', codeBanqueBeac: '10005AB' })
    const m = await s.modifier(i.id, {
      nom: 'Banque Atlantique du Littoral', sigle: 'BAL', pays: 'CM', categorie: 'BANQUE', codeIdentification: 'BALTCMCX', codeBanqueBeac: '10006', originePriseDeContact: 'Salon', datePriseDeContact: '2026-09-15',
    })
    expect(m).toMatchObject({ sigle: 'BAL', codeIdentification: 'BALTCMCX', codeBanqueBeac: '10006', originePriseDeContact: 'Salon', datePriseDeContact: '2026-09-15' })
    expect((await s.dossier(i.id)).institution.codeBanqueBeac).toBe('10006')
    await expect(s.modifier(i.id, { nom: 'x', pays: 'GA', categorie: 'BANQUE', codeIdentification: 'BDLIGALX' })).rejects.toEqual(refus('PAR_CODE_ALREADY_USED'))
  })
  it('proposition d’adhésion refusée sur un dossier incomplet', async () => {
    const s = svc()
    await expect(s.proposerTransition(await idDe(s, 'Fintech Kribi Pay'), 'ADHERENT', 'x')).rejects.toEqual(refus('PAR_PREREQUISITES_MISSING'))
  })
})
