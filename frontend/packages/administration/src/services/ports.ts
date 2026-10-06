import type { SessionCompte } from '@gimac/compte'
import type { SignPurpose } from '@gimac/socle'
import type { Demande, EntreePouvoirs, EtatDemande, MatricePouvoirs, PageDemandes, PouvoirsProfil, Unite } from '@gimac/lot1'
import type { ActionRequise, Compte, Creation, DemandeRattachement, EntreeJournal, FicheCompte, FiltreComptes, FiltreJournal, IndicateursComptes, NouveauCompte, Page } from '../modele/comptes'
import type { ConfigurationMessagerie, PolitiqueSecurite, Profil } from '../modele/configuration'
import type { DemandeGeste, EtatInfrastructure, EvenementCharge, Geste, JournauxCharge, NoeudCluster, Operation, PageOperations, PodCharge, TacheCluster } from '../modele/infrastructure'
import type {
  ChangementJournalisation, ConfigurationPlateforme, DemandeNiveau, EtatAlertes, EtatFlux, EtatServeurs, ImpulsionPlateforme, InstantanePlateforme,
  MetriqueSerie, NiveauxJournalisation, PeriodeSerie, SeriesMetriques,
} from '../modele/plateforme'

// Ports de la console (D-62) : seul contrat entre les écrans et les données. Implémentations : HTTP
// (passerelle /bff/console/iam et /api/plateforme) et simulée (développement des écrans). Les règles
// (séparation des pouvoirs, profils privilégiés, planchers de la politique) sont décidées par les services.

/** Assertion FIDO2 (IAM-07), transmise telle quelle dans X-Reauth-Assertion. */
export type Assertion = string

export interface ServiceComptes {
  lister(f: FiltreComptes): Promise<Page<Compte>>
  indicateurs(): Promise<IndicateursComptes>
  journal(f: FiltreJournal): Promise<Page<EntreeJournal>>
  lire(id: string): Promise<Compte>
  sessions(id: string): Promise<SessionCompte[]>
  creer(n: NouveauCompte): Promise<Creation>
  modifier(id: string, f: FicheCompte): Promise<Compte>
  /** Acte sensible : réauthentification forte exigée (ADMIN_SENSITIVE). */
  definirProfils(id: string, profils: string[], motif: string, assertion: Assertion): Promise<Compte>
  activer(id: string, motif: string): Promise<Compte>
  desactiver(id: string, motif: string): Promise<Compte>
  /** Acte sensible : réauthentification forte exigée (ADMIN_SENSITIVE). */
  supprimer(id: string, motif: string, assertion: Assertion): Promise<void>
  exigerActions(id: string, actions: ActionRequise[], parCourriel: boolean, motif: string): Promise<Compte>
  reinitialiserSecondFacteur(id: string, motif: string): Promise<Compte>
  motDePasseProvisoire(id: string, motif: string): Promise<{ motDePasseProvisoire: string }>
  fermerSessions(id: string, motif: string): Promise<void>
}

/**
 * Organisation du GIMAC (D-76, docs/lot1/mon-activite.md §1) : catalogue des unités organisationnelles de MS-IAM
 * et rattachement d'un compte (unité, responsable n+1). Refus : ORG_UNITE_INVALIDE, ORG_UNITE_EXISTANTE,
 * ORG_UNITE_INCONNUE, ORG_CYCLE, ORG_UNITE_SOUS_UNITES_ACTIVES, ORG_RESPONSABLE_INVALIDE, ORG_COMPTE_EXTERNE.
 */
export interface ServiceOrganisation {
  unites(): Promise<Unite[]>
  creerUnite(u: Unite): Promise<Unite>
  /** Libellé, type, parent ou état (désactivation) ; le code ne change pas. */
  modifierUnite(code: string, u: Unite): Promise<Unite>
  /** Rend la fiche du compte complétée de son rattachement. */
  rattacher(id: string, r: DemandeRattachement): Promise<Compte>
}

export interface ServiceConfiguration {
  politique(): Promise<PolitiqueSecurite>
  /** Acte sensible : réauthentification forte exigée (ADMIN_SENSITIVE). */
  appliquerPolitique(p: PolitiqueSecurite, motif: string, assertion: Assertion): Promise<PolitiqueSecurite>
  messagerie(): Promise<ConfigurationMessagerie>
  appliquerMessagerie(m: ConfigurationMessagerie, motif: string): Promise<ConfigurationMessagerie>
  testerMessagerie(destinataire: string): Promise<void>
  profils(): Promise<Profil[]>
  permissions(): Promise<string[]>
  /** Acte sensible : réauthentification forte exigée (ADMIN_SENSITIVE). */
  definirPermissions(code: string, permissions: string[], motif: string, assertion: Assertion): Promise<Profil>
  /**
   * Profil créé par l'administration (D-81 §5) : code ^ROLE_[A-Z][A-Z0-9_]{2,40}$, privilégié s'il porte une
   * permission IAM_*, PLATFORM_* ou INTERNAL_*. Acte sensible (IAM_PROFILE_EDIT, ADMIN_SENSITIVE).
   */
  creerProfil(p: NouveauProfil, motif: string, assertion: Assertion): Promise<Profil>
  /** Profil créé, sans titulaire ; un profil livré est refusé (409 IAM_PROFIL_SYSTEME). Acte sensible. */
  supprimerProfil(code: string, motif: string, assertion: Assertion): Promise<void>
  /**
   * Libellé (1 à 80) et description (0 à 500) d'un profil, livré ou créé (D-86 §2, PUT /configuration/profils/{code}) :
   * IAM_PROFILE_EDIT, motif obligatoire et réauthentification FIDO2 ; profil privilégié : IAM_ROLE_PRIVILEGED_GRANT.
   */
  modifierProfil(code: string, p: PresentationProfil, motif: string, assertion: Assertion): Promise<Profil>
}

