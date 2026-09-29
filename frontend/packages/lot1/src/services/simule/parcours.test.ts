import { describe, expect, it } from 'vitest'
import type { EtapeCode, Fonction, OperationParcours } from '../../modele/parcours'
import { servicesSimules } from './index'
import { PERSONNES } from './parcours'

// Serveur de substitution du parcours (étapes 3 à 10, D-56 à D-59) : progression des opérations, règle des six
// éléments d'une étape, qualités R/C/V/I/E, preuves sans secret, blocages datés, tableaux de bord et pilotage.
type S = ReturnType<typeof servicesSimules>
const svc = () => servicesSimules({ latenceMs: 0 })
const refus = (code: string) => expect.objectContaining({ code })
const fichier = (nom = 'preuve.pdf') => new File(['contenu'], nom, { type: 'application/pdf' })
const op = async (s: S, liaison: string, rang: string) => (await s.parcours.operations(liaison)).find((o) => o.rang === rang)!
const etat = async (s: S, liaison: string, rang: string) => (await op(s, liaison, rang)).etat
const etape = async (s: S, liaison: string, code: EtapeCode) => (await s.parcours.dossier(liaison)).etapes.find((e) => e.code === code)!
const ids = (l: { id: string }[]) => l.map((x) => x.id)
const compteurs = async (s: S, f: Fonction[]) => Object.fromEntries((await s.parcours.tableauActeur(f)).compteurs.map((c) => [c.cle, c.valeur]))

/** Clôt une opération ouverte comme le ferait son responsable : livrable, ou preuve (code de contrôle, valeur relevée) puis validation. */
async function clore(s: S, o: OperationParcours) {
  if (o.actionsPermises.includes('DEPOSER_LIVRABLE')) return s.parcours.deposerLivrable(o.id, fichier(), 'Livrable remis')
  await s.parcours.deposerPreuve(o.id, fichier(), { codeControle: o.cle ? 'A1B2C3' : undefined, valeurReference: o.valeurReference ? `REF-${o.rang}` : undefined })
  await s.parcours.validerOperation(o.id, 'assertion')
}

