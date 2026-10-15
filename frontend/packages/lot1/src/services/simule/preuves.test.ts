import { describe, expect, it } from 'vitest'
import type { OperationParcours } from '../../modele/parcours'
import { servicesSimules } from './index'
import { chronologieDemo, etatLivrable, livrablesAttendusDemo } from './preuves'

// Preuves de la démonstration (D-109), comme EtatsPreuves de MS-HOMOLOGATION : états, grain du service, dépôts par
// livrable (l'opération reste en cours tant qu'un livrable obligatoire manque), sans objet motivé, pièce, chronologie.
const op = (o: Partial<OperationParcours>): OperationParcours => ({
  id: 'L:7', liaisonId: 'L', rang: '7', etape: 'LANCEMENT', segment: 'AMONT', plateforme: null, grain: 'OPERATION', etat: 'A_FAIRE',
  raci: { R: null, V: null, C: [], I: [], E: [] }, echeance: null, sla: null, faiteLe: null, preuve: null, cle: false, secret: false, valeurReference: false,
  oubliSansSymptome: false, pivot: false, predecesseursNonFaits: [], transitionsPermises: [], actionsPermises: [], ...o,
})
const depot = { livrable: 'L1', documentRef: 'D1', nom: 'cr.pdf', empreinte: 'e', deposeLe: '2026-10-10', deposePar: 'Agent' }

describe('états des livrables de la démonstration', () => {
  it('attendu, déposé, validé, refusé, sans objet, reprise sans pièce, non exigé', () => {
    expect(etatLivrable(op({}), null)).toBe('ATTENDUE')
    expect(etatLivrable(op({ etat: 'EN_VALIDATION' }), depot)).toBe('DEPOSEE')
    expect(etatLivrable(op({ etat: 'FAITE' }), depot)).toBe('VALIDEE')
    expect(etatLivrable(op({ renvois: [{ motif: 'm', parId: 'v', parNom: 'V', le: '2026-10-11T08:00:00Z' }] }), depot)).toBe('REFUSEE')
    expect(etatLivrable(op({}), { ...depot, sansObjet: true, motif: 'x' })).toBe('SANS_OBJET')
    expect(etatLivrable(op({ etat: 'FAITE', reprise: true }), null)).toBe('REPRISE_SANS_PIECE')
    expect(etatLivrable(op({ etat: 'FAITE' }), null)).toBe('NON_EXIGEE')
  })
  it('livrables lus dans les données D-109 ; grain du service : une ligne par service ; repli sur la description', () => {
    const seize = livrablesAttendusDemo(op({ id: 'L:16', rang: '16', etat: 'EN_COURS' }), 'CARTE_INTERBANCAIRE', ['RETRAIT', 'TRANSFERT'], [{ ...depot, service: 'RETRAIT' }], true)
    expect(seize[0].grain).toBe('SERVICE')
    expect(seize[0].services?.map((s) => s.etat)).toEqual(['DEPOSEE', 'ATTENDUE'])
    expect(seize[0].etat).toBe('ATTENDUE')
    const tous = livrablesAttendusDemo(op({ id: 'L:16', rang: '16', etat: 'FAITE' }), 'CARTE_INTERBANCAIRE', ['RETRAIT'], [{ ...depot, service: 'RETRAIT' }], false)
    expect(tous[0].etat).toBe('VALIDEE')
    const sept = livrablesAttendusDemo(op({ preuve: { documentRef: 'P', nom: 'p.pdf', empreinte: 'e', deposeLe: '2026-10-10', deposePar: 'A' } }), 'CARTE_INTERBANCAIRE', [], [], true)
    expect(sept.map((l) => [l.code, l.obligatoire])).toEqual([['L1', true], ['L2', false]])
    expect(sept[0].depot?.documentRef).toBe('P')
    const libre = livrablesAttendusDemo(op({ rang: 'X1', description: 'Pièce libre' }), 'CARTE_INTERBANCAIRE', [], [], false)
    expect(libre).toMatchObject([{ code: 'L1', libelle: 'Pièce libre', grain: 'OPERATION' }])
  })
  it('chronologie : dépôts, renvois, signatures, clôture, reprise, blocages, du plus récent au plus ancien', () => {
    const ops = [
      op({ id: 'L:3', rang: '3', etat: 'FAITE', faiteLe: '2026-10-01', reprise: true }),
      op({ id: 'L:7', rang: '7', etat: 'FAITE', faiteLe: '2026-10-12', renvois: [{ motif: 'm', parId: 'v', parNom: 'V', le: '2026-10-11T08:00:00Z' }],
        signatures: [{ code: 'VISA', libelle: 'Visa', ordre: 1, signataire: 'CHEF_DES', porte: ['L1'], etat: 'SIGNEE', suivante: false, signeLe: '2026-10-12T09:00:00Z', signeParNom: 'Chef' }] }),
      op({ id: 'L:8', rang: '8', preuve: { documentRef: 'P8', nom: 'p.pdf', empreinte: 'e', deposeLe: '2026-10-13', deposePar: 'A' } }),
    ]
    const l = chronologieDemo(ops, [{ id: 'B', liaisonId: 'L', motif: 'Pièce', depuis: '2026-10-02', prochaineAction: 'x', responsable: null as never, echeanceLevee: '2026-10-05', leveLe: '2026-10-04', etape: 'CADRAGE', transitionsPermises: [], actionsPermises: [] }],
      new Map([['L:7', [depot, { livrable: 'L2', deposeLe: '2026-10-10', deposePar: 'A', sansObjet: true, motif: 'Aucune feuille' }]]]))
    expect(l.map((e) => e.type)).toEqual(['DEPOT', 'SIGNATURE', 'FAITE', 'RENVOI', 'DEPOT', 'SANS_OBJET', 'LEVEE', 'BLOCAGE', 'REPRISE'])
  })
})