/** POST /configuration/profils (D-81 §5) ; libellé facultatif (D-86 §5, le code à défaut). */
export type NouveauProfil = { code: string; libelle?: string; description: string; permissions: string[] }
/** PUT /configuration/profils/{code} (D-86 §2), sans le motif. */
export type PresentationProfil = { libelle: string; description: string }

/**
 * Pouvoirs hiérarchiques côté administration (D-81 §3 et §4) : matrice des pouvoirs (lecture IAM_ACCOUNT_READ,
 * modification IAM_PROFILE_EDIT et FIDO2) et demandes des chefs à traiter (acceptation avec la permission de l'acte
 * et FIDO2, refus motivé). Refus : ORG_DEMANDE_TRAITEE, ORG_DEMANDE_INTERDITE, IAM_PRIVILEGED_PROFILE, IAM_REAUTH_*…
 */
export interface ServicePouvoirsAdministration {
  matrice(): Promise<MatricePouvoirs>
  /** Acte sensible : réauthentification forte exigée (ADMIN_SENSITIVE). */
  modifierMatrice(profil: string, e: EntreePouvoirs, motif: string, assertion: Assertion): Promise<PouvoirsProfil>
  /** Vue « a-traiter » ; pages à partir de 0 comme le service. */
  demandes(q: { etat?: EtatDemande; page: number; taille: number }): Promise<PageDemandes>
  demande(id: string): Promise<Demande>
  /** Acte sensible : l'acte demandé est exécuté par MS-IAM avec ses règles habituelles. */
  accepter(id: string, motif: string, assertion: Assertion): Promise<Demande>
  refuser(id: string, motif: string): Promise<Demande>
}

export type EcouteurFlux = {
  onImpulsion: (i: ImpulsionPlateforme) => void
  onEtat: (e: EtatFlux) => void
  /** Événement « operation-infrastructure » (D-78) : une opération, à sa demande puis à son issue. */
  onOperation?: (o: Operation) => void
}

export interface ServicePlateforme {
  services(): Promise<InstantanePlateforme>
  serveurs(): Promise<EtatServeurs>
  alertes(): Promise<EtatAlertes>
  configuration(): Promise<ConfigurationPlateforme>
  journalisation(service: string, filtre?: string): Promise<NiveauxJournalisation>
  modifierJournalisation(service: string, d: DemandeNiveau): Promise<ChangementJournalisation>
  /** Flux temps réel (SSE) : renvoie la fonction de fermeture. */
  flux(e: EcouteurFlux): () => void
  /** Historique d'une métrique (graphiques) ; cible : service, conteneur ou hôte. */
  series(metrique: MetriqueSerie, periode: PeriodeSerie, cible?: string): Promise<SeriesMetriques>
}

/**
 * Exploitation de l'infrastructure (D-78) : charges de travail Docker ou Kubernetes du périmètre, journaux,
 * historique et gestes. Chaque geste exige PLATFORM_OPS_EXECUTE, un motif et une assertion FIDO2
 * (X-Reauth-Assertion) ; refus OPS_* (docs/exploitation/operations-infrastructure.md §5).
 */
export interface ServiceInfrastructure {
  charges(): Promise<EtatInfrastructure>
  journaux(nom: string, lignes: number, depuis: string): Promise<JournauxCharge>
  operations(page: number, taille: number): Promise<PageOperations>
  /** Acte sensible : 202, opération EN_COURS, issue poussée par le flux. */
  executer(nom: string, geste: Geste, d: DemandeGeste, assertion: Assertion): Promise<Operation>
  /** Opérations poussées par le flux temps réel ; renvoie la fonction de fermeture. */
  flux(e: { onOperation: (o: Operation) => void; onEtat: (e: EtatFlux) => void }): () => void
  /** Lectures Kubernetes (sous Docker : 422 OPS_NON_PRIS_EN_CHARGE). */
  pods(nom: string): Promise<PodCharge[]>
  evenements(nom: string): Promise<EvenementCharge[]>
  noeuds(): Promise<NoeudCluster[]>
  taches(): Promise<TacheCluster[]>
}

export interface ServiceReauthentification {
  /** Lève ReauthCancelled si l'utilisateur annule ; aucune requête d'effet n'est alors envoyée. */
  assertion(purpose: SignPurpose): Promise<Assertion>
}

export interface ServicesConsole {
  comptes: ServiceComptes
  organisation: ServiceOrganisation
  configuration: ServiceConfiguration
  pouvoirs: ServicePouvoirsAdministration
  plateforme: ServicePlateforme
  infrastructure: ServiceInfrastructure
  reauth: ServiceReauthentification
}
