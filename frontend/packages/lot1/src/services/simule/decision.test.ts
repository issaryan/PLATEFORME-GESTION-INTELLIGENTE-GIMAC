import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { OperationParcours } from '../../modele/parcours'
import { servicesSimules } from './index'

// Décision de mise en production simulée (modèle hybride de D-56) : verrou 1 (procès-verbal établi à 100 %,
// signé des deux parties, avec assertion), verrou 2 (dossier et fiche de changement, attestations vérifiées et
// archivées), visas optionnels, fiche de suivi et constat de mise en service ; portail du participant (qualité E).
type S = ReturnType<typeof servicesSimules>
const svc = () => servicesSimules({ latenceMs: 0 })
const refus = (code: string) => expect.objectContaining({ code })
// DDR-15 : la démonstration date ses gestes du jour courant de Douala ; ces attentes portent sur le 29/09/2026.
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-09-29T09:00:00Z'))
})
afterEach(() => vi.useRealTimers())
const fichier = (nom = 'piece.pdf') => new File(['contenu'], nom, { type: 'application/pdf' })
const op = async (s: S, liaison: string, rang: string) => (await s.parcours.operations(liaison)).find((o) => o.rang === rang)!
const etat = async (s: S, liaison: string, rang: string) => (await op(s, liaison, rang)).etat
const courante = async (s: S, liaison: string) => (await s.parcours.dossier(liaison)).etapeCourante

async function clore(s: S, o: OperationParcours) {
  if (o.actionsPermises.includes('DEPOSER_LIVRABLE')) return s.parcours.deposerLivrable(o.id, fichier(), 'Livrable remis')
  await s.parcours.deposerPreuve(o.id, fichier(), { codeControle: o.cle ? 'A1B2C3' : undefined, valeurReference: o.valeurReference ? `REF-${o.rang}` : undefined })
  await s.parcours.validerOperation(o.id, 'assertion')
}
/** Exécute les opérations ouvertes d'une liaison jusqu'à ce qu'il n'en reste que parmi `sauf` ; renvoie les rangs clos. */
async function executer(s: S, liaison: string, sauf: string[] = []) {
  const clos: string[] = []
  for (;;) {
    const o = (await s.parcours.operations(liaison)).find((x) => x.etat === 'A_FAIRE' && !sauf.includes(x.rang))
    if (!o) return clos
    await clore(s, o)
    clos.push(o.rang)
  }
}
/** LIA-2026-027 (délégataire, participant du portail) : procès-verbal signé des deux parties. */
async function pvSigne(s: S) {
  await s.decision.signerProcesVerbalGimac('LIA-2026-027', 'assertion-chef-des')
  await s.portailParcours.signerProcesVerbal('LIA-2026-027', 'assertion-participant')
}

