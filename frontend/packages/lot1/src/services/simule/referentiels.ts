import type { ApiError } from '@gimac/socle'
import type { TypeProjet } from '../../modele/commun'
import type {
  AnomalieGraphe, CatalogueModelisateur, ControleResultat, Geste, GrapheReferentiel, Jalon, Livrable, NoeudGraphe, Particularite, ReferenceOperation,
  ReferentielVersion, SaisieOperation, TypeAnomalie,
} from '../../modele/homologation'
import { ETAPES, type EtapeCode, type Fonction } from '../../modele/parcours'
import { referentielDe, type OperationReferentiel } from '../../modele/referentielCR'
import type { ServiceHomologation } from '../ports'
import { LIBELLES_CR } from './libellesCR'

// Serveur de substitution du modélisateur de processus (D-77, docs/lot1/modelisateur-processus.md) : version 1
// fidèle à l'annexe A du CR-CERT-GIMAC-01 v1.4 (même conversion que ConversionAnnexeA de MS-HOMOLOGATION),
// graphe étendu (couches de Kahn, dates au plus tôt, chemin critique, anomalies) et écritures du brouillon avec
// les codes d'erreur du service. Ces règles sont celles du serveur simulé ; l'interface n'en porte aucune.

const EXTERNES: Fonction[] = ['PARTICIPANT', 'BANQUE_PARTENAIRE', 'PERSONNALISATEUR']
const PDF = ['application/pdf']
export const DECLENCHEURS: Record<string, string> = {
  'certification.pv-established.v1': 'Procès-verbal de certification établi',
  'certification.pv-signed.v1': 'Procès-verbal signé des deux parties',
  'certification.change-record-constituted.v1': 'Dossier de changement constitué',
  'certification.attestations-archived.v1': 'Attestations vérifiées et archivées',
  'certification.security-approved.v1': 'Changement approuvé par la sécurité',
  'certification.change-record-referenced.v1': 'Fiche de changement référencée',
}
const JALONS: Jalon[] = ['APTE_CERTIFICATION', 'CERTIFIEE', 'APPROUVEE_SECURITE', 'EN_PRODUCTION']
const GESTES_DEDIES: Geste[] = ['AFFECTER_RESSOURCES', 'EVENEMENT_EXTERNE', 'FICHE_SUIVI', 'AUCUN']
const SEGMENTS = ['AMONT', 'BRANCHE', 'AVAL'] as const

/** Geste des rangs autrefois codés en dur (version 1, §1). */
export function gesteDe(o: Pick<OperationReferentiel, 'rang' | 'grain'>): Geste {
  if (o.rang === '6') return 'AFFECTER_RESSOURCES'
  if (o.rang === '12') return 'COORDONNEES_TECHNIQUES'
  if (o.rang === '17') return 'CONSTATER_COUVERTURE'
  if (/^(1[89]|2[0-5])$/.test(o.rang)) return 'EVENEMENT_EXTERNE'
  if (o.rang === '32' || o.rang === '33') return 'FICHE_SUIVI'
  if (/^([7-9]|1[01])$/.test(o.rang)) return 'DEPOSER_LIVRABLE'
  return o.grain === 'PHASE' ? 'DEPOSER_LIVRABLE' : 'DEPOSER_PREUVE'
}
/** Geste réalisé par sa route propre : aucune pièce libre (Geste.dedie de MS-HOMOLOGATION). */
export const estDedie = (g: Geste | null) => !!g && GESTES_DEDIES.includes(g)
/** Action proposée sur une opération ouverte : constat, livrable, sinon preuve (Geste.action). */
export const actionDuGeste = (g: Geste | null) => (g === 'CONSTATER_COUVERTURE' ? 'CONSTATER_COUVERTURE' : g === 'DEPOSER_LIVRABLE' ? 'DEPOSER_LIVRABLE' : 'DEPOSER_PREUVE')
const jalonDe = (rang: string): Jalon | null => ({ 17: 'APTE_CERTIFICATION', 19: 'CERTIFIEE', 25: 'APPROUVEE_SECURITE', 31: 'EN_PRODUCTION' } as Record<string, Jalon>)[rang] ?? null
const declencheurDe = (rang: string): string | null =>
  ({ 18: 'certification.pv-established.v1', 19: 'certification.pv-signed.v1', 20: 'certification.change-record-constituted.v1', 21: 'certification.attestations-archived.v1',
    22: 'certification.attestations-archived.v1', 23: 'certification.security-approved.v1', 24: 'certification.security-approved.v1', 25: 'certification.security-approved.v1',
    26: 'certification.change-record-referenced.v1' } as Record<string, string>)[rang] ?? null