describe('parcours simulé : liste, intervenants, ouverture et désignation (étape 3)', () => {
  it('liste filtrée (recherche, statut, type, étape, bloquées, mes projets) et paginée', async () => {
    const s = svc()
    const l = (f: Record<string, unknown>) => s.parcours.listerLiaisons({ page: 1, taille: 50, ...f })
    expect((await l({})).total).toBe(10)
    expect(ids((await l({ recherche: 'banque b' })).contenu)).toEqual(['LIA-2026-022', 'LIA-2026-044'])
    expect(ids((await l({ recherche: 'lia-2025' })).contenu)).toEqual(['LIA-2025-088'])
    expect(ids((await l({ statut: 'EN_TEST' })).contenu)).toEqual(['LIA-2026-022', 'LIA-2026-027'])
    expect(ids((await l({ typeProjet: 'DELEGATAIRE' })).contenu)).toEqual(['LIA-2026-019', 'LIA-2026-027'])
    expect(ids((await l({ etape: 'CERTIFICATION' })).contenu)).toEqual(['LIA-2026-022', 'LIA-2026-027'])
    expect(ids((await l({ bloquees: true })).contenu)).toEqual(['LIA-2026-030'])
    expect(ids((await l({ mesProjets: true })).contenu)).toEqual(['LIA-2026-041', 'LIA-2026-030', 'LIA-2026-019', 'LIA-2025-088', 'LIA-2026-027'])
    const p3 = await s.parcours.listerLiaisons({ page: 3, taille: 4 })
    expect(p3).toMatchObject({ total: 10 })
    expect(p3.contenu).toHaveLength(2)
  })
  it('résumé calculé : étape courante, couvertures, retards, prochaine échéance, date cible', async () => {
    const r = (await svc().parcours.listerLiaisons({ page: 1, taille: 50 })).contenu
    const de = (id: string) => r.find((x) => x.id === id)!
    expect(de('LIA-2026-022')).toMatchObject({ statut: 'EN_TEST', etapeCourante: 'CERTIFICATION', conducteurNom: 'Conducteur de projet 2', tachesEnRetard: 1, bloquee: false, couvertureCertification: { taux: 75, numerateur: 3, denominateur: 4 } })
    expect(de('LIA-2025-088')).toMatchObject({ etapeCourante: 'SUIVI', couvertureCertification: { taux: 100 }, couvertureMiseEnService: { taux: 33, numerateur: 1, denominateur: 3 }, dateCibleProduction: '2026-08-30' })
    expect(de('LIA-2026-044')).toMatchObject({ conducteurNom: null, dateCibleProduction: null, prochaineEcheance: '2026-10-03' })
    expect(de('LIA-2026-030')).toMatchObject({ bloquee: true, etapeCourante: 'INTEGRATION' })
  })
  it('intervenants : conducteurs et personnes d’une fonction', async () => {
    const s = svc()
    expect(await s.parcours.conducteurs()).toEqual(PERSONNES.filter((p) => p.fonction === 'CONDUCTEUR'))
    expect((await s.parcours.personnes('RSSI')).map((p) => p.id)).toEqual(['U-RSSI'])
    expect(await s.parcours.personnes('PARTICIPANT')).toEqual([])
  })
  it('ouverture au cadrage : liaison active en double refusée, conducteur non habilité refusé', async () => {
    const s = svc()
    await expect(s.parcours.ouvrirLiaison({ institutionId: 'INST-0105', typeProjet: 'PAIEMENT_MOBILE', chefDeProjetId: 'U-COND-1' })).rejects.toEqual(refus('HOM_LIAISON_DUPLICATE'))
    await expect(s.parcours.ouvrirLiaison({ institutionId: 'INST-0999', typeProjet: 'DELEGATAIRE', chefDeProjetId: 'U-SNA-1' })).rejects.toEqual(refus('HOM_LEAD_NOT_ELIGIBLE'))
    await expect(s.parcours.ouvrirLiaison({ institutionId: 'INST-0999', typeProjet: 'DELEGATAIRE', chefDeProjetId: 'inconnu' })).rejects.toEqual(refus('HOM_LEAD_NOT_ELIGIBLE'))
    // Une liaison en production n'empêche pas d'en ouvrir une nouvelle du même type.
    const l = await s.parcours.ouvrirLiaison({ institutionId: 'INST-0042', typeProjet: 'PAIEMENT_MOBILE', chefDeProjetId: 'U-COND-2' })
    expect(l).toMatchObject({ id: 'LIA-2026-060', institutionNom: 'Établissement de paiement A', statut: 'CADREE', etapeCourante: 'CADRAGE', conducteurNom: 'Conducteur de projet 2', bloquee: false })
    const d = await s.parcours.dossier(l.id)
    expect(d.perimetre.map((p) => [p.serviceCode, p.inclus, p.etat])).toEqual([['PAIEMENT_MARCHAND', true, 'NON_DEMARRE'], ['REMITTANCE', true, 'NON_DEMARRE'], ['FACTURE', true, 'NON_DEMARRE'], ['CASH_IN', true, 'NON_DEMARRE']])
    // Cadrage engagé, non clos (rang 3 à faire) : l'affectation des ressources (rang 6) n'est pas encore offerte.
    expect(d.actionsPermises).toEqual(['DECLARER_BLOCAGE', 'DESIGNER_CONDUCTEUR'])
    expect((await s.parcours.operations(l.id)).find((o) => o.rang === '3')?.etat).toBe('A_FAIRE')
    expect((await s.parcours.ouvrirLiaison({ institutionId: 'INST-0017', typeProjet: 'DELEGATAIRE', chefDeProjetId: 'U-COND-1' })).institutionNom).toBe('Banque B')
    expect((await s.parcours.ouvrirLiaison({ institutionId: 'INST-0999', typeProjet: 'DELEGATAIRE', chefDeProjetId: 'U-COND-1' })).institutionNom).toBe('Nouvel adhérent')
    expect((await s.parcours.listerLiaisons({ page: 1, taille: 50 })).total).toBe(13)
  })
  it('désignation du conducteur : pourvoit ses places R et V, motif exigé pour une redésignation', async () => {
    const s = svc()
    expect((await s.parcours.dossier('LIA-2026-044')).actionsPermises).toEqual(['DESIGNER_CONDUCTEUR', 'AFFECTER_RESSOURCES'])
    expect((await op(s, 'LIA-2026-044', '18')).raci.R).toBeNull()
    await expect(s.parcours.designerConducteur('LIA-2026-044', 'U-SNA-1', '')).rejects.toEqual(refus('HOM_LEAD_NOT_ELIGIBLE'))
    await s.parcours.designerConducteur('LIA-2026-044', 'U-COND-2', '')
    expect((await op(s, 'LIA-2026-044', '18')).raci).toMatchObject({ R: { id: 'U-COND-2' }, V: { fonction: 'CHEF_DES' } })
    expect((await op(s, 'LIA-2026-044', '4')).raci.V).toMatchObject({ id: 'U-COND-2' })
    expect((await s.parcours.dossier('LIA-2026-044')).actionsPermises).toContain('DECLARER_BLOCAGE')
    await expect(s.parcours.designerConducteur('LIA-2026-044', 'U-COND-1', '  ')).rejects.toEqual(refus('HOM_DESIGNATION_REASON_REQUIRED'))
    await s.parcours.designerConducteur('LIA-2026-027', 'U-COND-2', 'Réorganisation du portefeuille')
    expect((await op(s, 'LIA-2026-027', 'D1')).raci.R).toMatchObject({ id: 'U-COND-2' })
    expect((await op(s, 'LIA-2026-027', '19')).raci.V).toMatchObject({ id: 'U-COND-2' })
    expect((await s.parcours.dossier('LIA-2026-027')).conducteur?.id).toBe('U-COND-2')
    await expect(s.parcours.designerConducteur('inconnue', 'U-COND-1', 'x')).rejects.toEqual(refus('HOM_LIAISON_NOT_FOUND'))
  })
})