describe('verrou 1 : procès-verbal de certification (rangs 17 à 19)', () => {
  it('établissement refusé tant que la couverture n’est pas de 100 % ; avant le constat, aucune action', async () => {
    const s = svc()
    expect(await s.decision.procesVerbal('LIA-2026-022')).toMatchObject({ etat: 'NON_ETABLI', couverture: { taux: 75 }, servicesCouverts: ['RETRAIT', 'PAIEMENT', 'TRANSFERT'], etabliPar: null, empreinte: null, actionsPermises: [] })
    await s.parcours.constaterCouverture('LIA-2026-022')
    expect((await s.decision.procesVerbal('LIA-2026-022')).actionsPermises).toEqual(['ETABLIR_BLOQUE'])
    await expect(s.decision.etablirProcesVerbal('LIA-2026-022')).rejects.toEqual(expect.objectContaining({ code: 'CER_COVERAGE_INCOMPLETE', message: expect.stringContaining('75 %') }))
    await expect(s.decision.signerProcesVerbalGimac('LIA-2026-022', 'a')).rejects.toEqual(refus('CER_PV_NOT_SIGNABLE'))
    // Nouvelle liaison : couverture non calculable (0 %) ; déjà établi : le constat ne se refait pas.
    const n = await s.parcours.ouvrirLiaison({ institutionId: 'INST-0999', typeProjet: 'CARTE_INTERBANCAIRE', chefDeProjetId: 'U-COND-1' })
    await expect(s.decision.etablirProcesVerbal(n.id)).rejects.toEqual(expect.objectContaining({ code: 'CER_COVERAGE_INCOMPLETE', message: expect.stringContaining('0 %') }))
    await expect(s.decision.etablirProcesVerbal('LIA-2026-027')).rejects.toEqual(refus('CER_PV_NOT_READY'))
    await expect(s.decision.procesVerbal('inconnue')).rejects.toEqual(refus('HOM_LIAISON_NOT_FOUND'))
  })
  it('signature GIMAC puis participant : réauthentification exigée, la seconde signature clôt le rang 19', async () => {
    const s = svc()
    expect(await s.decision.procesVerbal('LIA-2026-027')).toMatchObject({ etat: 'ETABLI', etabliPar: 'Aïcha Mahamat Nour', couverture: { taux: 100 }, signatureGimac: null, actionsPermises: ['SIGNER_GIMAC'] })
    await expect(s.decision.signerProcesVerbalGimac('LIA-2026-027', '')).rejects.toEqual(refus('IAM_REAUTH_REQUIRED'))
    await s.decision.signerProcesVerbalGimac('LIA-2026-027', 'assertion')
    expect(await s.decision.procesVerbal('LIA-2026-027')).toMatchObject({ etat: 'SIGNE_GIMAC', signatureGimac: { fonction: 'CHEF_DES', signataire: 'Paul Mbarga Atangana', signeLe: '2026-09-29' }, actionsPermises: [] })
    await expect(s.decision.signerProcesVerbalGimac('LIA-2026-027', 'assertion')).rejects.toEqual(refus('CER_PV_NOT_SIGNABLE'))
    expect(await etat(s, 'LIA-2026-027', '19')).toBe('A_FAIRE')
    // Côté portail : signature attendue, puis refusée une fois donnée.
    expect((await s.portailParcours.procesVerbal('LIA-2026-027')).actionsPermises).toEqual(['SIGNER_PARTICIPANT'])
    await expect(s.portailParcours.signerProcesVerbal('LIA-2026-027', '')).rejects.toEqual(refus('IAM_REAUTH_REQUIRED'))
    await s.portailParcours.signerProcesVerbal('LIA-2026-027', 'assertion')
    expect(await s.portailParcours.procesVerbal('LIA-2026-027')).toMatchObject({ etat: 'SIGNE', signatureParticipant: { fonction: 'PARTICIPANT', signataire: 'Correspondant Établissement de paiement A' }, actionsPermises: [] })
    await expect(s.portailParcours.signerProcesVerbal('LIA-2026-027', 'assertion')).rejects.toEqual(refus('CER_PV_NOT_SIGNABLE'))
    expect(await op(s, 'LIA-2026-027', '19')).toMatchObject({ etat: 'FAITE', preuve: { nom: 'proces-verbal-signe.pdf' } })
    expect(await s.parcours.dossier('LIA-2026-027')).toMatchObject({ statut: 'CERTIFIEE', etapeCourante: 'APPROBATION_SECURITE' })
  })
  it('signature participant puis GIMAC : même issue, le chef du DES voit le procès-verbal à signer', async () => {
    const s = svc()
    await s.portailParcours.signerProcesVerbal('LIA-2026-027', 'assertion')
    expect((await s.decision.procesVerbal('LIA-2026-027'))).toMatchObject({ etat: 'SIGNE_PARTICIPANT', actionsPermises: ['SIGNER_GIMAC'] })
    expect((await s.parcours.tableauActeur(['CHEF_DES'])).compteurs.find((c) => c.cle === 'pvASigner')?.valeur).toBe(1)
    await s.decision.signerProcesVerbalGimac('LIA-2026-027', 'assertion')
    expect((await s.decision.procesVerbal('LIA-2026-027')).etat).toBe('SIGNE')
    expect(await etat(s, 'LIA-2026-027', '19')).toBe('FAITE')
    expect((await s.parcours.dossier('LIA-2026-027')).statut).toBe('CERTIFIEE')
  })
  it('chaque instance a son propre état : une certification ne fuit pas dans une autre instance', async () => {
    await pvSigne(svc())
    const autre = svc()
    expect((await autre.parcours.dossier('LIA-2026-027')).statut).toBe('EN_TEST')
    expect((await autre.decision.approbation('LIA-2026-027')).etat).toBe('EN_ATTENTE_DOSSIER')
  })
})

