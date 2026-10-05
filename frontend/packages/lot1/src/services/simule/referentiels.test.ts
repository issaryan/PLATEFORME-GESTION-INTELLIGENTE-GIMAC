import { describe, expect, it } from 'vitest'
import { ApiError } from '@gimac/socle'
import { servicesSimules } from './index'
import { GESTES_DEDIES } from './parcours'
import { actionDuGeste, calculerGraphe, codesDeclenches, controleDe, controlerGraphe, estDedie, versionCR, versionsInitiales } from './referentiels'
import { operationVierge, saisieDe } from '../../adaptateurs/modelisateur'
import type { ReferenceOperation } from '../../modele/homologation'

// Serveur simulé du modélisateur (D-77) : version 1 fidèle à l'annexe A (ConversionAnnexeA), graphe étendu,
// écritures du brouillon et codes d'erreur de MS-HOMOLOGATION.
const code = (e: unknown) => (e as ApiError).code
const op = (c: string, predecesseurs: string[] = [], o: Partial<ReferenceOperation> = {}): ReferenceOperation =>
  ({ ...operationVierge('TESTS'), code: c, libelle: c, responsable: 'SNA', validateur: 'CONDUCTEUR', predecesseurs, ...o })

describe('Référentiel simulé : version 1 de l\'annexe A', () => {
  it('carte : rangs 3 à 35 et branche C, gestes, jalons, déclencheurs, RACI, livrables, raccord 27', () => {
    const v = versionCR('CARTE_INTERBANCAIRE')
    const o = (c: string) => v.find((x) => x.code === c)!
    expect(v.length).toBe(43)
    expect(v[0]).toMatchObject({ code: '3', predecesseurs: [], etape: 'CADRAGE', segment: 'AMONT', responsable: 'DDRM', validateur: 'CONDUCTEUR', externes: ['PARTICIPANT'], informes: ['CHEF_DDRM'] })
    expect(o('6').geste).toBe('AFFECTER_RESSOURCES')
    expect(o('12')).toMatchObject({ geste: 'COORDONNEES_TECHNIQUES', valeurReference: expect.any(String) })
    expect(o('17')).toMatchObject({ geste: 'CONSTATER_COUVERTURE', jalon: 'APTE_CERTIFICATION' })
    expect(o('18')).toMatchObject({ geste: 'EVENEMENT_EXTERNE', declencheur: 'certification.pv-established.v1', validateur: 'CHEF_DES' })
    expect(o('19')).toMatchObject({ jalon: 'CERTIFIEE', pivot: true, livrables: [{ code: 'L1', deposant: 'CHEF_DES', typesPieces: ['application/pdf'] }] })
    expect(o('25').jalon).toBe('APPROUVEE_SECURITE')
    expect(o('26')).toMatchObject({ geste: 'DEPOSER_PREUVE', declencheur: 'certification.change-record-referenced.v1' })
    expect(o('31').jalon).toBe('EN_PRODUCTION')
    expect(o('32').geste).toBe('FICHE_SUIVI')
    expect(o('9').geste).toBe('DEPOSER_LIVRABLE')
    expect(o('15')).toMatchObject({ geste: 'DEPOSER_LIVRABLE', particularite: 'ENTREE_ESSAIS', plateforme: null })
    expect(o('C1').particularite).toBe('CEREMONIE_CLES')
    expect(o('27').predecesseurs).toEqual(['C10'])
    expect(versionCR('PAIEMENT_MOBILE').find((x) => x.code === '15')!.plateforme).toBe('SIMULATEUR')
    expect(versionCR('DELEGATAIRE').length).toBeGreaterThan(60)
    // Le simulateur du parcours lit ses gestes dédiés sur le référentiel : mêmes rangs qu'avant (D-77, §3).
    expect(GESTES_DEDIES).toEqual(['6', '18', '19', '20', '21', '22', '23', '24', '25', '32', '33'])
    expect([estDedie('EVENEMENT_EXTERNE'), estDedie('DEPOSER_PREUVE'), estDedie(null)]).toEqual([true, false, false])
    expect(['CONSTATER_COUVERTURE', 'DEPOSER_LIVRABLE', 'FICHE_SUIVI'].map((g) => actionDuGeste(g as never))).toEqual(['CONSTATER_COUVERTURE', 'DEPOSER_LIVRABLE', 'DEPOSER_PREUVE'])
  })
  it('graphe : couches de Kahn, dates au plus tôt, chemin critique, version conforme', () => {
    const { versions, operations } = versionsInitiales()
    const g = calculerGraphe(versions[0], operations['REF-CI-3'])
    expect(g.anomalies).toEqual([])
    const n = (c: string) => g.noeuds.find((x) => x.code === c)!
    expect([n('3').couche, n('4').couche, n('6').couche]).toEqual([0, 1, 1])
    expect([n('3').debutAuPlusTot, n('3').finAuPlusTot, n('4').debutAuPlusTot]).toEqual([0, 3, 3])
    expect(g.cheminCritique.codes[0]).toBe('3')
    expect(g.cheminCritique.codes.at(-1)).toBe('35')
    expect(g.cheminCritique.dureeJours).toBe(n('35').finAuPlusTot)
    expect(g.arcs.find((a) => a.de === '34' && a.vers === '35')!.critique).toBe(true)
    expect(n('C2').successeurs).toEqual(['C3', 'C4', 'C9'])
    const b = calculerGraphe(versions[1], operations['REF-CI-4'])
    expect(b.noeuds.find((x) => x.code === 'C11')!.anomalies).toEqual(['VALIDATEUR_ABSENT'])
    expect(b.manquants).toEqual([{ code: 'C11', attributs: ['validateur'] }])
  })
  it('anomalies : cycle, prédécesseur inconnu, segment, rôles, déclencheur, jalons, livrables, secret, sans entrée', () => {
    const r = controlerGraphe([
      op('A', [], { segment: 'AVAL' }), op('B', ['A', 'Z'], { segment: 'AMONT', validateur: 'SNA', geste: 'EVENEMENT_EXTERNE', jalon: 'CERTIFIEE' }),
      op('C', ['B'], { jalon: 'APTE_CERTIFICATION', responsable: null, validateur: null, secret: true, valeurReference: 'v',
        livrables: [{ code: 'L', libelle: 'l', nature: 'LIVRABLE', deposant: 'SNA', typesPieces: [], obligatoire: true }, { code: 'L', libelle: 'l', nature: 'PREUVE', deposant: 'SNA', typesPieces: [], obligatoire: true }] }),
      op('D', [], { jalon: 'CERTIFIEE' }), op('A', []),
    ])
    expect(r.anomalies.map((a) => a.type).sort()).toEqual(['CODE_DUPLIQUE', 'DECLENCHEUR_INCONNU', 'JALON_DESORDONNE', 'JALON_DUPLIQUE', 'LIVRABLE_DUPLIQUE', 'PREDECESSEUR_INCONNU',
      'RESPONSABLE_ABSENT', 'SECRET_AVEC_VALEUR', 'SEGMENT_INCOHERENT', 'VALIDATEUR_ABSENT', 'VALIDATEUR_EGAL_RESPONSABLE'].sort())
    const cycle = controleDe([op('X', ['Z']), op('Y', ['X']), op('Z', ['Y'])])
    expect(cycle.acyclique).toBe(false)
    expect(cycle.cycle).toEqual(['Y', 'Z', 'X'])
    expect(cycle.incoherences.map((i) => i.motif)).toEqual(['Aucune opération sans prédécesseur.'])
    const g = calculerGraphe(versionsInitiales().versions[0], [op('X', ['Y']), op('Y', ['X'])])
    expect(g.noeuds.every((n) => n.couche === null && n.debutAuPlusTot === null)).toBe(true)
    expect(g.cheminCritique).toEqual({ codes: [], dureeJours: 0 })
  })
})