const particulariteDe = (rang: string): Particularite | null => ({ 15: 'ENTREE_ESSAIS', 16: 'FIN_ESSAIS', C1: 'CEREMONIE_CLES', 35: 'SERVICE_SUPPLEMENTAIRE' } as Record<string, Particularite>)[rang] ?? null

/** Version 1 d'un type de projet, convertie de l'annexe A (rangs 3 et suivants, branche du type, tronc aval). */
export function versionCR(type: TypeProjet): ReferenceOperation[] {
  const suite = referentielDe(type).filter((o) => o.etape !== 'PRISE_CONTACT' && o.etape !== 'ADHESION')
  const codes = new Set(suite.map((o) => o.rang))
  const finBranche = suite.filter((o) => o.segment === 'BRANCHE').at(-1)?.rang
  const lignes = new Map<EtapeCode, number>()
  return suite.map((o, i): ReferenceOperation => {
    const [libelle, preuve, valeur] = LIBELLES_CR[o.rang] ?? [o.rang, '', '']
    const r = o.porteurs[0]
    const autres = o.porteurs.slice(1)
    const ligne = lignes.get(o.etape) ?? 0
    lignes.set(o.etape, ligne + 1)
    const predecesseurs = o.rang === '27' && finBranche ? [finBranche] : o.predecesseurs.filter((p) => codes.has(p))
    const livrables: Livrable[] = o.grain === 'PHASE' || o.pivot ? [{ code: 'L1', libelle: preuve || libelle, nature: 'LIVRABLE', deposant: r, typesPieces: PDF, obligatoire: true }] : []
    return {
      code: o.rang, libelle, description: preuve ? `Preuve attendue : ${preuve}.` : null, etape: o.etape, segment: o.segment, ordre: i + 1,
      responsable: r, validateur: r === 'CONDUCTEUR' ? 'CHEF_DES' : 'CONDUCTEUR', consultes: autres.filter((f) => !EXTERNES.includes(f)), informes: ['CHEF_DDRM'],
      externes: autres.filter((f) => EXTERNES.includes(f)), predecesseurs, delaiJours: 3, grain: o.grain,
      plateforme: o.rang === '15' || o.rang === '16' ? (type === 'PAIEMENT_MOBILE' ? 'SIMULATEUR' : null) : o.plateforme,
      cle: !!o.cle, secret: !!o.secret, oubliSansSymptome: !!o.oubliSansSymptome, pivot: !!o.pivot, valeurReference: o.valeurReference ? valeur || 'Valeur de référence' : null,
      livrables, geste: gesteDe(o), jalon: jalonDe(o.rang), declencheur: declencheurDe(o.rang), particularite: particulariteDe(o.rang),
      position: { x: ETAPES.indexOf(o.etape) * 300, y: ligne * 120 + SEGMENTS.indexOf(o.segment) * 40 },
      phase: null, partie: EXTERNES.includes(r) ? 'PARTICIPANT' : 'DES', serviceCode: 'TRANSVERSE', natureCouverture: 'AUCUNE', preuveAttendue: {}, idScenario: null,
      fonctionResponsable: r, grille: null,
    }
  })
}

const parType = new Map<TypeProjet, ReferenceOperation[]>()
/** Codes de la version 1 simulée d'un type clos par un sujet d'événement (déclencheur lu sur l'opération, §3). */
export function codesDeclenches(type: TypeProjet, sujet: string): string[] {
  if (!parType.has(type)) parType.set(type, versionCR(type))
  return (parType.get(type) ?? []).filter((o) => o.declencheur === sujet).map((o) => o.code)
}

