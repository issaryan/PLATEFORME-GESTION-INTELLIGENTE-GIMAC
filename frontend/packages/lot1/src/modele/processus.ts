import type { PersonneActivite } from './pilotage'

// Processus d'unité (D-87, docs/lot1/processus-unite.md) : définitions versionnées (étapes, intervenants, livrables,
// signatures, champs) et instances, servies par MS-PILOTAGE (/bff/des/pilotage/processus/**). L'interface n'en calcule
// aucune règle : contrôle, droits (droits, moi), résolution des intervenants, signatures et transitions sont décidés
// par le service ; les formes ci-dessous sont celles du §5.1.

export const ETATS_PROCESSUS = ['BROUILLON', 'PUBLIEE', 'RETIREE'] as const
export type EtatProcessus = (typeof ETATS_PROCESSUS)[number]

export const MODES_INTERVENANT = ['PROFIL', 'PERSONNE', 'AU_LANCEMENT', 'CHEF_UNITE', 'LANCEUR', 'N_PLUS_1_LANCEUR'] as const
export type ModeIntervenant = (typeof MODES_INTERVENANT)[number]
/** Modes qui exigent un profil de remplacement (§1). */
export const MODES_REMPLACEMENT: readonly ModeIntervenant[] = ['PERSONNE', 'AU_LANCEMENT']

export type Intervenant = {
  mode: ModeIntervenant
  profil?: string | null
  personneId?: string | null
  unite?: string | null
  remplacement?: string | null
}

export const TYPES_CHAMP = ['TEXTE', 'NOMBRE', 'DATE', 'CHOIX'] as const
export type TypeChamp = (typeof TYPES_CHAMP)[number]

export type LivrableProcessus = { code: string; libelle: string; typesPieces: string[]; obligatoire: boolean; modele: string | null }
export type SignatureProcessus = { code: string; libelle: string; signataire: Intervenant; porte: string[]; ordre: number }
export type ChampProcessus = { code: string; libelle: string; type: TypeChamp; choix: string[]; obligatoire: boolean }

export type EtapeProcessus = {
  code: string
  libelle: string
  action: string
  intervenant: Intervenant
  validateur: Intervenant | null
  predecesseurs: string[]
  delaiJours: number
  livrables: LivrableProcessus[]
  signatures: SignatureProcessus[]
  champs: ChampProcessus[]
  position: { x: number; y: number } | null
}

export type DroitsProcessus = { modifier: boolean; publier: boolean; lancer: boolean; retirer: boolean }

/** Résumé d'une version (GET /processus). */
export type ResumeProcessus = {
  id: string
  code: string
  version: number
  etat: EtatProcessus
  unite: string
  libelle: string
  description: string
  categorie: string
  delaiCibleJours: number | null
  nombreEtapes: number
  publieeLe: string | null
  modifieLe: string | null
  modifiePar: PersonneActivite | null
  droits: DroitsProcessus
}

export type GraviteAnomalie = 'BLOQUANTE' | 'LEVABLE' | 'AVERTISSEMENT'
export type AnomalieProcessus = { code: string; gravite: GraviteAnomalie; etape: string | null; element: string | null; message: string }
export type HistoriqueProcessus = { quand: string; acteur: PersonneActivite | null; action: string; etape: string | null; detail: string | null }

/** Définition rendue (GET /processus/{id} et réponses d'écriture). */
export type Processus = {
  id: string
  code: string
  version: number
  etat: EtatProcessus
  unite: string
  libelle: string
  description: string
  categorie: string
  delaiCibleJours: number | null
  etapes: EtapeProcessus[]
  creeLe: string | null
  creePar: PersonneActivite | null
  modifieLe: string | null
  modifiePar: PersonneActivite | null
  publieeLe: string | null
  publieePar: PersonneActivite | null
  motifPublication: string | null
  avertissementsLeves: boolean
  retireeLe: string | null
  retireePar: PersonneActivite | null
  motifRetrait: string | null
  droits: DroitsProcessus
  versions: { id: string; version: number; etat: EtatProcessus }[]
  avertissements: AnomalieProcessus[]
  personnes: Record<string, string>
  historique: HistoriqueProcessus[]
}

/** POST /processus et PUT /processus/{id} (code et unité immuables ensuite). */
export type SaisieProcessus = {
  code: string
  unite: string
  libelle: string
  description: string
  categorie: string
  delaiCibleJours: number | null
  etapes: EtapeProcessus[]
}

export type ControleProcessus = { publiable: boolean; anomalies: AnomalieProcessus[] }

// ------------------------------------------------------------------ Instances (§2, §5.1)

export const ETATS_INSTANCE = ['EN_COURS', 'SUSPENDUE', 'CLOSE', 'ANNULEE'] as const
export type EtatInstance = (typeof ETATS_INSTANCE)[number]
export const ETATS_ETAPE = ['EN_ATTENTE', 'ACTIVE', 'A_SIGNER', 'A_VALIDER', 'TERMINEE', 'ANNULEE'] as const
export type EtatEtape = (typeof ETATS_ETAPE)[number]
export type VueInstances = 'miennes' | 'unite' | 'lancees'

export type DepotLivrable = { documentId: string; nomFichier: string; typeMime: string; taille: number; sha256: string; deposePar: PersonneActivite | null; deposeLe: string }
export type Attendu = { personne: PersonneActivite | null; profil: string | null; unite: string | null }