describe('verrou 2 : approbation de la sécurité (rangs 20 à 26)', () => {
  it('attestations exigées selon l’activité : PCI DSS (carte, délégataire), ISO 27001 et GSMA (mobile), COBAC (fintech)', async () => {
    const s = svc()
    const types = async (id: string) => (await s.decision.approbation(id)).attestations.map((a) => a.type)
    expect(await types('LIA-2026-022')).toEqual(['PCI_DSS'])
    expect(await types('LIA-2026-027')).toEqual(['PCI_DSS'])
    expect(await types('LIA-2025-088')).toEqual(['ISO_27001', 'GSMA'])
    expect(await types('LIA-2026-041')).toEqual(['ISO_27001', 'GSMA', 'COBAC'])
    expect((await s.decision.approbation('LIA-2026-041')).fintech).toBe(true)
    expect(await s.decision.approbation('LIA-2025-088')).toMatchObject({ etat: 'APPROUVEE', ficheChangement: { etatApprobation: 'APPROUVEE', aRelire: false }, actionsPermises: [] })
    // LIA-2026-019 : attestations déjà fournies (à vérifier) et visas optionnels activés, signables une fois l'opération de
    // l'approbation ouverte (archivage des attestations), avant l'approbation.
    expect(await s.decision.approbation('LIA-2026-019')).toMatchObject({ etat: 'DOSSIER_CONSTITUE', ficheChangement: null, actionsPermises: ['VERIFIER_ATTESTATION', 'REFERENCER_FICHE'] })
  })
  it('parcours complet : dossier, fiche lue (jamais refaite), attestations vérifiées puis archivées, approbation', async () => {
    const s = svc()
    const id = 'LIA-2026-027'
    await expect(s.decision.constituerDossierChangement(id, fichier())).rejects.toEqual(refus('CER_PV_NOT_SIGNED'))
    await pvSigne(s)
    expect(await s.decision.approbation(id)).toMatchObject({ etat: 'EN_ATTENTE_DOSSIER', actionsPermises: ['CONSTITUER_DOSSIER'] })
    expect((await s.parcours.tableauActeur(['PRODUCTION_MONETIQUE'])).compteurs.find((c) => c.cle === 'dossiersChangement')?.valeur).toBe(1)
    await expect(s.decision.relireFicheChangement(id)).rejects.toEqual(refus('CER_CHANGE_RECORD_MISSING'))
    await s.decision.constituerDossierChangement(id, fichier('dossier-changement.pdf'))
    expect(await s.decision.approbation(id)).toMatchObject({ etat: 'DOSSIER_CONSTITUE', dossierChangement: { nom: 'dossier-changement.pdf' }, actionsPermises: ['REFERENCER_FICHE'] })
    expect(await etat(s, id, '20')).toBe('FAITE')
    expect([await etat(s, id, '21'), await etat(s, id, '22')]).toEqual(['A_FAIRE', 'A_FAIRE'])
    await expect(s.decision.referencerFicheChangement(id, 'fiche 12')).rejects.toEqual(refus('CER_CHANGE_RECORD_INVALID'))
    await s.decision.referencerFicheChangement(id, 'CHG-2026-0412')
    expect((await s.decision.approbation(id))).toMatchObject({ ficheChangement: { reference: 'CHG-2026-0412', etatApprobation: 'EN_EXAMEN' }, actionsPermises: ['RELIRE_FICHE'] })
    // Attestation : non fournie, puis rejetée (motif exigé), puis fournie à nouveau et vérifiée.
    await expect(s.decision.verifierAttestation(id, 'PCI_DSS', true, '')).rejects.toEqual(refus('CER_ATTESTATION_MISSING'))
    expect((await s.portailParcours.approbation(id))).toMatchObject({ visas: [], actionsPermises: ['DEPOSER_ATTESTATION'] })
    await expect(s.portailParcours.deposerAttestation(id, 'GSMA', fichier())).rejects.toEqual(refus('CER_ATTESTATION_NOT_REQUIRED'))
    await expect(s.portailParcours.deposerAttestation(id, 'PCI_DSS', fichier('attestation.docx'))).rejects.toEqual(refus('CER_ATTESTATION_FORMAT'))
    await s.portailParcours.deposerAttestation(id, 'PCI_DSS', fichier('pci-dss.PDF'))
    expect((await s.decision.approbation(id)).actionsPermises).toEqual(['VERIFIER_ATTESTATION', 'RELIRE_FICHE'])
    expect((await s.parcours.tableauActeur(['RSSI'])).compteurs.find((c) => c.cle === 'attestationsAVerifier')?.valeur).toBe(2)
    await expect(s.decision.verifierAttestation(id, 'PCI_DSS', false, ' ')).rejects.toEqual(refus('CER_REJECTION_REASON_REQUIRED'))
    await s.decision.verifierAttestation(id, 'PCI_DSS', false, 'Certificat expiré')
    expect((await s.decision.approbation(id))).toMatchObject({ etat: 'EN_VERIFICATION', attestations: [{ preuve: null, motifRejet: 'Certificat expiré', verifieeLe: null }] })
    await expect(s.decision.archiverAttestations(id)).rejects.toEqual(refus('CER_ATTESTATIONS_INCOMPLETE'))
    await s.portailParcours.deposerAttestation(id, 'PCI_DSS', fichier('pci-dss-2026.pdf'))
    await s.decision.verifierAttestation(id, 'PCI_DSS', true, '')
    expect((await s.decision.approbation(id))).toMatchObject({ attestations: [{ verifieePar: 'RSSI', verifieeLe: '2026-09-29', motifRejet: null }], actionsPermises: ['ARCHIVER', 'RELIRE_FICHE'] })
    // Relire avant l'archivage : la fiche est relue, rien n'est approuvé.
    await s.decision.relireFicheChangement(id)
    expect((await s.decision.approbation(id))).toMatchObject({ etat: 'EN_VERIFICATION', approuveeLe: null, ficheChangement: { etatApprobation: 'EN_EXAMEN' } })
    await s.decision.archiverAttestations(id)
    expect((await s.decision.approbation(id))).toMatchObject({ etat: 'VERIFIE_ARCHIVE', attestations: [{ archiveeLe: '2026-09-29' }], actionsPermises: ['RELIRE_FICHE'] })
    await s.decision.relireFicheChangement(id)
    expect((await s.decision.approbation(id))).toMatchObject({ etat: 'APPROUVEE', approuveeLe: '2026-09-29', ficheChangement: { etatApprobation: 'APPROUVEE', lueLe: '2026-09-29' } })
    expect((await s.parcours.dossier(id)).statut).toBe('APPROUVEE_SECURITE')
    // L'approbation clôt les rangs 21 à 25 ; l'ouverture de la fiche (rang 26) suit l'approbation (rang 24) et se clôt
    // aussitôt par la fiche déjà référencée et lue (jamais refaite).
    for (const r of ['21', '22', '23', '24', '25', '26']) expect([r, await etat(s, id, r)]).toEqual([r, 'FAITE'])
    expect(await courante(s, id)).toBe('MISE_EN_PRODUCTION')
    expect(await op(s, id, 'D1')).toMatchObject({ etat: 'A_FAIRE', raci: { R: { fonction: 'CONDUCTEUR' } } })
  })
  it('refus d’approbation : liaison inconnue', async () => {
    const s = svc()
    await expect(s.decision.approbation('inconnue')).rejects.toEqual(refus('HOM_LIAISON_NOT_FOUND'))
    await expect(s.decision.referencerFicheChangement('inconnue', 'CHG-2026-1')).rejects.toEqual(refus('HOM_LIAISON_NOT_FOUND'))
  })
})