/** Catalogue du modélisateur (GET /referentiels/catalogue), aux libellés français du service. */
export const CATALOGUE: CatalogueModelisateur = {
  fonctions: ([
    ['DDRM', 'Développement du Réseau et du Marché', ['ROLE_CHARGE_DDRM']], ['CHEF_DDRM', 'Chef du département DDRM', ['ROLE_CHEF_DDRM']],
    ['CHEF_DES', 'Chef du département DES', ['ROLE_CHEF_DES']], ['CHEFS_DEPARTEMENT', 'Chefs des deux départements', ['ROLE_CHEF_DES']],
    ['CONDUCTEUR', 'Conducteur de projet', ['ROLE_CONDUCTEUR_PROJET']], ['SNA', 'Normes et Agréments', ['ROLE_SNA', 'ROLE_INGENIEUR_HOMOLOGATION']],
    ['PRODUCTION_MONETIQUE', 'Production monétique', ['ROLE_PRODUCTION_MONETIQUE']], ['POLE_RESEAU', 'Pôle réseau', ['ROLE_POLE_RESEAU']],
    ['POLE_SYSTEME', 'Pôle système', ['ROLE_POLE_SYSTEME']], ['RSSI', 'Sécurité des systèmes d\'information', ['ROLE_RSSI']], ['CONFORMITE', 'Conformité', ['ROLE_CONFORMITE']],
    ['DG', 'Direction générale', ['ROLE_DG']], ['EXPLOITATION', 'Exploitation et Supervision', ['ROLE_PRODUCTION_MONETIQUE']], ['PARTICIPANT', 'Participant', ['ROLE_CORRESPONDANT']],
    ['BANQUE_PARTENAIRE', 'Banque partenaire', []], ['PERSONNALISATEUR', 'Personnalisateur', []],
  ] as const).map(([code, libelle, profils]) => ({ code, libelle, profils: [...profils], externe: EXTERNES.includes(code) })),
  gestes: ([
    ['DEPOSER_LIVRABLE', 'Déposer un livrable'], ['DEPOSER_PREUVE', 'Déposer une preuve'], ['CONSTATER_COUVERTURE', 'Constater la couverture du périmètre'],
    ['AFFECTER_RESSOURCES', 'Affecter les intervenants'], ['COORDONNEES_TECHNIQUES', 'Communiquer les coordonnées techniques d\'essai'], ['FICHE_SUIVI', 'Fiche de suivi des services'],
    ['EVENEMENT_EXTERNE', 'Événement d\'un autre service'], ['AUCUN', 'Aucun geste : point de passage'],
  ] as const).map(([code, libelle]) => ({ code, libelle, dedie: GESTES_DEDIES.includes(code) })),
  jalons: [['APTE_CERTIFICATION', 'Apte à la certification'], ['CERTIFIEE', 'Certifiée'], ['APPROUVEE_SECURITE', 'Approuvée par la sécurité'], ['EN_PRODUCTION', 'En production']]
    .map(([code, libelle], i) => ({ code, libelle, rang: i + 1 })),
  declencheurs: Object.entries(DECLENCHEURS).map(([code, libelle]) => ({ code, libelle })),
  etapes: ([
    ['PRISE_CONTACT', 'Prise de contact', 1], ['ADHESION', 'Adhésion', 2], ['CADRAGE', 'Cadrage', 3], ['LANCEMENT', 'Lancement', 4], ['ATELIERS', 'Ateliers', 5],
    ['INTEGRATION', 'Intégration', 6], ['TESTS', 'Tests', 7], ['CERTIFICATION', 'Certification', 8], ['APPROBATION_SECURITE', 'Approbation de la sécurité', undefined],
    ['MISE_EN_PRODUCTION', 'Mise en production', 9], ['SUIVI', 'Suivi post-production', 10],
  ] as const).map(([code, libelle, rang]) => (rang ? { code, libelle, rang } : { code, libelle })),
  segments: [{ code: 'AMONT', libelle: 'Tronc commun amont' }, { code: 'BRANCHE', libelle: 'Branche du type de projet' }, { code: 'AVAL', libelle: 'Tronc commun aval' }],
  plateformes: [{ code: 'SWITCH_INTERBANCAIRE', libelle: 'Switch interbancaire' }, { code: 'SWITCH_DELEGATAIRE', libelle: 'Switch délégataire' },
    { code: 'PLATEFORME_MOBILE', libelle: 'Plateforme de paiement mobile' }, { code: 'SIMULATEUR', libelle: 'Simulateur' }],
  grains: [{ code: 'PHASE', libelle: 'Pièce de phase' }, { code: 'OPERATION', libelle: 'Preuve par opération' }],
  naturesLivrable: [{ code: 'LIVRABLE', libelle: 'Livrable' }, { code: 'PREUVE', libelle: 'Preuve' }],
  particularites: [{ code: 'ENTREE_ESSAIS', libelle: 'Entrée en essai des services' }, { code: 'FIN_ESSAIS', libelle: 'Fin des essais et certification des services' },
    { code: 'CEREMONIE_CLES', libelle: 'Cérémonie de clés en trois composantes' }, { code: 'SERVICE_SUPPLEMENTAIRE', libelle: 'Ouverture d\'un service supplémentaire' }],
  typesPieces: ['application/pdf', 'image/png', 'image/jpeg', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'text/csv', 'application/zip'],
  pilotes: [{ id: 'U-101', nom: 'Ingénieur d\'homologation A' }, { id: 'U-102', nom: 'Ingénieur d\'homologation B' }],
}

