import type { CleSecurite } from '@gimac/socle'
import type { ChangementMotDePasse, ConfirmationTotp, EnrolementTotp, FichePersonnelle, Moi, PremiereConfiguration, SessionCompte } from '../modele'

// Port de « Mon compte » (D-62, D-65) : seul contrat entre les écrans et les données. Implémentations :
// HTTP (/bff/<couche>/iam/moi, relayé par la passerelle vers MS-IAM) et simulée (développement).
// Les règles (politique des mots de passe, téléphone, langue, charte, code TOTP) sont décidées par MS-IAM.
// Tout se fait sur la plateforme : aucune page du fournisseur d'identité n'est ouverte.
export interface ServiceMonCompte {
  moi(): Promise<Moi>
  premiereConfiguration(p: PremiereConfiguration): Promise<Moi>
  modifier(f: FichePersonnelle): Promise<Moi>
  /** 204 ; 400 IAM_PASSWORD_INVALID (actuel faux), IAM_PASSWORD_POLICY (violations), IAM_PASSWORD_REUSED. */
  changerMotDePasse(c: ChangementMotDePasse): Promise<void>
  /** Nouveau secret TOTP (base32) et son URI otpauth, gardés 10 minutes côté MS-IAM. */
  enrolerTotp(): Promise<EnrolementTotp>
  /** 204 ; 400 IAM_TOTP_CODE_INVALID, IAM_TOTP_ENROLMENT_EXPIRED ; remplace le code existant. */
  confirmerTotp(c: ConfirmationTotp): Promise<void>
  sessions(): Promise<SessionCompte[]>
  fermerSession(id: string): Promise<void>
  fermerSessions(): Promise<void>
  cles(): Promise<CleSecurite[]>
  /** Défi, navigator.credentials.create, enregistrement ; ReauthCancelled si l'utilisateur annule. */
  enrolerCle(nom: string): Promise<CleSecurite>
  supprimerCle(id: string): Promise<void>
}