describe('visas optionnels (D-56 : désactivés par défaut)', () => {
  it('visas actifs : conformité favorable, direction générale défavorable motivée, réauthentification exigée', async () => {
    const s = svc()
    const id = 'LIA-2026-019'
    expect((await s.decision.approbation(id)).visas.map((v) => [v.nature, v.actif, v.etat])).toEqual([['CONFORMITE', true, 'ATTENDU'], ['DIRECTION_GENERALE', true, 'ATTENDU']])
    await expect(s.decision.signerVisa(id, 'CONFORMITE', true, 'Conforme', '')).rejects.toEqual(refus('IAM_REAUTH_REQUIRED'))
    // L'opération de l'approbation (rang 23) s'ouvre à l'archivage des attestations : le visa se signe ensuite.
    await s.decision.verifierAttestation(id, 'PCI_DSS', true, '')
    await s.decision.archiverAttestations(id)
    await s.decision.signerVisa(id, 'CONFORMITE', true, 'Conforme', 'assertion')
    await s.decision.signerVisa(id, 'DIRECTION_GENERALE', false, 'Calendrier incompatible', 'assertion')
    const [c, dg] = (await s.decision.approbation(id)).visas
    expect(c).toMatchObject({ etat: 'SIGNE_FAVORABLE', motif: 'Conforme', signature: { fonction: 'CONFORMITE', signataire: 'Responsable conformité' } })
    expect(dg).toMatchObject({ etat: 'SIGNE_DEFAVORABLE', motif: 'Calendrier incompatible', signature: { fonction: 'DG', signataire: 'Directeur général' } })
    expect((await s.parcours.tableauActeur(['CONFORMITE'])).compteurs.find((x) => x.cle === 'visasAttendus')?.valeur).toBe(0)
    expect((await s.parcours.tableauActeur(['DG'])).compteurs.find((x) => x.cle === 'visasAttendus')?.valeur).toBe(0)
  })
  it('visa non requis quand il n’est pas activé pour la liaison', async () => {
    await expect(svc().decision.signerVisa('LIA-2026-027', 'DIRECTION_GENERALE', true, 'x', 'assertion')).rejects.toEqual(refus('CER_VISA_NOT_REQUIRED'))
  })
})