describe('parcours simulé : dossier numérique et opérations', () => {
  it('opérations filtrées par étape et par fonction du responsable', async () => {
    const s = svc()
    expect((await s.parcours.operations('LIA-2026-041', { etape: 'ATELIERS' })).map((o) => o.rang)).toEqual(['8', '9', '10', '11'])
    expect((await s.parcours.operations('LIA-2026-041', { fonction: 'SNA' })).map((o) => o.rang)).toEqual(['10', '15', '16', '17', '33'])
    expect((await s.parcours.operations('LIA-2026-041', { etape: 'SUIVI', fonction: 'SNA' })).map((o) => o.rang)).toEqual(['33'])
    // Les rangs 1 et 2 (étapes 1 et 2) relèvent de MS-PARTICIPANT : le parcours commence au rang 3.
    expect((await s.parcours.operations('LIA-2026-041'))[0].rang).toBe('3')
    await expect(s.parcours.operations('inconnue')).rejects.toEqual(refus('HOM_LIAISON_NOT_FOUND'))
  })
  it('qualités R/C/V/I/E, plateforme d’exécution et grain de preuve de chaque opération', async () => {
    const s = svc()
    const o9 = await op(s, 'LIA-2026-041', '9')
    expect(o9.raci).toMatchObject({ R: { fonction: 'POLE_RESEAU' }, C: [], V: { id: 'U-COND-1' }, I: [{ fonction: 'CHEF_DDRM' }], E: [{ id: 'EXT-INST-0105', fonction: 'PARTICIPANT' }] })
    expect((await op(s, 'LIA-2026-041', '15'))).toMatchObject({ grain: 'PHASE', plateforme: 'SIMULATEUR' })
    expect((await op(s, 'LIA-2026-022', '16')).plateforme).toBeNull()
    expect((await op(s, 'LIA-2026-041', 'M1')).plateforme).toBe('PLATEFORME_MOBILE')
    expect((await op(s, 'LIA-2026-041', '18')).raci.V).toMatchObject({ fonction: 'CHEF_DES' })
    // Délégataire : le personnalisateur, tiers externe, est responsable ou exécutant.
    expect((await op(s, 'LIA-2026-019', 'D1')).raci).toMatchObject({ R: { id: 'U-COND-1' }, E: [{ fonction: 'PERSONNALISATEUR' }] })
    expect((await op(s, 'LIA-2026-019', 'D2')).raci).toMatchObject({ R: { id: 'EXT-PERSO', nom: 'Personnalisateur P' }, C: [{ fonction: 'RSSI' }] })
    // Rang 6 porté par les chefs de département : le chef du DES en répond.
    expect((await op(s, 'LIA-2026-044', '6')).raci.R).toMatchObject({ fonction: 'CHEF_DES' })
  })
  it('preuves des opérations faites : valeur de référence relisible, code de contrôle sans clé, document de phase', async () => {
    const s = svc()
    expect((await op(s, 'LIA-2026-022', '12')).preuve).toMatchObject({ valeurReference: { libelle: 'parcours.valeur.12', valeur: 'participant-022.essai:8443' }, codeControle: undefined })
    expect((await op(s, 'LIA-2026-022', '15')).preuve?.nom).toBe('document-parametrage-LIA-2026-022.pdf')
    const c1 = (await op(s, 'LIA-2026-012', 'C1')).preuve!
    expect(c1.codeControle).toMatch(/^[0-9A-F]{6}$/)
    expect(c1.empreinte).toMatch(/^[0-9a-f]{64}$/)
    expect((await op(s, 'LIA-2026-012', 'C2')).preuve?.valeurReference).toEqual({ libelle: 'parcours.valeur.C2', valeur: 'REF-C2-012' })
    const ouverte = await op(s, 'LIA-2026-022', '17')
    expect(ouverte).toMatchObject({ preuve: null, faiteLe: null, actionsPermises: ['DEPOSER_PREUVE'], sla: 'DEPASSE' })
    expect(await op(s, 'LIA-2026-022', '19')).toMatchObject({ etat: 'VERROUILLEE', predecesseursNonFaits: ['18'], actionsPermises: [], sla: null })
  })
  it('étapes à six éléments : close seulement si tous sont réunis ; étapes 1 et 2 closes (MS-PARTICIPANT)', async () => {
    const s = svc()
    const d = await s.parcours.dossier('LIA-2026-041')
    expect(d.etapes.map((e) => [e.code, e.etat, e.rang])).toEqual([
      ['PRISE_CONTACT', 'CLOSE', 1], ['ADHESION', 'CLOSE', 2], ['CADRAGE', 'CLOSE', 3], ['LANCEMENT', 'EN_COURS', 4], ['ATELIERS', 'A_VENIR', 5],
      ['INTEGRATION', 'A_VENIR', 6], ['TESTS', 'A_VENIR', 7], ['CERTIFICATION', 'A_VENIR', 8], ['APPROBATION_SECURITE', 'A_VENIR', null],
      ['MISE_EN_PRODUCTION', 'A_VENIR', 9], ['SUIVI', 'A_VENIR', 10],
    ])
    const lancement = d.etapes[3]
    expect(lancement).toMatchObject({ actionsFaites: 1, actionsTotal: 2, sla: 'ECHEANCE_PROCHE', valideeLe: null, closeLe: null, elements: { RESPONSABLE: true, ACTIONS: false, LIVRABLES: false, VALIDATION: false, PREUVE: true, DATE: false } })
    expect(d.etapes[2]).toMatchObject({ etat: 'CLOSE', sla: null, elements: { RESPONSABLE: true, ACTIONS: true, LIVRABLES: true, VALIDATION: true, PREUVE: true, DATE: true } })
    expect(d.etapes[2].closeLe).not.toBeNull()
    expect(d.etapes[4]).toMatchObject({ debut: null, elements: { RESPONSABLE: true, ACTIONS: false, PREUVE: false } })
    expect(d.etapes[1]).toMatchObject({ porteurs: ['DDRM'], livrables: [{ libelleKey: 'parcours.livrable.dossierAdhesion', fourni: true }], validateur: { fonction: 'CHEF_DDRM' } })
    // Les livrables d'une étape sont ses pièces de phase et sa pièce pivot.
    expect((await etape(s, 'LIA-2026-027', 'INTEGRATION')).livrables).toEqual([{ libelleKey: 'parcours.op.15', fourni: true }])
    expect((await etape(s, 'LIA-2026-027', 'CERTIFICATION')).livrables).toEqual([{ libelleKey: 'parcours.op.19', fourni: false }])
    // Une étape bloquée l'est par ses opérations bloquées ; la branche mobile compte 17 opérations et le tronc aval 5.
    expect((await etape(s, 'LIA-2026-030', 'INTEGRATION')).etat).toBe('BLOQUEE')
    expect((await etape(s, 'LIA-2026-041', 'MISE_EN_PRODUCTION')).actionsTotal).toBe(17 + 5)
  })
  it('périmètre : états des services, avancement repris du simulateur pour la branche mobile', async () => {
    const s = svc()
    const p = (await s.parcours.dossier('LIA-2025-088')).perimetre
    expect(p.map((x) => [x.serviceCode, x.etat, x.avancementSimulateur])).toEqual([['PAIEMENT_MARCHAND', 'OUVERT', 100], ['REMITTANCE', 'CERTIFIE', 100], ['FACTURE', 'CERTIFIE', 100], ['CASH_IN', 'NON_DEMARRE', null]])
    expect(p[0]).toMatchObject({ entreeEssai: expect.any(String), sortieEssai: expect.any(String), certifieLe: expect.any(String) })
    expect(p[3]).toMatchObject({ entreeEssai: null, sortieEssai: null, certifieLe: null })
    const t = (await s.parcours.dossier('LIA-2026-022')).perimetre
    expect(t.map((x) => x.etat)).toEqual(['CERTIFIE', 'CERTIFIE', 'CERTIFIE', 'EN_TEST'])
    expect(t[3]).toMatchObject({ entreeEssai: expect.any(String), sortieEssai: null, avancementSimulateur: null })
    const d = await s.parcours.dossier('LIA-2026-019')
    expect(d).toMatchObject({ banquePartenaireNom: 'Banque D', personnalisateurNom: 'Personnalisateur P', versionReferentiel: 4, liaisonOrigineId: null, responsableFonctionnel: { fonction: 'DDRM' }, responsableTechnique: { fonction: 'SNA' } })
    expect((await s.parcours.dossier('LIA-2026-041')).versionReferentiel).toBe(2)
    expect((await s.parcours.dossier('LIA-2026-044')).liaisonOrigineId).toBe('LIA-2026-022')
  })
})