export type EtapeInstance = {
  code: string
  libelle: string
  action: string
  predecesseurs: string[]
  position: { x: number; y: number } | null
  delaiJours: number
  etat: EtatEtape
  activeeLe: string | null
  echeance: string | null
  soumiseLe: string | null
  termineeLe: string | null
  enRetard: boolean
  renvois: number
  tacheId: string | null
  executant: PersonneActivite | null
  aPrendre: { unite: string; profil: string | null } | null
  validateur: Attendu | null
  livrables: (Omit<LivrableProcessus, 'modele'> & { modele: string | null; depot: DepotLivrable | null })[]
  champs: (ChampProcessus & { valeur: string | number | null })[]
  signatures: {
    code: string
    libelle: string
    ordre: number
    porte: string[]
    attendu: Attendu
    etat: 'ATTENDUE' | 'SIGNEE'
    signePar: PersonneActivite | null
    signeLe: string | null
    empreinte: string | null
    documents: { livrable: string; documentId: string; sha256: string }[]
  }[]
  moi: { executer: boolean; prendre: boolean; signer: string[]; valider: boolean; reaffecter: boolean }
}

export type InstanceProcessus = {
  id: string
  processus: { id: string; code: string; version: number; libelle: string }
  unite: string
  intitule: string
  etat: EtatInstance
  lanceur: PersonneActivite
  lanceeLe: string
  closeLe: string | null
  motif: string | null
  designations: Record<string, PersonneActivite>
  avancement: { total: number; terminees: number }
  etapes: EtapeInstance[]
  historique: HistoriqueProcessus[]
  droits: { suspendre: boolean; reprendre: boolean; annuler: boolean; reaffecter: boolean }
}

export type ResumeInstance = {
  id: string
  processus: { id: string; code: string; version: number; libelle: string }
  unite: string
  intitule: string
  etat: EtatInstance
  lanceur: PersonneActivite
  lanceeLe: string
  closeLe: string | null
  avancement: { total: number; terminees: number }
  etapesEnCours: { code: string; libelle: string; etat: EtatEtape; echeance: string | null }[]
}

export type PageInstances = { contenu: ResumeInstance[]; total: number; page: number; taille: number }

/** POST /processus/{id}/instances : clés des désignations CODE_ETAPE, CODE_ETAPE.validateur, CODE_ETAPE.signature.CODE. */
export type Lancement = { intitule: string; champs: Record<string, string | number | null>; designations: Record<string, string> }

export type DecisionValidation = 'VALIDER' | 'RENVOYER'

/** Bornes du contrat (§5.1), annoncées avant l'envoi ; le service refait tout. */
export const LIMITES_PROCESSUS = {
  code: /^[A-Z][A-Z0-9_-]{1,39}$/,
  codeEtape: /^[A-Z0-9][A-Z0-9_-]{0,39}$/,
  libelle: 200,
  description: 4000,
  categorie: 80,
  delai: 365,
  etapes: 100,
  elements: 30,
  typesPieces: 20,
  choix: 50,
  ordre: 99,
  motif: 1000,
  texte: 4000,
} as const

/** Clés des désignations d'une étape au lancement (§5.1). */
export const cleDesignation = {
  executant: (etape: string) => etape,
  validateur: (etape: string) => `${etape}.validateur`,
  signature: (etape: string, signature: string) => `${etape}.signature.${signature}`,
}

/** Désignations attendues au lancement : chaque intervenant AU_LANCEMENT de la définition. */
export function designationsAttendues(etapes: EtapeProcessus[]): { cle: string; etape: EtapeProcessus; role: 'executant' | 'validateur' | 'signature'; signature?: SignatureProcessus; intervenant: Intervenant }[] {
  return etapes.flatMap((e) => [
    ...(e.intervenant.mode === 'AU_LANCEMENT' ? [{ cle: cleDesignation.executant(e.code), etape: e, role: 'executant' as const, intervenant: e.intervenant }] : []),
    ...(e.validateur?.mode === 'AU_LANCEMENT' ? [{ cle: cleDesignation.validateur(e.code), etape: e, role: 'validateur' as const, intervenant: e.validateur }] : []),
    ...e.signatures.filter((s) => s.signataire.mode === 'AU_LANCEMENT').map((s) => ({ cle: cleDesignation.signature(e.code, s.code), etape: e, role: 'signature' as const, signature: s, intervenant: s.signataire })),
  ])
}

/** Champs saisis au lancement : ceux des étapes initiales (sans prédécesseur). */
export const champsDeLancement = (etapes: EtapeProcessus[]) => etapes.filter((e) => e.predecesseurs.length === 0).flatMap((e) => e.champs)

/** Étape vierge d'un brouillon. */
export const etapeVierge = (n: number, position: { x: number; y: number } | null = null): EtapeProcessus => ({
  code: `ETAPE_${n}`, libelle: '', action: '', intervenant: { mode: 'LANCEUR' }, validateur: null, predecesseurs: [], delaiJours: 1, livrables: [], signatures: [], champs: [], position,
})

/** Intervenant réduit aux attributs de son mode (le service ne garde qu'eux). */
export function intervenantNormalise(i: Intervenant): Intervenant {
  const base: Intervenant = { mode: i.mode }
  if (i.mode === 'PROFIL') return { ...base, profil: i.profil ?? null, unite: i.unite || null }
  if (i.mode === 'PERSONNE') return { ...base, personneId: i.personneId ?? null, remplacement: i.remplacement ?? null }
  if (i.mode === 'AU_LANCEMENT') return { ...base, remplacement: i.remplacement ?? null, unite: i.unite || null }
  if (i.mode === 'CHEF_UNITE') return { ...base, unite: i.unite || null }
  return base
}
