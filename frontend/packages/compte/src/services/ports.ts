import type { CleSecurite } from '@gimac/socle'
import type { Appareil, ChangementMotDePasse, ConfirmationTotp, EnrolementTotp, FichePersonnelle, Moi, PageHistorique, PremiereConfiguration, SessionCompte, VersionPhoto } from '../modele'

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
  /** PUT /moi/avatar (multipart « image ») ; 400 IAM_AVATAR_TYPE, IAM_AVATAR_TOO_LARGE, IAM_AVATAR_DIMENSIONS, IAM_AVATAR_UNREADABLE. */
  changerPhoto(image: Blob): Promise<VersionPhoto>
  /** DELETE /moi/avatar : retour aux initiales. */
  retirerPhoto(): Promise<void>
  /** Appareils connectés : sessions ouvertes de la passerelle (GET /api/session/appareils). */
  appareils(): Promise<Appareil[]>
  /** Ferme une autre session ; 409 CNX_SESSION_COURANTE pour la session courante (utiliser la déconnexion). */
  fermerAppareil(id: string): Promise<void>
  /** Ferme toutes les autres sessions du compte. */
  fermerAutresAppareils(): Promise<void>
  /** Historique des connexions (90 jours), page à partir de 0. */
  historique(page: number, taille: number): Promise<PageHistorique>
}
