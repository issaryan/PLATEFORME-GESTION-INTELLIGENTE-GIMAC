import type { EtapeCode, Fonction, GrainPreuve, PlateformeExecution, SegmentParcours } from './parcours'
import type { TypeProjet } from './commun'

// Référentiel des étapes et des preuves du compte rendu de cadrage (CR-CERT-GIMAC-01 v1.4, annexe A),
// jeu d'initialisation des référentiels (D-59). Tronc commun amont (rangs 1 à 26), branche du type de
// projet (C1 à C10, M1 à M17, D1 à D34), tronc commun aval (rangs 27 à 35). Aucune valeur secrète :
// les opérations sur des clés ne portent que la trace et le code de contrôle.

export type OperationReferentiel = {
  rang: string
  /** Clé i18n du libellé : `parcours.op.<rang>`. */
  etape: EtapeCode
  segment: SegmentParcours
  /** Fonctions qui réalisent l'opération ; la première est le responsable principal (R). */
  porteurs: Fonction[]
  /** Types de projet concernés (tronc commun : colonnes C, M, D). */
  types: TypeProjet[]
  plateforme: PlateformeExecution | null
  grain: GrainPreuve
  /** Opération sur un élément cryptographique : seule la trace et le code de contrôle sont admis. */
  cle?: boolean
  /** Élément secret non cryptographique (mot de passe, identifiants) : seule la trace, hors valeurs, est admise. */
  secret?: boolean
  /** Preuve portant une valeur de référence relisible et comparable (point de terminaison, code membre, BIN). */
  valeurReference?: boolean
  /** Opération parmi les plus faciles à omettre, sans symptôme à la bascule (CR §5.5). */
  oubliSansSymptome?: boolean
  /** Pièce pivot : sa réception déclenche le raccordement de production (CR §4.4.2). */
  pivot?: boolean
  predecesseurs: string[]
}

const TOUS: TypeProjet[] = ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE']
const CARTE: TypeProjet[] = ['CARTE_INTERBANCAIRE']
const MOBILE: TypeProjet[] = ['PAIEMENT_MOBILE']
const DELEG: TypeProjet[] = ['DELEGATAIRE']

const op = (
  rang: string,
  etape: EtapeCode,
  segment: SegmentParcours,
  porteurs: Fonction[],
  predecesseurs: string[],
  o: Partial<Pick<OperationReferentiel, 'types' | 'plateforme' | 'grain' | 'cle' | 'secret' | 'valeurReference' | 'oubliSansSymptome' | 'pivot'>> = {},
): OperationReferentiel => ({ rang, etape, segment, porteurs, predecesseurs, types: o.types ?? TOUS, plateforme: o.plateforme ?? null, grain: o.grain ?? 'OPERATION', ...o })