describe('étape 10 : fiche de suivi des services et constat de mise en service (rangs 32 et 33)', () => {
  it('le participant renseigne la fiche remise, le SNA constate ; couverture de mise en service recalculée', async () => {
    const s = svc()
    const id = 'LIA-2025-088'
    expect(await s.decision.ficheSuivi(id)).toMatchObject({ etat: 'REMISE', actionsPermises: [] })
    await expect(s.decision.constaterMiseEnService(id)).rejects.toEqual(refus('HOM_SERVICE_SHEET_INCOMPLETE'))
    expect(await s.portailParcours.ficheSuivi(id)).toMatchObject({ etat: 'REMISE', actionsPermises: ['RENSEIGNER'], lignes: [{ serviceCode: 'PAIEMENT_MARCHAND', ouvert: true }, { serviceCode: 'REMITTANCE', ouvert: null }, { serviceCode: 'FACTURE', ouvert: null }] })
    await expect(s.portailParcours.renseignerFicheSuivi(id, [{ serviceCode: 'PAIEMENT_MARCHAND', ouvert: true, commentaire: null }])).rejects.toEqual(refus('HOM_SERVICE_SHEET_INCOMPLETE'))
    await s.portailParcours.renseignerFicheSuivi(id, [
      { serviceCode: 'PAIEMENT_MARCHAND', ouvert: true, commentaire: null }, { serviceCode: 'REMITTANCE', ouvert: true, commentaire: 'Ouvert le 20/09' }, { serviceCode: 'FACTURE', ouvert: false, commentaire: 'Report' },
    ])
    await expect(s.portailParcours.renseignerFicheSuivi(id, [])).rejects.toEqual(refus('HOM_SERVICE_SHEET_NOT_OPEN'))
    expect(await s.decision.ficheSuivi(id)).toMatchObject({ etat: 'RENSEIGNEE', renseigneeLe: '2026-09-29', actionsPermises: ['CONSTATER'] })
    expect(await etat(s, id, '32')).toBe('FAITE')
    expect((await s.parcours.tableauActeur(['SNA'])).compteurs.find((c) => c.cle === 'constatsMiseEnService')?.valeur).toBe(2)
    const d = await s.parcours.dossier(id)
    expect(d.perimetre.map((p) => p.etat)).toEqual(['OUVERT', 'OUVERT', 'NON_OUVERT', 'NON_DEMARRE'])
    expect(d.couvertureMiseEnService).toMatchObject({ taux: 67, numerateur: 2, denominateur: 3 })
    expect(d.couvertureCertification.taux).toBe(100)
    await s.decision.constaterMiseEnService(id)
    expect(await s.decision.ficheSuivi(id)).toMatchObject({ etat: 'CONSTATEE', constateeLe: '2026-09-29', actionsPermises: [] })
    expect(await op(s, id, '33')).toMatchObject({ etat: 'FAITE', preuve: { documentRef: 'CST-088' } })
    expect(await etat(s, id, '34')).toBe('A_FAIRE')
  })
  it('fiche à remettre : pas encore ouverte à la saisie du participant', async () => {
    const s = svc()
    expect(await s.decision.ficheSuivi('LIA-2026-030')).toMatchObject({ etat: 'A_REMETTRE', actionsPermises: [] })
    expect((await s.portailParcours.ficheSuivi('LIA-2026-030')).actionsPermises).toEqual([])
    await expect(s.portailParcours.renseignerFicheSuivi('LIA-2026-030', [])).rejects.toEqual(refus('HOM_SERVICE_SHEET_NOT_OPEN'))
    await s.decision.remettreFicheSuivi('LIA-2026-030')
    expect((await s.portailParcours.ficheSuivi('LIA-2026-030')).actionsPermises).toEqual(['RENSEIGNER'])
    expect((await s.parcours.tableauActeur(['PARTICIPANT'])).compteurs.find((c) => c.cle === 'fichesSuivi')?.valeur).toBe(2)
  })
})

