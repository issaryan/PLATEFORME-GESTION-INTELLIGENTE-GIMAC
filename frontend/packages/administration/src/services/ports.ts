import type { SessionCompte } from '@gimac/compte'
import type { SignPurpose } from '@gimac/socle'
import type { ActionRequise, Compte, Creation, EntreeJournal, FicheCompte, FiltreComptes, FiltreJournal, IndicateursComptes, NouveauCompte, Page } from '../modele/comptes'
import type { ConfigurationMessagerie, PolitiqueSecurite, Profil } from '../modele/configuration'
import type {
  ChangementJournalisation, ConfigurationPlateforme, DemandeNiveau, EtatAlertes, EtatFlux, EtatServeurs, ImpulsionPlateforme, InstantanePlateforme,
  NiveauxJournalisation,
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
}

export type EcouteurFlux = { onImpulsion: (i: ImpulsionPlateforme) => void; onEtat: (e: EtatFlux) => void }

export interface ServicePlateforme {
  services(): Promise<InstantanePlateforme>
  serveurs(): Promise<EtatServeurs>
  alertes(): Promise<EtatAlertes>
  configuration(): Promise<ConfigurationPlateforme>
  journalisation(service: string, filtre?: string): Promise<NiveauxJournalisation>
  modifierJournalisation(service: string, d: DemandeNiveau): Promise<ChangementJournalisation>
  /** Flux temps réel (SSE) : renvoie la fonction de fermeture. */
  flux(e: EcouteurFlux): () => void
}

export interface ServiceReauthentification {
  /** Lève ReauthCancelled si l'utilisateur annule ; aucune requête d'effet n'est alors envoyée. */
  assertion(purpose: SignPurpose): Promise<Assertion>
}

export interface ServicesConsole {
  comptes: ServiceComptes
  configuration: ServiceConfiguration
  plateforme: ServicePlateforme
  reauth: ServiceReauthentification
}