describe('parcours simulé : conduite et exécution (étapes 4 à 9)', () => {
  it('affectation des ressources (rang 6) : ouverte seulement au lancement, au moins une ressource', async () => {
    const s = svc()
    // LIA-2026-041 : le rang 6 est déjà fait, l'action n'est plus offerte et le service la refuse.
    expect((await s.parcours.dossier('LIA-2026-041')).actionsPermises).not.toContain('AFFECTER_RESSOURCES')
    await expect(s.parcours.affecterRessources('LIA-2026-041', [{ fonction: 'SNA', personneId: 'U-SNA-1' }])).rejects.toEqual(refus('HOM_STEP_NOT_OPEN'))
    await expect(s.parcours.affecterRessources('LIA-2026-044', [])).rejects.toEqual(refus('HOM_RESOURCES_REQUIRED'))
    await s.parcours.affecterRessources('LIA-2026-044', [{ fonction: 'SNA', personneId: 'U-SNA-1' }])
    expect(await op(s, 'LIA-2026-044', '6')).toMatchObject({ etat: 'FAITE', actionsPermises: ['CONSULTER'], preuve: { deposePar: 'Chef du département DES' } })
    // La réunion de lancement (rang 7) s'ouvre et attend un livrable (compte rendu), pas une preuve.
    expect(await op(s, 'LIA-2026-044', '7')).toMatchObject({ etat: 'A_FAIRE', actionsPermises: ['DEPOSER_LIVRABLE'], predecesseursNonFaits: [] })
    expect((await s.parcours.dossier('LIA-2026-044')).actionsPermises).not.toContain('AFFECTER_RESSOURCES')
  })
  it('livrables : réunion puis ateliers ; l’étape se clôt quand les six éléments sont réunis', async () => {
    const s = svc()
    await s.parcours.deposerLivrable('LIA-2026-041:7', fichier('cr-lancement.pdf'), 'Réunion tenue')
    expect(await op(s, 'LIA-2026-041', '7')).toMatchObject({ etat: 'FAITE', preuve: { nom: 'cr-lancement.pdf', documentRef: 'DOC-LIA-2026-041-7' } })
    expect((await s.parcours.operations('LIA-2026-041', { etape: 'ATELIERS' })).map((o) => [o.etat, o.actionsPermises[0]])).toEqual(Array(4).fill(['A_FAIRE', 'DEPOSER_LIVRABLE']))
    expect((await s.parcours.dossier('LIA-2026-041')).etapeCourante).toBe('ATELIERS')
    expect((await etape(s, 'LIA-2026-041', 'LANCEMENT')).etat).toBe('CLOSE')
    for (const r of ['8', '9', '10']) await s.parcours.deposerLivrable(`LIA-2026-041:${r}`, fichier(), 'Atelier tenu')
    expect((await etape(s, 'LIA-2026-041', 'ATELIERS'))).toMatchObject({ etat: 'EN_COURS', actionsFaites: 3, elements: { ACTIONS: false, DATE: false } })
    await s.parcours.deposerLivrable('LIA-2026-041:11', fichier(), 'Atelier tenu')
    const ateliers = await etape(s, 'LIA-2026-041', 'ATELIERS')
    expect(ateliers).toMatchObject({ etat: 'CLOSE', actionsFaites: 4, sla: null, valideeLe: '2026-09-29', closeLe: '2026-09-29', elements: { RESPONSABLE: true, ACTIONS: true, LIVRABLES: true, VALIDATION: true, PREUVE: true, DATE: true } })
    // L'atelier réseau (rang 9) ouvre la communication des coordonnées d'essai par le participant (rang 12).
    expect(await etat(s, 'LIA-2026-041', '12')).toBe('A_FAIRE')
    expect((await s.parcours.dossier('LIA-2026-041')).etapeCourante).toBe('INTEGRATION')
  })
  it('livrable refusé : opération fermée, inconnue, ou qui exige un code de contrôle ou une valeur relevée', async () => {
    const s = svc()
    await expect(s.parcours.deposerLivrable('LIA-2026-041:13', fichier(), 'x')).rejects.toEqual(refus('HOM_OPERATION_NOT_OPEN'))
    await expect(s.parcours.deposerLivrable('LIA-2026-041:7', fichier(), 'x')).resolves.toBeUndefined()
    await expect(s.parcours.deposerLivrable('LIA-2026-041:7', fichier(), 'x')).rejects.toEqual(refus('HOM_OPERATION_NOT_OPEN'))
    await expect(s.parcours.deposerLivrable('LIA-2026-041:Z9', fichier(), 'x')).rejects.toEqual(refus('HOM_OPERATION_NOT_FOUND'))
    await expect(s.parcours.deposerLivrable('INCONNUE:7', fichier(), 'x')).rejects.toEqual(refus('HOM_LIAISON_NOT_FOUND'))
    // Aucun contournement de D-59 : une opération sur clé ne se clôt pas par un livrable sans code de contrôle.
    await expect(s.parcours.deposerLivrable('LIA-2026-005:29', fichier(), 'x')).rejects.toEqual(refus('HOM_KEY_CHECK_VALUE_REQUIRED'))
    await expect(s.parcours.deposerLivrable('LIA-2026-036:12', fichier(), 'x')).rejects.toEqual(refus('HOM_REFERENCE_VALUE_REQUIRED'))
    expect(await etat(s, 'LIA-2026-005', '29')).toBe('A_FAIRE')
  })
  it('attribution des qualités : un seul responsable, validateur distinct du responsable, C/I/E cumulables', async () => {
    const s = svc()
    const id = 'LIA-2026-041:8'
    await s.parcours.attribuer(id, 'R', 'U-DDRM-2', 'Charge de travail')
    await expect(s.parcours.attribuer(id, 'V', 'U-DDRM-2', 'x')).rejects.toEqual(refus('HOM_VALIDATOR_IS_RESPONSIBLE'))
    // Symétrique : le validateur en place ne peut pas devenir responsable.
    await expect(s.parcours.attribuer(id, 'R', 'U-COND-1', 'x')).rejects.toEqual(refus('HOM_VALIDATOR_IS_RESPONSIBLE'))
    await s.parcours.attribuer(id, 'V', 'U-CDDRM', 'Validation par le chef')
    await s.parcours.attribuer(id, 'C', 'U-SNA-1', 'Avis')
    await s.parcours.attribuer(id, 'C', 'U-RES-1', 'Avis')
    await s.parcours.attribuer(id, 'I', 'U-DG', 'Information')
    await s.parcours.attribuer(id, 'E', 'U-EXP', 'Exécution')
    const raci = (await op(s, 'LIA-2026-041', '8')).raci
    expect(raci.R?.id).toBe('U-DDRM-2')
    expect(raci.V?.id).toBe('U-CDDRM')
    expect(raci.C.map((p) => p.id)).toEqual(['U-SNA-1', 'U-RES-1'])
    expect(raci.I.map((p) => p.id)).toEqual(['U-CDDRM', 'U-DG'])
    expect(raci.E.map((p) => p.id)).toEqual(['EXT-INST-0105', 'U-EXP'])
    await expect(s.parcours.attribuer(id, 'C', 'U-INCONNU', 'x')).rejects.toEqual(refus('HOM_PERSON_UNKNOWN'))
    await expect(s.parcours.attribuer('LIA-2026-041:Z1', 'C', 'U-DG', 'x')).rejects.toEqual(refus('HOM_OPERATION_NOT_FOUND'))
  })
  it('preuve d’une opération sur clé : code de contrôle exigé et hexadécimal, jamais la clé ; puis validation', async () => {
    const s = svc()
    const id = 'LIA-2026-005:29'
    await expect(s.parcours.deposerPreuve(id, fichier(), {})).rejects.toEqual(refus('HOM_KEY_CHECK_VALUE_REQUIRED'))
    await expect(s.parcours.deposerPreuve(id, fichier(), { codeControle: 'cle-secrete' })).rejects.toEqual(refus('HOM_KEY_CHECK_VALUE_INVALID'))
    await expect(s.parcours.deposerPreuve(id, fichier(), { codeControle: 'ABC' })).rejects.toEqual(refus('HOM_KEY_CHECK_VALUE_INVALID'))
    await expect(s.parcours.validerOperation(id)).rejects.toEqual(refus('HOM_OPERATION_NOT_IN_VALIDATION'))
    await s.parcours.deposerPreuve(id, fichier('ceremonie.pdf'), { codeControle: 'A1B2C3' })
    expect(await op(s, 'LIA-2026-005', '29')).toMatchObject({ etat: 'EN_VALIDATION', actionsPermises: ['VALIDER'], preuve: { nom: 'ceremonie.pdf', codeControle: 'A1B2C3', deposePar: 'Ingénieur pôle système' } })
    await expect(s.parcours.deposerPreuve(id, fichier(), { codeControle: 'A1B2C3' })).rejects.toEqual(refus('HOM_OPERATION_NOT_OPEN'))
    // L'opération en validation apparaît chez son validateur (le conducteur), en qualité V.
    const t = (await s.parcours.tableauActeur(['CONDUCTEUR'])).taches.find((x) => x.operationId === id)
    expect(t).toMatchObject({ qualite: 'V', rang: '29', lien: '/des/liaisons/LIA-2026-005?onglet=operations&operation=LIA-2026-005%3A29' })
    await s.parcours.validerOperation(id, 'assertion')
    expect(await op(s, 'LIA-2026-005', '29')).toMatchObject({ etat: 'FAITE', sla: null, faiteLe: '2026-09-29', actionsPermises: ['CONSULTER'] })
    // Le rang 30 attend encore le rang 28 (oubli sans symptôme) : il reste verrouillé.
    expect(await op(s, 'LIA-2026-005', '30')).toMatchObject({ etat: 'VERROUILLEE', predecesseursNonFaits: ['28'] })
    await clore(s, await op(s, 'LIA-2026-005', '28'))
    expect(await op(s, 'LIA-2026-005', '30')).toMatchObject({ etat: 'A_FAIRE', predecesseursNonFaits: [], actionsPermises: ['DEPOSER_PREUVE'] })
  })
  it('preuve à valeur de référence : valeur relevée exigée et restituée ; opération verrouillée ou bloquée refusée', async () => {
    const s = svc()
    await expect(s.parcours.deposerPreuve('LIA-2026-036:12', fichier(), {})).rejects.toEqual(refus('HOM_REFERENCE_VALUE_REQUIRED'))
    await s.parcours.deposerPreuve('LIA-2026-036:12', fichier(), { valeurReference: 'hote.essai:8443' })
    expect((await op(s, 'LIA-2026-036', '12')).preuve?.valeurReference).toEqual({ libelle: 'parcours.valeur.12', valeur: 'hote.essai:8443' })
    await expect(s.parcours.deposerPreuve('LIA-2026-036:13', fichier(), {})).rejects.toEqual(refus('HOM_OPERATION_NOT_OPEN'))
    await expect(s.parcours.deposerPreuve('LIA-2026-030:14', fichier(), {})).rejects.toEqual(refus('HOM_OPERATION_NOT_OPEN'))
  })
})