type Rapport = { anomalies: AnomalieGraphe[]; ordre: string[]; couches: Map<string, number>; cycle: string[] }

/** Contrôle du graphe (ReferentialGraphValidator) : Kahn, cycle, prédécesseurs, rôles, jalons, livrables, secrets. */
export function controlerGraphe(ops: ReferenceOperation[]): Rapport {
  const anomalies: AnomalieGraphe[] = []
  const ajouter = (type: TypeAnomalie, codes: string[], motif: string) => anomalies.push({ type, codes, motif })
  const parCode = new Map(ops.map((o) => [o.code, o]))
  const vus = new Set<string>()
  ops.forEach((o) => {
    if (vus.has(o.code)) ajouter('CODE_DUPLIQUE', [o.code], `Code ${o.code} employé plusieurs fois.`)
    vus.add(o.code)
  })
  const preds = (o: ReferenceOperation) => o.predecesseurs.filter((p) => parCode.has(p))
  ops.forEach((o) => {
    o.predecesseurs.filter((p) => !parCode.has(p)).forEach((p) => ajouter('PREDECESSEUR_INCONNU', [o.code, p], `Prédécesseur ${p} inconnu.`))
    preds(o).forEach((p) => {
      const sp = SEGMENTS.indexOf(parCode.get(p)!.segment ?? 'AMONT')
      if (o.segment && sp > SEGMENTS.indexOf(o.segment)) ajouter('SEGMENT_INCOHERENT', [o.code, p], `${p} appartient à un segment postérieur à celui de ${o.code}.`)
    })
    if (!o.responsable) ajouter('RESPONSABLE_ABSENT', [o.code], `${o.code} n'a pas de responsable (R).`)
    if (!o.validateur) ajouter('VALIDATEUR_ABSENT', [o.code], `${o.code} n'a pas de validateur (V).`)
    else if (o.validateur === o.responsable) ajouter('VALIDATEUR_EGAL_RESPONSABLE', [o.code], `Le validateur de ${o.code} est aussi son responsable.`)
    if (o.geste === 'EVENEMENT_EXTERNE' && !(o.declencheur && DECLENCHEURS[o.declencheur])) ajouter('DECLENCHEUR_INCONNU', [o.code], `${o.code} attend un événement inconnu.`)
    if (o.secret && o.valeurReference) ajouter('SECRET_AVEC_VALEUR', [o.code], `${o.code} est secrète : aucune valeur de référence n'est admise.`)
    const lv = o.livrables.map((l) => l.code)
    if (new Set(lv).size !== lv.length) ajouter('LIVRABLE_DUPLIQUE', [o.code], `${o.code} a deux livrables au même code.`)
  })
  // Kahn : ordre topologique et couche (plus long chemin depuis une entrée).
  const degre = new Map(ops.map((o) => [o.code, preds(o).length]))
  const couches = new Map<string, number>()
  const file = ops.filter((o) => degre.get(o.code) === 0).map((o) => o.code)
  file.forEach((c) => couches.set(c, 0))
  if (ops.length && !file.length) ajouter('SANS_ENTREE', [], 'Aucune opération sans prédécesseur.')
  const ordre: string[] = []
  while (file.length) {
    const c = file.shift()!
    ordre.push(c)
    ops.filter((o) => preds(o).includes(c)).forEach((s) => {
      couches.set(s.code, Math.max(couches.get(s.code) ?? 0, (couches.get(c) ?? 0) + 1))
      degre.set(s.code, degre.get(s.code)! - 1)
      if (degre.get(s.code) === 0) file.push(s.code)
    })
  }
  let cycle: string[] = []
  if (ordre.length < ops.length) {
    const reste = ops.filter((o) => !ordre.includes(o.code))
    // Suite du cycle : en remontant les prédécesseurs restants depuis la première opération bloquée.
    const chemin: string[] = []
    let c: string | undefined = reste[0].code
    while (c && !chemin.includes(c)) {
      chemin.push(c)
      c = preds(parCode.get(c)!).find((p) => !ordre.includes(p))
    }
    cycle = c ? chemin.slice(chemin.indexOf(c)).reverse() : chemin
    ajouter('CYCLE', cycle, `Cycle : ${cycle.join(' → ')}.`)
  }
  // Jalons : au plus une fois chacun, jamais un jalon antérieur après un jalon postérieur dans le DAG.
  JALONS.forEach((j) => {
    const porteurs = ops.filter((o) => o.jalon === j).map((o) => o.code)
    if (porteurs.length > 1) ajouter('JALON_DUPLIQUE', porteurs, `Jalon ${j} porté par ${porteurs.join(', ')}.`)
  })
  const ancetres = (code: string, acc = new Set<string>()): Set<string> => {
    preds(parCode.get(code)!).forEach((p) => {
      if (!acc.has(p)) {
        acc.add(p)
        ancetres(p, acc)
      }
    })
    return acc
  }
  if (!cycle.length) {
    ops.filter((o) => o.jalon).forEach((o) => {
      ancetres(o.code).forEach((a) => {
        const ja = parCode.get(a)!.jalon
        if (ja && JALONS.indexOf(ja) > JALONS.indexOf(o.jalon!)) ajouter('JALON_DESORDONNE', [a, o.code], `Le jalon ${ja} de ${a} précède le jalon ${o.jalon} de ${o.code}.`)
      })
    })
  }
  return { anomalies, ordre, couches, cycle }
}