/** A.2 à A.4 : tronc commun amont, rangs 1 à 26. */
export const TRONC_AMONT: OperationReferentiel[] = [
  op('1', 'PRISE_CONTACT', 'AMONT', ['DDRM'], []),
  op('2', 'ADHESION', 'AMONT', ['DDRM'], ['1']),
  op('3', 'CADRAGE', 'AMONT', ['DDRM', 'PARTICIPANT'], ['2']),
  op('4', 'CADRAGE', 'AMONT', ['DDRM', 'PARTICIPANT'], ['3']),
  op('5', 'CADRAGE', 'AMONT', ['DDRM', 'PARTICIPANT'], ['3'], { types: ['CARTE_INTERBANCAIRE', 'DELEGATAIRE'] }),
  op('6', 'LANCEMENT', 'AMONT', ['CHEFS_DEPARTEMENT'], ['3']),
  op('7', 'LANCEMENT', 'AMONT', ['DDRM', 'CHEF_DES'], ['6']),
  op('8', 'ATELIERS', 'AMONT', ['DDRM', 'PARTICIPANT'], ['7']),
  op('9', 'ATELIERS', 'AMONT', ['POLE_RESEAU', 'PARTICIPANT'], ['7']),
  op('10', 'ATELIERS', 'AMONT', ['SNA', 'PARTICIPANT'], ['7']),
  op('11', 'ATELIERS', 'AMONT', ['EXPLOITATION', 'PARTICIPANT'], ['7']),
  op('12', 'INTEGRATION', 'AMONT', ['PARTICIPANT'], ['9'], { valeurReference: true }),
  op('13', 'INTEGRATION', 'AMONT', ['POLE_RESEAU'], ['12']),
  op('14', 'INTEGRATION', 'AMONT', ['POLE_RESEAU'], ['13']),
  op('15', 'INTEGRATION', 'AMONT', ['SNA', 'PARTICIPANT'], ['14'], { grain: 'PHASE' }),
  op('16', 'TESTS', 'AMONT', ['SNA', 'PARTICIPANT'], ['15']),
  op('17', 'CERTIFICATION', 'AMONT', ['SNA'], ['16']),
  op('18', 'CERTIFICATION', 'AMONT', ['CONDUCTEUR'], ['17']),
  op('19', 'CERTIFICATION', 'AMONT', ['CHEF_DES', 'PARTICIPANT'], ['18'], { pivot: true }),
  op('20', 'APPROBATION_SECURITE', 'AMONT', ['PRODUCTION_MONETIQUE'], ['19']),
  op('21', 'APPROBATION_SECURITE', 'AMONT', ['PARTICIPANT'], ['20']),
  op('22', 'APPROBATION_SECURITE', 'AMONT', ['PARTICIPANT'], ['20']),
  op('23', 'APPROBATION_SECURITE', 'AMONT', ['RSSI'], ['21', '22']),
  op('24', 'APPROBATION_SECURITE', 'AMONT', ['RSSI'], ['23']),
  op('25', 'APPROBATION_SECURITE', 'AMONT', ['PRODUCTION_MONETIQUE'], ['24']),
  op('26', 'APPROBATION_SECURITE', 'AMONT', ['PRODUCTION_MONETIQUE'], ['24']),
]