describe('parcours simulé : blocages datés (annexe B)', () => {
  it('levée du blocage existant : opérations débloquées, liaison et pilotage à jour', async () => {
    const s = svc()
    const d = await s.parcours.dossier('LIA-2026-030')
    expect(d.blocages).toEqual([expect.objectContaining({ id: 'BLQ-0007', etape: 'INTEGRATION', depuis: '2026-09-20', echeanceLevee: '2026-09-27', leveLe: null, actionsPermises: ['LEVER'] })])
    expect((await s.parcours.pilotage()).questions.projetsBloques).toEqual([expect.objectContaining({ liaisonId: 'LIA-2026-030', jours: 9 })])
    await s.parcours.leverBlocage('BLQ-0007', 'Élément reçu')
    expect((await s.parcours.dossier('LIA-2026-030')).blocages[0]).toMatchObject({ leveLe: '2026-09-29', actionsPermises: [] })
    expect(await etat(s, 'LIA-2026-030', '14')).toBe('A_FAIRE')
    expect((await etape(s, 'LIA-2026-030', 'INTEGRATION')).etat).toBe('EN_COURS')
    expect((await s.parcours.listerLiaisons({ page: 1, taille: 50, bloquees: true })).total).toBe(0)
    expect((await s.parcours.pilotage()).projetsBloques).toBe(0)
    await expect(s.parcours.leverBlocage('BLQ-INCONNU', 'x')).rejects.toEqual(refus('HOM_BLOCKAGE_NOT_FOUND'))
  })
  it('déclaration : responsable désigné (ou le conducteur à défaut), étape courante, identifiants distincts entre liaisons', async () => {
    const s = svc()
    const b1 = await s.parcours.declarerBlocage('LIA-2026-041', { motif: 'Salle indisponible', prochaineAction: 'Replanifier', responsableId: 'U-DDRM-1', echeanceLevee: '2026-10-05' })
    const b2 = await s.parcours.declarerBlocage('LIA-2026-022', { motif: 'Simulateur arrêté', prochaineAction: 'Relancer', responsableId: 'inconnu', echeanceLevee: '2026-10-02' })
    expect(b1).toMatchObject({ liaisonId: 'LIA-2026-041', etape: 'LANCEMENT', depuis: '2026-09-29', responsable: { id: 'U-DDRM-1' }, leveLe: null })
    expect(b2).toMatchObject({ etape: 'CERTIFICATION', responsable: { fonction: 'CONDUCTEUR' } })
    expect(b1.id).not.toBe(b2.id)
    expect(ids((await s.parcours.listerLiaisons({ page: 1, taille: 50, bloquees: true })).contenu)).toEqual(['LIA-2026-041', 'LIA-2026-030', 'LIA-2026-022'])
    expect((await s.parcours.tableauActeur(['CHEF_DES'])).blocages).toHaveLength(3)
    // Chaque blocage se lève séparément.
    await s.parcours.leverBlocage(b2.id, 'Simulateur relancé')
    expect((await s.parcours.dossier('LIA-2026-022')).blocages[0].leveLe).toBe('2026-09-29')
    expect((await s.parcours.dossier('LIA-2026-041')).blocages[0].leveLe).toBeNull()
    await expect(s.parcours.declarerBlocage('inconnue', { motif: 'x', prochaineAction: 'x', responsableId: 'x', echeanceLevee: '2026-10-01' })).rejects.toEqual(refus('HOM_LIAISON_NOT_FOUND'))
  })
})