/** Attributs requis à la publication (ReferentialCompletenessChecker). */
function manquants(ops: ReferenceOperation[]) {
  return ops
    .map((o) => ({ code: o.code, attributs: (['libelle', 'etape', 'segment', 'responsable', 'validateur', 'geste'] as const).filter((a) => !o[a]) as string[] }))
    .filter((m) => m.attributs.length)
}

/** Successeurs déduits des prédécesseurs. */
const avecSuccesseurs = (ops: ReferenceOperation[]) => ops.map((o) => ({ ...o, successeurs: ops.filter((x) => x.predecesseurs.includes(o.code)).map((x) => x.code).sort() }))

/** Graphe étendu (GET /referentiels/{id}/graphe). */
export function calculerGraphe(v: ReferentielVersion, ops: ReferenceOperation[]): GrapheReferentiel {
  const r = controlerGraphe(ops)
  const parCode = new Map(ops.map((o) => [o.code, o]))
  const debut = new Map<string, number>()
  const fin = new Map<string, number>()
  r.ordre.forEach((c) => {
    const o = parCode.get(c)!
    const d = Math.max(0, ...o.predecesseurs.filter((p) => fin.has(p)).map((p) => fin.get(p)!))
    debut.set(c, d)
    fin.set(c, d + (o.delaiJours ?? 0))
  })
  // Chemin critique : depuis la fin la plus tardive, en remontant le prédécesseur qui la fixe.
  const chemin: string[] = []
  let c: string | undefined = [...fin.entries()].sort((a, b) => b[1] - a[1] || r.ordre.indexOf(a[0]) - r.ordre.indexOf(b[0]))[0]?.[0]
  while (c) {
    chemin.unshift(c)
    const d: number = debut.get(c) ?? 0
    c = parCode.get(c)!.predecesseurs.find((p: string): boolean => fin.get(p) === d && d > 0)
  }
  const critiques = new Set(chemin)
  const arcsCritiques = new Set(chemin.slice(1).map((x, i) => `${chemin[i]}>${x}`))
  const noeuds: NoeudGraphe[] = avecSuccesseurs(ops)
    .sort((a, b) => (a.ordre ?? 0) - (b.ordre ?? 0) || a.code.localeCompare(b.code))
    .map((o) => ({
      ...o, couche: r.couches.get(o.code) ?? null, debutAuPlusTot: debut.get(o.code) ?? null, finAuPlusTot: fin.get(o.code) ?? null, critique: critiques.has(o.code),
      anomalies: [...new Set(r.anomalies.filter((a) => a.codes.includes(o.code)).map((a) => a.type))],
    }))
  return {
    versionId: v.id, typeProjet: v.typeProjet, numero: v.numero, etat: v.etat, piloteId: v.piloteId ?? null, noeuds,
    arcs: ops.flatMap((o) => o.predecesseurs.filter((p) => parCode.has(p)).map((p) => ({ de: p, vers: o.code, critique: arcsCritiques.has(`${p}>${o.code}`) }))),
    anomalies: r.anomalies, manquants: manquants(ops), cheminCritique: { codes: chemin, dureeJours: Math.max(0, ...fin.values()) }, actionsPermises: v.actionsPermises,
  }
}