/** A.5 : branche carte interbancaire, dix opérations sur le switch interbancaire. */
export const BRANCHE_CARTE: OperationReferentiel[] = [
  op('C1', 'MISE_EN_PRODUCTION', 'BRANCHE', ['RSSI'], ['24'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE', cle: true }),
  op('C2', 'MISE_EN_PRODUCTION', 'BRANCHE', ['PRODUCTION_MONETIQUE'], ['26'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE', valeurReference: true }),
  op('C3', 'MISE_EN_PRODUCTION', 'BRANCHE', ['PRODUCTION_MONETIQUE'], ['C2'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE', valeurReference: true }),
  op('C4', 'MISE_EN_PRODUCTION', 'BRANCHE', ['PRODUCTION_MONETIQUE'], ['C2'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE' }),
  op('C5', 'MISE_EN_PRODUCTION', 'BRANCHE', ['PRODUCTION_MONETIQUE'], ['C3', 'C4'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE' }),
  op('C6', 'MISE_EN_PRODUCTION', 'BRANCHE', ['PRODUCTION_MONETIQUE'], ['C4'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE' }),
  op('C7', 'MISE_EN_PRODUCTION', 'BRANCHE', ['PRODUCTION_MONETIQUE'], ['C1', 'C5', 'C6'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE' }),
  op('C8', 'MISE_EN_PRODUCTION', 'BRANCHE', ['PRODUCTION_MONETIQUE'], ['C7'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE' }),
  op('C9', 'MISE_EN_PRODUCTION', 'BRANCHE', ['PRODUCTION_MONETIQUE'], ['C2'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE' }),
  op('C10', 'MISE_EN_PRODUCTION', 'BRANCHE', ['PRODUCTION_MONETIQUE'], ['C9'], { types: CARTE, plateforme: 'SWITCH_INTERBANCAIRE' }),
]

/** A.6 : branche mobile, dix-sept opérations sur la plateforme de paiement mobile (D-59 : 17, non 21). */
const M = (rang: string, porteur: Fonction, pred: string[], o: Partial<OperationReferentiel> = {}) =>
  op(rang, 'MISE_EN_PRODUCTION', 'BRANCHE', [porteur], pred, { types: MOBILE, plateforme: 'PLATEFORME_MOBILE', ...o })
export const BRANCHE_MOBILE: OperationReferentiel[] = [
  M('M1', 'PRODUCTION_MONETIQUE', ['26']),
  M('M2', 'PRODUCTION_MONETIQUE', ['26'], { valeurReference: true }),
  M('M3', 'PRODUCTION_MONETIQUE', ['M2'], { secret: true }),
  M('M4', 'PRODUCTION_MONETIQUE', ['M2']),
  M('M5', 'PRODUCTION_MONETIQUE', ['M4']),
  M('M6', 'POLE_RESEAU', ['M2']),
  M('M7', 'PRODUCTION_MONETIQUE', ['M3', 'M5'], { secret: true }),
  M('M8', 'PRODUCTION_MONETIQUE', ['M5'], { valeurReference: true }),
  M('M9', 'PRODUCTION_MONETIQUE', ['M8'], { secret: true }),
  M('M10', 'PRODUCTION_MONETIQUE', ['M8']),
  M('M11', 'POLE_RESEAU', ['M6', 'M10']),
  M('M12', 'POLE_RESEAU', ['M11']),
  M('M13', 'POLE_RESEAU', ['M12']),
  M('M14', 'POLE_RESEAU', ['M13']),
  M('M15', 'PRODUCTION_MONETIQUE', ['M13', 'M14'], { valeurReference: true }),
  M('M16', 'POLE_RESEAU', ['M15']),
  M('M17', 'POLE_RESEAU', ['M16']),
]

/** A.7 : branche délégataire, trente-quatre opérations sur le switch délégataire puis interbancaire. */
const D = (rang: string, porteurs: Fonction[], pred: string[], plateforme: PlateformeExecution | null = 'SWITCH_DELEGATAIRE', o: Partial<OperationReferentiel> = {}) =>
  op(rang, 'MISE_EN_PRODUCTION', 'BRANCHE', porteurs, pred, { types: DELEG, plateforme, ...o })
export const BRANCHE_DELEGATAIRE: OperationReferentiel[] = [
  D('D1', ['CONDUCTEUR', 'PERSONNALISATEUR'], ['26'], null),
  D('D2', ['PERSONNALISATEUR', 'RSSI'], ['D1'], null, { secret: true }),
  D('D3', ['RSSI'], ['D2'], null, { cle: true }),
  D('D4', ['POLE_RESEAU'], ['D1'], null),
  D('D5', ['PARTICIPANT'], ['D1'], null),
  D('D6', ['PARTICIPANT'], ['D1'], null),
  D('D7', ['PRODUCTION_MONETIQUE'], ['D1'], 'SWITCH_DELEGATAIRE', { valeurReference: true }),
  D('D8', ['PRODUCTION_MONETIQUE'], ['D7']),
  D('D9', ['PRODUCTION_MONETIQUE'], ['D8']),
  D('D10', ['PRODUCTION_MONETIQUE'], ['D7']),
  D('D11', ['PRODUCTION_MONETIQUE'], ['D10']),
  D('D12', ['PRODUCTION_MONETIQUE'], ['D3', 'D11'], 'SWITCH_DELEGATAIRE', { cle: true }),
  D('D13', ['PRODUCTION_MONETIQUE'], ['D7']),
  D('D14', ['PRODUCTION_MONETIQUE'], ['D7']),
  D('D15', ['PRODUCTION_MONETIQUE'], ['D5', 'D12', 'D14']),
  D('D16', ['PRODUCTION_MONETIQUE'], ['D15']),
  D('D17', ['PRODUCTION_MONETIQUE'], ['D16']),
  D('D18', ['PRODUCTION_MONETIQUE'], ['D7']),
  D('D19', ['PRODUCTION_MONETIQUE'], ['D18']),
  D('D20', ['POLE_RESEAU'], ['D4']),
  D('D21', ['PRODUCTION_MONETIQUE'], ['D20']),
  D('D22', ['PRODUCTION_MONETIQUE'], ['D21']),
  D('D23', ['PRODUCTION_MONETIQUE'], ['D6', 'D22']),
  D('D24', ['PRODUCTION_MONETIQUE'], ['D7'], 'SWITCH_DELEGATAIRE', { valeurReference: true }),
  D('D25', ['PRODUCTION_MONETIQUE'], ['D24']),
  D('D26', ['PRODUCTION_MONETIQUE'], ['D23']),
  D('D27', ['PRODUCTION_MONETIQUE'], ['D17'], 'SWITCH_DELEGATAIRE', { cle: true }),
  D('D28', ['PRODUCTION_MONETIQUE', 'PARTICIPANT'], ['D27']),
  D('D29', ['PRODUCTION_MONETIQUE'], ['D25'], 'SWITCH_INTERBANCAIRE', { valeurReference: true }),
  D('D30', ['PRODUCTION_MONETIQUE'], ['D29'], 'SWITCH_INTERBANCAIRE'),
  D('D31', ['PRODUCTION_MONETIQUE'], ['D29'], 'SWITCH_INTERBANCAIRE', { valeurReference: true }),
  D('D32', ['PRODUCTION_MONETIQUE'], ['D31'], 'SWITCH_INTERBANCAIRE'),
  D('D33', ['PRODUCTION_MONETIQUE'], ['D29'], 'SWITCH_INTERBANCAIRE'),
  D('D34', ['PRODUCTION_MONETIQUE'], ['D14'], 'SWITCH_DELEGATAIRE'),
]

/** A.8 et A.9 : tronc commun aval, rangs 27 à 35. */
export const TRONC_AVAL: OperationReferentiel[] = [
  op('27', 'MISE_EN_PRODUCTION', 'AVAL', ['POLE_SYSTEME'], []),
  op('28', 'MISE_EN_PRODUCTION', 'AVAL', ['POLE_SYSTEME'], ['27'], { oubliSansSymptome: true }),
  op('29', 'MISE_EN_PRODUCTION', 'AVAL', ['POLE_SYSTEME'], ['27'], { secret: true }),
  op('30', 'MISE_EN_PRODUCTION', 'AVAL', ['PRODUCTION_MONETIQUE', 'PARTICIPANT'], ['28', '29']),
  op('31', 'MISE_EN_PRODUCTION', 'AVAL', ['PRODUCTION_MONETIQUE'], ['30']),
  op('32', 'SUIVI', 'AVAL', ['PARTICIPANT'], ['31']),
  op('33', 'SUIVI', 'AVAL', ['SNA'], ['32']),
  op('34', 'SUIVI', 'AVAL', ['EXPLOITATION'], ['33']),
  op('35', 'SUIVI', 'AVAL', ['DDRM'], ['34']),
]

const BRANCHES: Record<string, OperationReferentiel[]> = {
  CARTE_INTERBANCAIRE: BRANCHE_CARTE,
  PAIEMENT_MOBILE: BRANCHE_MOBILE,
  DELEGATAIRE: BRANCHE_DELEGATAIRE,
}

/** Suite d'opérations d'une liaison : tronc amont, sa seule branche, tronc aval (CR §5.2.3). */
export function referentielDe(type: TypeProjet): OperationReferentiel[] {
  // Type créé (D-91, modèle GENERIQUE) : tronc commun seul, sans branche.
  const repris = type in BRANCHES
  const branche = BRANCHES[type] ?? []
  const amont = TRONC_AMONT.filter((o) => (repris ? o.types.includes(type) : o.types.length === TOUS.length))
  const avantAval = branche.at(-1)?.rang ?? amont.at(-1)?.rang
  const aval = TRONC_AVAL.map((o) => (o.rang === '27' && avantAval ? { ...o, predecesseurs: [avantAval] } : o))
  return [...amont, ...branche, ...aval]
}