describe('parcours simulé : constat de la couverture (rang 17) et service supplémentaire (rang 35)', () => {
  it('constat : seulement après les essais, une seule fois ; ouvre l’établissement du procès-verbal', async () => {
    const s = svc()
    await expect(s.parcours.constaterCouverture('LIA-2026-041')).rejects.toEqual(refus('HOM_COVERAGE_NOT_READY'))
    expect((await s.parcours.dossier('LIA-2026-022')).actionsPermises).toContain('CONSTATER_COUVERTURE')
    // LIA-2026-027 : le constat est déjà fait, l'action n'est pas offerte.
    expect((await s.parcours.dossier('LIA-2026-027')).actionsPermises).not.toContain('CONSTATER_COUVERTURE')
    await s.parcours.constaterCouverture('LIA-2026-022')
    expect(await op(s, 'LIA-2026-022', '17')).toMatchObject({ etat: 'FAITE', preuve: { documentRef: 'COUV-022', deposePar: 'Ingénieur Normes et Agréments 1' } })
    expect(await etat(s, 'LIA-2026-022', '18')).toBe('A_FAIRE')
    expect((await s.parcours.dossier('LIA-2026-022')).actionsPermises).not.toContain('CONSTATER_COUVERTURE')
    await expect(s.parcours.constaterCouverture('LIA-2026-022')).rejects.toEqual(refus('HOM_COVERAGE_NOT_READY'))
  })
  it('service supplémentaire : sur une liaison en production, nouvelle liaison au cadrage rattachée à l’origine', async () => {
    const s = svc()
    await expect(s.parcours.ouvrirServiceSupplementaire('LIA-2026-022', ['RETRAIT'])).rejects.toEqual(refus('HOM_LIAISON_NOT_IN_PRODUCTION'))
    expect((await s.parcours.dossier('LIA-2025-088')).actionsPermises).toContain('OUVRIR_SERVICE_SUPPLEMENTAIRE')
    const n = await s.parcours.ouvrirServiceSupplementaire('LIA-2025-088', ['CASH_IN', 'INCONNU'])
    expect(n).toMatchObject({ id: 'LIA-2026-060', statut: 'CADREE', etapeCourante: 'CADRAGE', conducteurNom: null, institutionNom: 'Établissement de paiement A', typeProjet: 'PAIEMENT_MOBILE' })
    const d = await s.parcours.dossier(n.id)
    expect(d).toMatchObject({ liaisonOrigineId: 'LIA-2025-088', perimetre: [expect.objectContaining({ serviceCode: 'CASH_IN', inclus: true })] })
    expect(d.perimetre).toHaveLength(1)
    // Sans repasser par l'adhésion : la nouvelle liaison est aussitôt visible du participant.
    expect(ids(await s.portailParcours.mesLiaisons())).toContain(n.id)
  })
})

