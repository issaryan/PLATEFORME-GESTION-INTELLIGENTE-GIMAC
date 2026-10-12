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

/**
 * `modifierPublic` (D-103) : régler le public d'un brouillon ou d'une version publiée sans nouvelle version (qui définit
 * les processus de l'unité) ; absent d'un service antérieur (repli : `modifier`).
 */
export type DroitsProcessus = { modifier: boolean; publier: boolean; lancer: boolean; retirer: boolean; modifierPublic?: boolean }

// ------------------------------------------------------------------ Lancement (D-98, docs/lot1/lancement-processus.md §1, §2)

/**
 * Public d'une version publiée (D-98 §1, D-100) : qui peut la voir dans « Lancer une demande » et la lancer, règles
 * cumulables (l'une suffit), en données dans la définition et choisies par son créateur. Elles font seules foi : aucun droit
 * d'administration (REF_METIER_ADMIN, PROCESSUS_LANCER) ne s'y ajoute.
 */
export const REGLES_INITIATEUR = ['MEMBRES_UNITE', 'TOUT_LE_GIMAC', 'UNITES', 'PROFILS', 'PERMISSIONS', 'PERSONNES'] as const
export type RegleInitiateur = (typeof REGLES_INITIATEUR)[number]
/** Personne désignée nommément, avec son nom affiché servi par MS-PILOTAGE (null s'il est inconnu de l'annuaire). */
export type PersonneDesignee = { id: string; nomAffiche: string | null }
/**
 * `personnes` (D-100) : identifiants des comptes désignés nommément (règle PERSONNES, 50 au plus) ; `personnesDetail` :
 * les mêmes avec leur nom, rendus par le service (absents d'un service antérieur). `initiateurIntervenant` (D-99,
 * séparation des fonctions) : vrai si l'initiateur peut aussi exécuter, valider ou signer les étapes de sa propre demande ;
 * absent ou faux par défaut.
 */
export type Initiateurs = {
  regles: RegleInitiateur[]; unites: string[]; profils: string[]; permissions: string[]; personnes?: string[]; personnesDetail?: PersonneDesignee[]; initiateurIntervenant?: boolean
}
/** Défaut (et reprise des définitions existantes) : les membres de l'unité propriétaire et ses responsables. */
export const INITIATEURS_DEFAUT: Initiateurs = { regles: ['MEMBRES_UNITE'], unites: [], profils: [], permissions: [], personnes: [] }
/**
 * Bornes du public (§10) : personnes désignées nommément ; motif d'un changement de public d'une version publiée (D-103,
 * §11) : 10 caractères au moins dans l'interface (comme les autres fenêtres motivées), 500 au plus (le service admet 3).
 */
export const LIMITES_PUBLIC = { personnes: 50, motifMin: 10, motifMax: 500 } as const
/**
 * PUT /processus/{id}/public (D-103) : règles et paramètres du public, sans `initiateurIntervenant` (la séparation des
 * fonctions reste celle de la définition) ; motif obligatoire sur une version publiée, facultatif sur un brouillon.
 */
export type SaisiePublic = { initiateurs: Pick<Initiateurs, 'regles' | 'unites' | 'profils' | 'permissions' | 'personnes'>; motif?: string }

/** Types des champs du formulaire de lancement. */
export const TYPES_CHAMP_FORMULAIRE = ['TEXTE', 'TEXTE_LONG', 'NOMBRE', 'DATE', 'PERIODE', 'LISTE', 'CASE', 'PIECE_JOINTE'] as const
export type TypeChampFormulaire = (typeof TYPES_CHAMP_FORMULAIRE)[number]
export type ChampFormulaire = { code: string; libelle: string; type: TypeChampFormulaire; choix: string[]; obligatoire: boolean; aide: string | null; typesPieces: string[] }
export type Formulaire = { champs: ChampFormulaire[] }
/** Pièce jointe déposée par le dépôt fragmenté (finalité PROCESSUS_UNITE, référence : la définition lancée). */
export type PieceJointe = { documentId: string; nomFichier: string; typeMime: string; taille: number; sha256: string }
export type Periode = { debut: string; fin: string }
/** Valeur d'un champ : texte (texte, date AAAA-MM-JJ, liste), nombre, période, case, pièce jointe. */
export type ValeurFormulaire = string | number | boolean | Periode | PieceJointe | null
/** Valeur rendue avec le champ (instance, visible à chaque étape). */
export type ValeurChampFormulaire = ChampFormulaire & { valeur: ValeurFormulaire }
/** Bornes du formulaire (§2), annoncées avant l'envoi ; le service refait tout (422 et fieldErrors). */
export const LIMITES_FORMULAIRE = { champs: 30, texte: 500, texteLong: 4000, aide: 500, parametres: 20 } as const

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
  /** D-98 : absents d'un service antérieur. */
  initiateurs?: Initiateurs
  nombreChampsFormulaire?: number
}