describe('service simulé : dépôts par livrable, sans objet, pièce, chronologie', () => {
  it('rang 10 : deux livrables obligatoires, l’opération reste en cours après le premier', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-S', nom: 'Ingénieur SNA', profils: ['ROLE_SNA'] } })
    const lire = async () => (await s.parcours.operations('LIA-2026-036')).find((o) => o.rang === '10')!
    const o10 = await lire()
    expect(o10.livrablesAttendus?.map((l) => [l.code, l.obligatoire])).toEqual([['L1', true], ['L2', true]])
    await s.parcours.deposerLivrable(o10.id, new File(['x'], 'cr-api.pdf'), 'relu', 'L1')
    let v = await lire()
    expect(v.etat).toBe('EN_COURS')
    expect(v.livrablesAttendus?.map((l) => l.etat)).toEqual(['DEPOSEE', 'ATTENDUE'])
    const piece = await s.parcours.telechargerPiece(o10.id, v.livrablesAttendus![0].depot!.documentRef!)
    expect(piece.empreinte).toBe(v.livrablesAttendus![0].depot!.empreinte)
    await expect(s.parcours.telechargerPiece(o10.id, 'INCONNUE')).rejects.toMatchObject({ code: 'HOM_PIECE_INCONNUE' })
    await s.parcours.deposerLivrable(o10.id, new File(['x'], 'perimetre-technique.pdf'), '', 'L2')
    v = await lire()
    expect(v.etat).toBe('FAITE')
    expect(v.livrablesAttendus?.map((l) => l.etat)).toEqual(['VALIDEE', 'VALIDEE'])
    const chrono = await s.parcours.chronologie('LIA-2026-036')
    expect(chrono.filter((e) => e.rang === '10').map((e) => e.type)).toEqual(expect.arrayContaining(['DEPOT', 'FAITE']))
  })
  it('livrable sans objet motivé : motif exigé, livrable connu, l’opération passe en validation', async () => {
    const s = servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-D', nom: 'Chargé DDRM', profils: ['ROLE_CHARGE_DDRM'] } })
    const o7 = (await s.parcours.operations('LIA-2026-041')).find((o) => o.rang === '7')!
    await expect(s.parcours.declarerSansObjet(o7.id, 'L1', ' x ')).rejects.toMatchObject({ code: 'HOM_MOTIF_REQUIS' })
    await expect(s.parcours.declarerSansObjet(o7.id, 'L9', 'Sans objet ici')).rejects.toMatchObject({ code: 'HOM_LIVRABLE_INCONNU' })
    await s.parcours.declarerSansObjet(o7.id, 'L1', 'Réunion tenue hors plateforme')
    const v = (await s.parcours.operations('LIA-2026-041')).find((o) => o.rang === '7')!
    expect(v.etat).toBe('EN_VALIDATION')
    expect(v.livrablesAttendus?.[0]).toMatchObject({ etat: 'SANS_OBJET' })
    const faite = (await s.parcours.operations('LIA-2026-041')).find((o) => o.etat === 'FAITE')!
    await expect(s.parcours.declarerSansObjet(faite.id, 'L1', 'Trop tard')).rejects.toMatchObject({ code: 'HOM_OPERATION_NOT_OPEN' })
  })
})