/** Restitution du contrôle à blanc (ControleVue). */
export function controleDe(ops: ReferenceOperation[]): ControleResultat {
  const r = controlerGraphe(ops)
  return {
    acyclique: !r.cycle.length, ordreTopologique: r.ordre, cycle: r.cycle,
    incoherences: r.anomalies.filter((a) => a.type !== 'CYCLE').map((a) => ({ code: a.codes[0] ?? '', predecesseur: a.codes[1] ?? '', motif: a.motif })),
    manquants: manquants(ops),
  }
}

// ------------------------------------------------------------------------------------------------ jeu et état

export function versionsInitiales(): { versions: ReferentielVersion[]; operations: Record<string, ReferenceOperation[]> } {
  const carte = versionCR('CARTE_INTERBANCAIRE')
  // Brouillon 4 : C9 retirée (C10 rattachée à C2), C11 ajoutée sans validateur (anomalie montrée au modélisateur).
  const brouillon = carte
    .filter((o) => o.code !== 'C9')
    .map((o) => (o.code === 'C10' ? { ...o, predecesseurs: ['C2'] } : o.code === '27' ? { ...o, predecesseurs: ['C11'] } : o))
  const c10 = carte.find((o) => o.code === 'C10')!
  brouillon.splice(brouillon.findIndex((o) => o.code === 'C10') + 1, 0, {
    ...c10, code: 'C11', libelle: 'Contrôle croisé de la table des BIN après chargement', predecesseurs: ['C8', 'C10'], validateur: null, ordre: c10.ordre! + 1,
    position: { x: c10.position!.x + 150, y: c10.position!.y + 40 }, description: null,
  })
  const v = (id: string, typeProjet: TypeProjet, numero: number | null, libelle: string, etat: ReferentielVersion['etat'], dateCreation: string, datePublication: string | null,
    versionSourceId: string | null, versionActiveId: string | null, actionsPermises: string[], piloteId: string | null = null): ReferentielVersion => ({
    id, typeProjet, numero, libelle, etat, dateCreation, datePublication, versionSourceId, versionActiveId, transitionsPermises: etat === 'BROUILLON' ? ['PUBLIEE'] : [],
    actionsPermises, piloteId,
  })
  const versions = [
    v('REF-CI-3', 'CARTE_INTERBANCAIRE', 3, 'Carte interbancaire, version 3', 'PUBLIEE', '02/06/2026 09:10', '15/06/2026 16:40', 'REF-CI-2', 'REF-CI-3', ['DERIVER', 'CONTROLER']),
    v('REF-CI-4', 'CARTE_INTERBANCAIRE', null, 'Carte interbancaire, brouillon 4', 'BROUILLON', '22/09/2026 11:02', null, 'REF-CI-3', 'REF-CI-3', ['CONTROLER', 'PUBLIER', 'MODIFIER', 'MIGRER'], 'U-101'),
    v('REF-CI-2', 'CARTE_INTERBANCAIRE', 2, 'Carte interbancaire, version 2', 'RETIREE', '10/01/2026 08:30', '20/01/2026 10:00', null, 'REF-CI-3', []),
    v('REF-PM-2', 'PAIEMENT_MOBILE', 2, 'Paiement mobile, version 2', 'PUBLIEE', '04/05/2026 14:00', '12/05/2026 09:15', null, 'REF-PM-2', ['DERIVER', 'CONTROLER']),
    v('REF-DL-1', 'DELEGATAIRE', null, 'Délégataire, reprise de l\'annexe A', 'BROUILLON', '01/10/2026 08:00', null, null, null, ['CONTROLER', 'PUBLIER', 'MODIFIER'], 'U-102'),
  ]
  return {
    versions,
    operations: {
      'REF-CI-3': carte, 'REF-CI-4': brouillon, 'REF-CI-2': carte.filter((o) => o.code !== '35'),
      'REF-PM-2': versionCR('PAIEMENT_MOBILE'), 'REF-DL-1': versionCR('DELEGATAIRE'),
    },
  }
}

type Refus = (status: number, code: string, message: string) => ApiError
type Attendre = <T>(v: T | (() => T)) => Promise<T>
const CODE = /^[A-Z0-9][A-Z0-9_-]*$/