describe('Référentiel simulé : écritures du brouillon', () => {
  const s = () => servicesSimules({ latenceMs: 0, utilisateur: { id: 'U-102', nom: 'B' } }).homologation
  it('création, enregistrement (position et particularité conservées), arcs, positions, suppression', async () => {
    const h = s()
    const n = await h.creerOperation('REF-CI-4', { ...saisieDe(op('NV-1', ['C11'])), code: 'NV-1' })
    expect(n).toMatchObject({ code: 'NV-1', successeurs: [], position: null })
    await h.placerOperations('REF-CI-4', [{ code: 'NV-1', x: 5, y: 6 }, { code: 'INCONNU', x: 0, y: 0 }])
    const { position: _p, particularite: _q, ...sans } = saisieDe(op('NV-1', ['C11'], { libelle: 'Modifiée' }))
    expect(await h.enregistrerOperation('REF-CI-4', 'NV-1', sans as never)).toMatchObject({ libelle: 'Modifiée', position: { x: 5, y: 6 }, particularite: null })
    expect((await h.arcReferentiel('REF-CI-4', { de: '35', vers: 'NV-1', present: true })).predecesseurs).toEqual(['C11', '35'])
    expect((await h.arcReferentiel('REF-CI-4', { de: '35', vers: 'NV-1', present: true })).predecesseurs).toEqual(['C11', '35'])
    expect((await h.arcReferentiel('REF-CI-4', { de: '35', vers: 'NV-1', present: false })).predecesseurs).toEqual(['C11'])
    expect((await h.operationsReferentiel('REF-CI-4')).find((x) => x.code === 'C11')!.successeurs).toEqual(['27', 'NV-1'])
    await h.supprimerOperation('REF-CI-4', 'C11')
    const ops = await h.operationsReferentiel('REF-CI-4')
    expect(ops.find((x) => x.code === 'NV-1')!.predecesseurs).toEqual([])
    expect(ops.find((x) => x.code === '27')!.predecesseurs).toEqual([])
  })
  it('refus : figée, code invalide ou dupliqué, secret avec valeur, auto-prédécesseur, cycle, introuvable, pilote', async () => {
    const h = s()
    const saisie = saisieDe(op('Q'))
    await expect(h.creerOperation('REF-CI-3', { ...saisie, code: 'Q' })).rejects.toSatisfy((e) => code(e) === 'HOM_REFERENTIAL_FROZEN')
    await expect(h.creerOperation('REF-CI-4', { ...saisie, code: 'q' })).rejects.toSatisfy((e) => code(e) === 'HOM_OPERATION_CODE_INVALID')
    await expect(h.creerOperation('REF-CI-4', { ...saisie, code: 'C1' })).rejects.toSatisfy((e) => code(e) === 'HOM_OPERATION_CODE_DUPLICATE')
    await expect(h.creerOperation('REF-CI-4', { ...saisie, code: 'Q', secret: true, valeurReference: 'x' })).rejects.toSatisfy((e) => code(e) === 'HOM_OPERATION_SECRET_VALUE')
    await expect(h.enregistrerOperation('REF-CI-4', 'C1', { ...saisie, predecesseurs: ['C1'] })).rejects.toSatisfy((e) => code(e) === 'HOM_OPERATION_SELF_PREDECESSOR')
    await expect(h.enregistrerOperation('REF-CI-4', 'ZZ', saisie)).rejects.toSatisfy((e) => code(e) === 'HOM_OPERATION_NOT_FOUND')
    await expect(h.arcReferentiel('REF-CI-4', { de: 'C1', vers: 'C1', present: true })).rejects.toSatisfy((e) => code(e) === 'HOM_OPERATION_SELF_PREDECESSOR')
    await expect(h.arcReferentiel('REF-CI-4', { de: '35', vers: '3', present: true })).rejects.toSatisfy((e) => (e as ApiError).status === 409 && code(e) === 'HOM_REFERENTIAL_CYCLE')
    await expect(h.supprimerOperation('REF-CI-4', 'ZZ')).rejects.toSatisfy((e) => (e as ApiError).status === 404)
    await expect(h.designerPilote('REF-CI-4', 'U-999')).rejects.toSatisfy((e) => (e as ApiError).status === 422 && code(e) === 'HOM_PILOTE_NOT_ELIGIBLE')
    expect((await h.designerPilote('REF-CI-4', 'U-102')).piloteId).toBe('U-102')
    await expect(h.grapheReferentiel('REF-X')).rejects.toSatisfy((e) => code(e) === 'HOM_REFERENTIAL_NOT_FOUND')
    await expect(h.creerBrouillon({ typeProjet: 'DELEGATAIRE', libelle: 'x', piloteId: 'U-999' })).rejects.toSatisfy((e) => code(e) === 'HOM_PILOTE_NOT_ELIGIBLE')
  })
  it('brouillons : pilote par défaut, dérivation, publication (version précédente retirée, numéro suivant), cycle refusé', async () => {
    const h = s()
    const b = await h.creerBrouillon({ typeProjet: 'PAIEMENT_MOBILE', libelle: 'Mobile 3' })
    expect(b).toMatchObject({ piloteId: 'U-102', versionActiveId: 'REF-PM-2', etat: 'BROUILLON' })
    expect((await h.creerBrouillon({ typeProjet: 'DELEGATAIRE', libelle: 'D', piloteId: 'U-101' })).piloteId).toBe('U-101')
    const d = await h.deriverBrouillon('REF-PM-2')
    expect((await h.operationsReferentiel(d.id)).length).toBe((await h.operationsReferentiel('REF-PM-2')).length)
    await h.publierReferentiel(d.id)
    expect((await h.lireReferentiel(d.id))).toMatchObject({ etat: 'PUBLIEE', numero: 3, actionsPermises: ['DERIVER', 'CONTROLER'] })
    expect((await h.lireReferentiel('REF-PM-2')).etat).toBe('RETIREE')
    await expect(h.publierReferentiel(d.id)).rejects.toSatisfy((e) => code(e) === 'HOM_REFERENTIAL_FROZEN')
    const c = await h.deriverBrouillon('REF-CI-3')
    await h.creerOperation(c.id, { ...saisieDe(op('Y1', ['Y2'])), code: 'Y1' })
    await h.creerOperation(c.id, { ...saisieDe(op('Y2', ['Y1'])), code: 'Y2' })
    await expect(h.publierReferentiel(c.id)).rejects.toSatisfy((e) => code(e) === 'HOM_REFERENTIAL_GRAPH_INVALID')
    expect((await h.controlerReferentiel(c.id)).acyclique).toBe(false)
    expect((await h.catalogueReferentiels()).pilotes.length).toBe(2)
    expect((await h.listerReferentiels()).find((v) => v.id === 'REF-CI-3')!.nombreOperations).toBe(43)
  })
})

describe('Simulateur du parcours : déclencheurs lus sur le référentiel', () => {
  it('les opérations closes par un événement viennent de la version 1 du type', () => {
    expect(codesDeclenches('CARTE_INTERBANCAIRE', 'certification.security-approved.v1')).toEqual(['23', '24', '25'])
    expect(codesDeclenches('DELEGATAIRE', 'certification.attestations-archived.v1')).toEqual(['21', '22'])
    expect(codesDeclenches('PAIEMENT_MOBILE', 'certification.pv-signed.v1')).toEqual(['19'])
    expect(codesDeclenches('PAIEMENT_MOBILE', 'sujet.inconnu.v1')).toEqual([])
  })
})