describe('parcours simulé : tableaux de bord par acteur et pilotage', () => {
  it('compteurs de chaque fonction (valeur nulle : ton « ok »)', async () => {
    const s = svc()
    expect(await compteurs(s, ['CHEF_DDRM'])).toEqual({ conducteursADesigner: 1, liaisonsAmont: 3, ateliersATenir: 0, perimetresAArreter: 2, tachesEnRetard: 0, echeancesProches: 0 })
    expect(await compteurs(s, ['DDRM'])).toEqual({ ateliersATenir: 0, perimetresAArreter: 2, tachesEnRetard: 0, echeancesProches: 2 })
    expect(await compteurs(s, ['CHEF_DES'])).toEqual({ ressourcesAAffecter: 1, enIntegrationTests: 1, pvASigner: 1, tachesEnRetard: 1, echeancesProches: 0 })
    expect(await compteurs(s, ['CONDUCTEUR'])).toEqual({ mesProjets: 5, pvAEtablir: 0, tachesEnRetard: 0, echeancesProches: 0 })
    expect(await compteurs(s, ['SNA'])).toEqual({ essaisEnCours: 0, documentsParametrage: 0, couverturesAConstater: 1, constatsMiseEnService: 0, tachesEnRetard: 2, echeancesProches: 0 })
    expect(await compteurs(s, ['PRODUCTION_MONETIQUE'])).toEqual({ dossiersChangement: 0, operationsBranche: 1, rapportsAConsolider: 0, tachesEnRetard: 1, echeancesProches: 0 })
    expect(await compteurs(s, ['POLE_RESEAU'])).toEqual({ liensEssai: 1, operationsReseau: 1, tachesEnRetard: 1, echeancesProches: 0 })
    expect(await compteurs(s, ['POLE_SYSTEME'])).toEqual({ scriptsDepot: 1, operationsSysteme: 2, tachesEnRetard: 2, echeancesProches: 0 })
    expect(await compteurs(s, ['RSSI'])).toEqual({ attestationsAVerifier: 0, approbationsEnAttente: 1, ceremoniesCles: 0, tachesEnRetard: 0, echeancesProches: 0 })
    expect(await compteurs(s, ['CONFORMITE'])).toEqual({ visasAttendus: 1, fintechsCobac: 1, tachesEnRetard: 0, echeancesProches: 0 })
    expect(await compteurs(s, ['DG'])).toEqual({ visasAttendus: 1, tachesEnRetard: 0, echeancesProches: 0 })
    const t = await s.parcours.tableauActeur(['CHEF_DDRM'])
    expect(t.compteurs[0]).toEqual({ cle: 'conducteursADesigner', valeur: 1, ton: 'warn', lien: '/des/liaisons?sansConducteur=1' })
    expect(t.compteurs.find((c) => c.cle === 'ateliersATenir')).toEqual({ cle: 'ateliersATenir', valeur: 0, ton: 'ok', lien: null })
    expect((await s.parcours.tableauActeur(['POLE_SYSTEME'])).compteurs.find((c) => c.cle === 'scriptsDepot')?.ton).toBe('crit')
  })
  it('tâches de l’acteur : responsable principal, triées par échéance ; chefs de département pour le rang 6', async () => {
    const s = svc()
    const sna = await s.parcours.tableauActeur(['SNA'])
    expect(sna).toMatchObject({ fonction: 'SNA' })
    expect(sna.taches.map((t) => [t.liaisonId, t.rang, t.qualite, t.sla])).toEqual([['LIA-2026-022', '17', 'R', 'DEPASSE'], ['LIA-2026-036', '10', 'R', 'DEPASSE']])
    const chefs = await s.parcours.tableauActeur(['CHEFS_DEPARTEMENT'])
    expect(chefs.taches.map((t) => t.operationId)).toEqual(['LIA-2026-027:19', 'LIA-2026-044:6'])
    expect((await s.parcours.tableauActeur(['POLE_SYSTEME'])).taches.every((t) => t.oubliSansSymptome === (t.rang === '28'))).toBe(true)
  })
  it('liaisons de l’acteur : « mes projets » du conducteur seul, toutes sinon ; fonction par défaut', async () => {
    const s = svc()
    expect((await s.parcours.tableauActeur(['CONDUCTEUR'])).liaisons).toHaveLength(5)
    expect((await s.parcours.tableauActeur(['CONDUCTEUR', 'SNA'])).liaisons).toHaveLength(10)
    const vide = await s.parcours.tableauActeur([])
    expect(vide).toMatchObject({ fonction: 'DDRM', taches: [] })
    expect(vide.compteurs.map((c) => c.cle)).toEqual(['tachesEnRetard', 'echeancesProches'])
  })
  it('pilotage : quatre compteurs, répartition par étape (3 à 10), six questions de l’annexe B', async () => {
    const s = svc()
    const p = await s.parcours.pilotage()
    expect(p).toMatchObject({ projetsActifs: 9, projetsBloques: 1, tachesEnRetard: 12, echeancesProches: 2, delaiMoyenCertificationJours: 46, tauxRespectDelais: 78 })
    expect(p.repartitionParEtape.map((x) => x.etape)).toEqual(['CADRAGE', 'LANCEMENT', 'ATELIERS', 'INTEGRATION', 'TESTS', 'CERTIFICATION', 'APPROBATION_SECURITE', 'MISE_EN_PRODUCTION', 'SUIVI'])
    expect(p.repartitionParEtape.reduce((n, x) => n + x.nombre, 0)).toBe(10)
    expect(p.repartitionParEtape.find((x) => x.etape === 'CERTIFICATION')?.nombre).toBe(2)
    expect(p.questions.enCertification).toBe(3)
    expect(p.questions.servicesPlusDemandes.map((x) => [x.serviceCode, x.nombre])).toEqual([['RETRAIT', 6], ['PAIEMENT', 6], ['TRANSFERT', 4], ['PAIEMENT_MARCHAND', 3]])
    expect(p.questions.lancementsParService12Mois.length).toBeGreaterThan(4)
    expect(p.questions.servicesPlusLongs[0]).toEqual({ serviceCode: 'REMITTANCE', libelle: 'Rémittence entrante', delaiMoyenJours: 38 })
    expect(p.questions.delaiMoyenAdhesionProductionJours).toBe(174)
    // Une nouvelle liaison compte aussitôt parmi les projets actifs, au cadrage.
    await s.parcours.ouvrirLiaison({ institutionId: 'INST-0999', typeProjet: 'CARTE_INTERBANCAIRE', chefDeProjetId: 'U-COND-1' })
    const q = await s.parcours.pilotage()
    expect(q.projetsActifs).toBe(10)
    expect(q.repartitionParEtape.find((x) => x.etape === 'CADRAGE')?.nombre).toBe(2)
  })
})