/** Port des référentiels simulé, état propre à l'instance (versions, opérations, positions). */
export function referentielsSimules(attendre: Attendre, refus: Refus, o: { utilisateur?: string; maintenant: () => string; liaisonsActives: (versionId: string) => number }) {
  const { versions, operations } = versionsInitiales()
  let numero = 0
  const nouvelId = () => `REF-${Date.now().toString(36).toUpperCase()}-${++numero}`
  const version = (id: string) => {
    const v = versions.find((x) => x.id === id)
    if (!v) throw refus(404, 'HOM_REFERENTIAL_NOT_FOUND', 'Version introuvable.')
    return v
  }
  const brouillon = (id: string) => {
    const v = version(id)
    if (v.etat !== 'BROUILLON') throw refus(409, 'HOM_REFERENTIAL_FROZEN', 'Une version publiée est figée.')
    return operations[id] ?? (operations[id] = [])
  }
  const trouver = (ops: ReferenceOperation[], code: string) => {
    const op = ops.find((x) => x.code === code)
    if (!op) throw refus(404, 'HOM_OPERATION_NOT_FOUND', `Opération ${code} introuvable.`)
    return op
  }
  const valider = (code: string, s: SaisieOperation) => {
    if (s.secret && s.valeurReference) throw refus(422, 'HOM_OPERATION_SECRET_VALUE', 'Une opération secrète ne relève aucune valeur de référence.')
    if (s.predecesseurs.includes(code)) throw refus(422, 'HOM_OPERATION_SELF_PREDECESSOR', 'Une opération ne peut pas se précéder elle-même.')
  }
  const vers = (code: string, s: SaisieOperation, avant?: ReferenceOperation): ReferenceOperation => ({
    phase: null, partie: null, serviceCode: null, natureCouverture: null, idScenario: null, fonctionResponsable: null, preuveAttendue: null, grille: null,
    ...avant, ...s, code, position: s.position === undefined ? avant?.position ?? null : s.position,
    particularite: s.particularite === undefined ? avant?.particularite ?? null : s.particularite,
  })
  const avecSucc = (ops: ReferenceOperation[], code: string) => avecSuccesseurs(ops).find((x) => x.code === code)!
  /** L'arc de → vers fermerait un cycle si « de » descend déjà de « vers ». */
  const descend = (ops: ReferenceOperation[], depuis: string, cible: string): boolean => {
    const vus = new Set<string>()
    const pile = [depuis]
    while (pile.length) {
      const c = pile.pop()!
      if (c === cible) return true
      if (vus.has(c)) continue
      vus.add(c)
      ops.filter((x) => x.predecesseurs.includes(c)).forEach((x) => pile.push(x.code))
    }
    return false
  }

  const port: Pick<ServiceHomologation,
    'listerReferentiels' | 'lireReferentiel' | 'operationsReferentiel' | 'controlerReferentiel' | 'creerBrouillon' | 'deriverBrouillon' | 'publierReferentiel'
    | 'grapheReferentiel' | 'catalogueReferentiels' | 'creerOperation' | 'enregistrerOperation' | 'supprimerOperation' | 'placerOperations' | 'arcReferentiel' | 'designerPilote'
    | 'liaisonsImpactees'> = {
    listerReferentiels: () => attendre(() => versions.map((v) => ({ ...v, nombreOperations: (operations[v.id] ?? []).length }))),
    lireReferentiel: (id) => attendre(() => version(id)),
    operationsReferentiel: (id) => attendre(() => avecSuccesseurs(operations[id] ?? [])),
    controlerReferentiel: (id) => attendre(() => controleDe(operations[id] ?? [])),
    liaisonsImpactees: (versionId) => attendre({ total: o.liaisonsActives(versionId) }),
    creerBrouillon: (e) => attendre(() => {
      const piloteId = e.piloteId ?? o.utilisateur ?? 'U-101'
      if (!CATALOGUE.pilotes.some((p) => p.id === piloteId) && e.piloteId) throw refus(422, 'HOM_PILOTE_NOT_ELIGIBLE', 'Le pilote désigné ne peut pas modifier les référentiels.')
      const active = versions.find((x) => x.typeProjet === e.typeProjet && x.etat === 'PUBLIEE')
      const v: ReferentielVersion = {
        id: nouvelId(), typeProjet: e.typeProjet as TypeProjet, numero: null, libelle: e.libelle, etat: 'BROUILLON', dateCreation: o.maintenant(),
        datePublication: null, versionSourceId: null, versionActiveId: active?.id ?? null, transitionsPermises: ['PUBLIEE'], actionsPermises: ['CONTROLER', 'PUBLIER', 'MODIFIER'], piloteId,
      }
      versions.push(v)
      operations[v.id] = []
      return v
    }),
    deriverBrouillon: (source) => attendre(() => {
      const s = version(source)
      const v: ReferentielVersion = {
        ...s, id: nouvelId(), numero: null, etat: 'BROUILLON', libelle: `${s.libelle}, brouillon`, versionSourceId: s.id, datePublication: null,
        dateCreation: o.maintenant(), transitionsPermises: ['PUBLIEE'], actionsPermises: ['CONTROLER', 'PUBLIER', 'MODIFIER'], piloteId: o.utilisateur ?? 'U-101',
        versionActiveId: versions.find((x) => x.typeProjet === s.typeProjet && x.etat === 'PUBLIEE')?.id ?? null,
      }
      versions.push(v)
      operations[v.id] = structuredClone(operations[s.id] ?? [])
      return v
    }),
    publierReferentiel: (id) => attendre(() => {
      const v = version(id)
      if (v.etat !== 'BROUILLON') throw refus(409, 'HOM_REFERENTIAL_FROZEN', 'Une version publiée est figée.')
      const r = controlerGraphe(operations[id] ?? [])
      if (r.cycle.length) throw refus(409, 'HOM_REFERENTIAL_GRAPH_INVALID', 'Le graphe contient un cycle.')
      versions.filter((x) => x.typeProjet === v.typeProjet && x.etat === 'PUBLIEE').forEach((x) => {
        x.etat = 'RETIREE'
        x.actionsPermises = []
      })
      v.etat = 'PUBLIEE'
      v.numero = Math.max(0, ...versions.filter((x) => x.typeProjet === v.typeProjet).map((x) => x.numero ?? 0)) + 1
      v.datePublication = o.maintenant()
      v.transitionsPermises = []
      v.actionsPermises = ['DERIVER', 'CONTROLER']
      versions.filter((x) => x.typeProjet === v.typeProjet).forEach((x) => (x.versionActiveId = v.id))
    }),
    grapheReferentiel: (id) => attendre(() => calculerGraphe(version(id), operations[id] ?? [])),
    catalogueReferentiels: () => attendre(CATALOGUE),
    creerOperation: (id, s) => attendre(() => {
      const ops = brouillon(id)
      if (!CODE.test(s.code) || s.code.length > 40) throw refus(422, 'HOM_OPERATION_CODE_INVALID', 'Code d\'opération invalide.')
      if (ops.some((x) => x.code === s.code)) throw refus(409, 'HOM_OPERATION_CODE_DUPLICATE', `Le code ${s.code} existe déjà dans cette version.`)
      valider(s.code, s)
      const { code, ...saisie } = s
      ops.push(vers(code, { ...saisie, ordre: saisie.ordre ?? ops.length + 1 }))
      return avecSucc(ops, code)
    }),
    enregistrerOperation: (id, code, s) => attendre(() => {
      const ops = brouillon(id)
      const i = ops.indexOf(trouver(ops, code))
      valider(code, s)
      ops[i] = vers(code, s, ops[i])
      return avecSucc(ops, code)
    }),
    supprimerOperation: (id, code) => attendre(() => {
      const ops = brouillon(id)
      trouver(ops, code)
      operations[id] = ops.filter((x) => x.code !== code).map((x) => ({ ...x, predecesseurs: x.predecesseurs.filter((p) => p !== code) }))
    }),
    placerOperations: (id, positions) => attendre(() => {
      const ops = brouillon(id)
      positions.forEach((p) => {
        const op = ops.find((x) => x.code === p.code)
        if (op) op.position = { x: p.x, y: p.y }
      })
    }),
    arcReferentiel: (id, a) => attendre(() => {
      const ops = brouillon(id)
      trouver(ops, a.de)
      const cible = trouver(ops, a.vers)
      if (a.de === a.vers) throw refus(422, 'HOM_OPERATION_SELF_PREDECESSOR', 'Une opération ne peut pas se précéder elle-même.')
      if (a.present && !cible.predecesseurs.includes(a.de)) {
        if (descend(ops, a.vers, a.de)) throw refus(409, 'HOM_REFERENTIAL_CYCLE', `L'arc ${a.de} → ${a.vers} fermerait un cycle.`)
        cible.predecesseurs = [...cible.predecesseurs, a.de]
      }
      if (!a.present) cible.predecesseurs = cible.predecesseurs.filter((p) => p !== a.de)
      return avecSucc(ops, a.vers)
    }),
    designerPilote: (id, compteId) => attendre(() => {
      brouillon(id)
      if (!CATALOGUE.pilotes.some((p) => p.id === compteId)) throw refus(422, 'HOM_PILOTE_NOT_ELIGIBLE', 'Le pilote désigné ne peut pas modifier les référentiels.')
      const v = version(id)
      v.piloteId = compteId
      return v
    }),
  }
  return { port, versions }
}
