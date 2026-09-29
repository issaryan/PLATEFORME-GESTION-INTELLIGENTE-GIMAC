import type { CleSecurite } from '@gimac/socle'
import type { ActionLibreService, FichePersonnelle, Moi, PremiereConfiguration, SessionCompte } from '../modele'

// Port de « Mon compte » (D-62) : seul contrat entre les écrans et les données. Implémentations :
// HTTP (/bff/<couche>/iam/moi, relayé par la passerelle vers MS-IAM) et simulée (développement).
// Les règles (téléphone, langue, charte, actions permises) sont décidées par MS-IAM.
export interface ServiceMonCompte {
  moi(): Promise<Moi>
  premiereConfiguration(p: PremiereConfiguration): Promise<Moi>
  modifier(f: FichePersonnelle): Promise<Moi>
  /** Inscrit l'action pour la prochaine authentification ; l'interface reconnecte ensuite l'utilisateur. */
  demanderAction(action: ActionLibreService): Promise<Moi>
  sessions(): Promise<SessionCompte[]>
  fermerSession(id: string): Promise<void>
  fermerSessions(): Promise<void>
  cles(): Promise<CleSecurite[]>
  /** Défi, navigator.credentials.create, enregistrement ; ReauthCancelled si l'utilisateur annule. */
  enrolerCle(nom: string): Promise<CleSecurite>
  supprimerCle(id: string): Promise<void>
  /** Nouvelle authentification : le fournisseur d'identité exécute les actions requises sur ses pages GIMAC. */
  reconnecter(): Promise<void>
}