describe('portail du participant (qualité Externe)', () => {
  it('ses liaisons seulement ; projection du dossier sans donnée interne ; liaison d’un autre participant introuvable', async () => {
    const s = svc()
    expect((await s.portailParcours.mesLiaisons()).map((l) => l.id)).toEqual(['LIA-2026-030', 'LIA-2025-088', 'LIA-2026-046', 'LIA-2026-027'])
    const d = await s.portailParcours.dossier('LIA-2026-030')
    expect(d).toMatchObject({ conducteur: { nom: 'Conducteur de projet GIMAC' }, blocages: [{ responsable: { nom: 'POLE_RESEAU' } }], actionsPermises: [] })
    for (const appel of [
      () => s.portailParcours.dossier('LIA-2026-022'), () => s.portailParcours.procesVerbal('LIA-2026-022'), () => s.portailParcours.approbation('LIA-2026-022'),
      () => s.portailParcours.ficheSuivi('LIA-2026-022'), () => s.portailParcours.signerProcesVerbal('LIA-2026-022', 'a'),
      () => s.portailParcours.deposerAttestation('LIA-2026-022', 'PCI_DSS', fichier()), () => s.portailParcours.communiquerCoordonnees('LIA-2026-022', { adresse: 'h', port: '80' }),
      () => s.portailParcours.deposerLivrable('LIA-2026-022:17', fichier(), 'x'),
    ]) await expect(appel()).rejects.toEqual(refus('HOM_LIAISON_NOT_FOUND'))
    const n = await s.parcours.ouvrirLiaison({ institutionId: 'INST-0999', typeProjet: 'CARTE_INTERBANCAIRE', chefDeProjetId: 'U-COND-1' })
    expect(await s.portailParcours.dossier(n.id).catch((e: { code: string }) => e.code)).toBe('HOM_LIAISON_NOT_FOUND')
  })
  it('tableau du participant : tâches et blocages de sa seule institution, liens vers le portail', async () => {
    const s = svc()
    const t = await s.portailParcours.mesTaches()
    expect(t.fonction).toBe('PARTICIPANT')
    expect(t.liaisons.map((l) => l.id)).toEqual(['LIA-2026-030', 'LIA-2025-088', 'LIA-2026-046', 'LIA-2026-027'])
    expect(t.taches.map((x) => [x.operationId, x.qualite])).toEqual([['LIA-2025-088:32', 'R'], ['LIA-2026-027:19', 'E'], ['LIA-2026-046:12', 'R']])
    // Chaque tâche mène à l'écran du portail où le geste s'accomplit.
    expect(t.taches.map((x) => x.lien)).toEqual(['/portail/liaisons/LIA-2025-088/fiche-suivi', '/portail/liaisons/LIA-2026-027/proces-verbal', '/portail/liaisons/LIA-2026-046/coordonnees'])
    expect(t.blocages.map((b) => b.id)).toEqual(['BLQ-0007'])
    expect(Object.fromEntries(t.compteurs.map((c) => [c.cle, c.valeur]))).toEqual({ tachesARealiser: 3, pvASigner: 1, attestationsAFournir: 0, fichesSuivi: 1, tachesEnRetard: 3, echeancesProches: 0 })
    // Un blocage d'un autre participant n'apparaît pas.
    await s.parcours.declarerBlocage('LIA-2026-022', { motif: 'x', prochaineAction: 'x', responsableId: 'U-SNA-1', echeanceLevee: '2026-10-01' })
    expect((await s.portailParcours.mesTaches()).blocages.map((b) => b.liaisonId)).toEqual(['LIA-2026-030'])
    // Procès-verbal signé : l'attestation exigée devient une pièce à fournir.
    await pvSigne(s)
    expect((await s.portailParcours.mesTaches()).compteurs.find((c) => c.cle === 'attestationsAFournir')?.valeur).toBe(1)
  })
})