/** « Lancer une demande » (GET /processus/lancables) : versions publiées que je peux lancer, par unité propriétaire. */
export type Lancables = { groupes: { unite: string; processus: ResumeProcessus[] }[] }

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
  /** D-98 : qui peut lancer, formulaire de lancement (absents d'un service antérieur : défauts). */
  initiateurs?: Initiateurs
  formulaire?: Formulaire
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
  initiateurs?: Initiateurs
  formulaire?: Formulaire
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
  /** Étape à prendre : unité nulle si elle est offerte hors de l'unité, à tous les porteurs du profil dans le GIMAC. */
  aPrendre: { unite: string | null; profil: string | null } | null
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
  /** D-98 : valeurs du formulaire de lancement, visibles à chaque étape. */
  formulaire?: ValeurChampFormulaire[]
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
  /**
   * Étapes en cours ; D-98 : `moi` (actions permises à l'appelant, calculées par le serveur) et `attendMoi` (l'étape attend
   * sa décision : exécuter, prendre, signer ou valider). Absents d'un service antérieur.
   */
  etapesEnCours: { code: string; libelle: string; etat: EtatEtape; echeance: string | null; moi?: EtapeInstance['moi']; attendMoi?: boolean }[]
}

export type PageInstances = { contenu: ResumeInstance[]; total: number; page: number; taille: number }

/** Étape d'une demande vue de l'initiateur (« Mes demandes ») : état et personne qui la tient. */
export type EtapeDemande = {
  code: string
  libelle: string
  etat: EtatEtape
  executant: PersonneActivite | null
  aPrendre: { unite: string | null; profil: string | null } | null
  activeeLe: string | null
  echeance: string | null
  termineeLe: string | null
  enRetard: boolean
}
/** « Mes demandes » (GET /processus/demandes, D-98 §3) : instances lancées par moi. */
export type DemandeProcessus = {
  id: string
  processus: { id: string; code: string; version: number; libelle: string }
  unite: string
  intitule: string
  etat: EtatInstance
  lanceeLe: string
  closeLe: string | null
  motif: string | null
  avancement: { total: number; terminees: number }
  etapes: EtapeDemande[]
  derniereEvolution: HistoriqueProcessus | null
}
export type PageDemandesProcessus = { contenu: DemandeProcessus[]; total: number; page: number; taille: number }

/**
 * POST /processus/{id}/instances : clés des désignations CODE_ETAPE, CODE_ETAPE.validateur, CODE_ETAPE.signature.CODE ;
 * `formulaire` (D-98) : valeurs du formulaire de lancement par code (pièce jointe : { documentId }).
 */
export type Lancement = {
  intitule: string
  champs: Record<string, string | number | null>
  designations: Record<string, string>
  formulaire?: Record<string, string | number | boolean | Periode | { documentId: string } | null>
}

/** Étapes en cours d'une demande (active, à signer, à valider). */
export const etapesEnCours = <T extends { etat: EtatEtape }>(etapes: T[]) => etapes.filter((e) => e.etat === 'ACTIVE' || e.etat === 'A_SIGNER' || e.etat === 'A_VALIDER')

export type DecisionValidation = 'VALIDER' | 'RENVOYER'

/** Porteur actif d'un profil proposé pour un intervenant (désignation hors de l'unité) ; `unite` : code, null sans rattachement. */
export type CandidatProcessus = { id: string; nomAffiche: string; fonction: string | null; unite: string | null }
/**
 * Candidats d'un intervenant (GET /processus/candidats, GET …/etapes/{code}/candidats) : porteurs actifs du profil dans
 * l'unité (sous-unités comprises), puis ailleurs dans le GIMAC ; sans profil : membres actifs de l'unité seulement.
 */
/** `exclus` (D-99) : comptes jamais proposés (initiateur de l'instance), à écarter aussi de l'annuaire. */
export type CandidatsProcessus = { unite: string; profil: string | null; dansUnite: CandidatProcessus[]; ailleurs: CandidatProcessus[]; exclus?: string[] }

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

/**
 * Champs saisis au lancement : ceux des étapes initiales (sans prédécesseur) que le lanceur exécute lui-même. D-98 : une
 * étape initiale confiée au service destinataire garde ses champs pour son exécutant (le lanceur remplit le formulaire).
 */
export const champsDeLancement = (etapes: EtapeProcessus[]) => etapes.filter((e) => e.predecesseurs.length === 0 && e.intervenant.mode === 'LANCEUR').flatMap((e) => e.champs)

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

/**
 * Portée de l'appelant sur les processus d'unité (GET /processus/portee, D-92) : TOUS_DOMAINES (REF_METIER_ADMIN, ou
 * PROCESSUS_DEFINIR / PROCESSUS_LANCER à la portée TOUTES) ou MES_UNITES ; `unites` et `unitesDefinissables` ne sont
 * remplies qu'en MES_UNITES.
 */
export type PorteeProcessus = {
  portee: 'TOUS_DOMAINES' | 'MES_UNITES'
  definirToutesUnites: boolean
  lancerToutesUnites: boolean
  auTitreDe: string[]
  unites: string[]
  unitesDefinissables: string[]
}