describe('parcours de bout en bout (étapes 3 à 10)', () => {
  it('nouvelle liaison mobile : cadrage, lancement, ateliers, coordonnées du participant, intégration, essais, constat ; verrou 1 à 100 %', async () => {
    const s = svc()
    const l = await s.parcours.ouvrirLiaison({ institutionId: 'INST-0042', typeProjet: 'PAIEMENT_MOBILE', chefDeProjetId: 'U-COND-1' })
    const id = l.id
    expect(await courante(s, id)).toBe('CADRAGE')
    // L'ouverture engage le cadrage sans le clore (six éléments, D-59) : le rang 3 est à faire.
    expect((await op(s, id, '3')).etat).toBe('A_FAIRE')
    await clore(s, await op(s, id, '3'))
    await clore(s, await op(s, id, '4'))
    expect(await courante(s, id)).toBe('LANCEMENT')
    await s.parcours.affecterRessources(id, [{ fonction: 'SNA', personneId: 'U-SNA-2' }, { fonction: 'POLE_RESEAU', personneId: 'U-RES-1' }])
    await s.parcours.deposerLivrable(`${id}:7`, fichier('cr-lancement.pdf'), 'Réunion de lancement tenue')
    // HOM-05 : visa du conducteur configuré au référentiel sur le compte rendu, avant la clôture.
    await s.parcours.signerOperation(id, '7', 'VISA_CONDUITE', 'assertion')
    expect(await courante(s, id)).toBe('ATELIERS')
    // Le participant dépose le compte rendu d'un atelier depuis le portail.
    await s.portailParcours.deposerLivrable(`${id}:8`, fichier('cr-atelier.pdf'), 'Atelier fonctionnel')
    expect(await executer(s, id, ['12'])).toEqual(['9', '10', '11'])
    expect(await courante(s, id)).toBe('INTEGRATION')
    // Rang 12 : coordonnées d'essai communiquées par le participant, contrôlées puis relisibles.
    await expect(s.portailParcours.communiquerCoordonnees(id, { adresse: 'hôte invalide!', port: '8443' })).rejects.toEqual(refus('HOM_TECHNICAL_DATA_INVALID'))
    await expect(s.portailParcours.communiquerCoordonnees(id, { adresse: 'essai.exemple', port: 'x' })).rejects.toEqual(refus('HOM_TECHNICAL_DATA_INVALID'))
    await s.portailParcours.communiquerCoordonnees(id, { adresse: 'essai.exemple', port: '8443' })
    expect((await op(s, id, '12')).preuve?.valeurReference).toEqual({ libelle: 'parcours.valeur.12', valeur: 'essai.exemple:8443' })
    await expect(s.portailParcours.communiquerCoordonnees(id, { adresse: 'essai.exemple', port: '8443' })).rejects.toEqual(refus('HOM_OPERATION_NOT_OPEN'))
    // Rangs 13 à 16, dont le document de paramétrage (grain de phase, livrable unique).
    expect((await op(s, id, '15'))).toMatchObject({ etat: 'VERROUILLEE', actionsPermises: ['ATTRIBUER'] })
    expect(await executer(s, id, ['17'])).toEqual(['13', '14', '15', '16'])
    expect((await op(s, id, '15')).preuve?.nom).toBe('piece.pdf')
    expect(await courante(s, id)).toBe('CERTIFICATION')
    for (const e of ['CADRAGE', 'LANCEMENT', 'ATELIERS', 'INTEGRATION', 'TESTS'] as const) {
      expect((await s.parcours.dossier(id)).etapes.find((x) => x.code === e)).toMatchObject({ etat: 'CLOSE', elements: { RESPONSABLE: true, ACTIONS: true, LIVRABLES: true, VALIDATION: true, PREUVE: true, DATE: true } })
    }
    await s.parcours.constaterCouverture(id)
    // Aucun service certifié sur cette liaison neuve : le verrou 1 tient.
    expect((await s.decision.procesVerbal(id)).actionsPermises).toEqual(['ETABLIR_BLOQUE'])
    await expect(s.decision.etablirProcesVerbal(id)).rejects.toEqual(refus('CER_COVERAGE_INCOMPLETE'))
  })
  it('liaison délégataire certifiée : approbation, branche D1 à D34, tronc aval jusqu’au suivi, toutes les étapes closes', async () => {
    const s = svc()
    const id = 'LIA-2026-027'
    await pvSigne(s)
    await s.decision.constituerDossierChangement(id, fichier())
    await s.portailParcours.deposerAttestation(id, 'PCI_DSS', fichier())
    await s.decision.verifierAttestation(id, 'PCI_DSS', true, '')
    await s.decision.archiverAttestations(id)
    await s.decision.referencerFicheChangement(id, 'CHG-2026-0027')
    await s.decision.relireFicheChangement(id)
    // Branche délégataire (34 opérations) puis tronc aval jusqu'à la fiche de suivi (rang 32).
    const clos = await executer(s, id, ['32'])
    expect(clos.filter((r) => r.startsWith('D'))).toHaveLength(34)
    expect(clos.slice(-5)).toEqual(['27', '28', '29', '30', '31'])
    expect(await courante(s, id)).toBe('SUIVI')
    await s.decision.remettreFicheSuivi(id)
    const lignes = (await s.portailParcours.ficheSuivi(id)).lignes.map((x) => ({ serviceCode: x.serviceCode, ouvert: true, commentaire: null }))
    await s.portailParcours.renseignerFicheSuivi(id, lignes)
    await s.decision.constaterMiseEnService(id)
    expect(await executer(s, id)).toEqual(['34', '35'])
    const d = await s.parcours.dossier(id)
    expect(d.etapes.every((e) => e.etat === 'CLOSE')).toBe(true)
    expect(d.etapeCourante).toBe('SUIVI')
    expect(d.couvertureMiseEnService.taux).toBe(100)
    expect(d.perimetre.filter((p) => p.inclus).every((p) => p.etat === 'OUVERT')).toBe(true)
    expect((await s.parcours.listerLiaisons({ page: 1, taille: 50 })).contenu.find((x) => x.id === id)).toMatchObject({ tachesEnRetard: 0, prochaineEcheance: null })
  })
})
