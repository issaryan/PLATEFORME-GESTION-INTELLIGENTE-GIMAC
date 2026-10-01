import type { Couche } from '../http/prefixes'
import type { Session } from '../session/session'

// Connexion sur la plateforme (D-65, docs/lot1/connexion.md §1 et §2) : formes des requêtes et des
// réponses de POST /api/connexion et des routes du mot de passe oublié.

/** Étapes du premier accès annoncées par la passerelle (actions exigées tenues par MS-IAM). */
export type EtapePremierAcces = 'MOT_DE_PASSE' | 'SECOND_FACTEUR' | 'FICHE' | 'CHARTE' | 'VERIFIER_COURRIEL'

export type DemandeConnexion = {
  identifiant: string
  motDePasse: string
  /** Code TOTP à six chiffres, second temps de la connexion. */
  code?: string
  couche: Couche
}

export type ReponseConnexion =
  | { etat: 'CONNECTE'; session: Session }
  | { etat: 'CODE_REQUIS' }
  | { etat: 'PREMIER_ACCES'; etapes: string[] }

/** Codes d'erreur de la connexion (problème RFC 9457, champ `code`). */
export const CODES_CONNEXION = [
  'CNX_IDENTIFIANTS_INVALIDES', 'CNX_COUCHE_REFUSEE', 'CNX_COMPTE_INACTIF', 'CNX_TROP_DE_TENTATIVES',
  'CNX_JETON_INVALIDE', 'CNX_SESSION_EXPIREE', 'IAM_PASSWORD_POLICY',
] as const

// Port de la connexion : seul contrat entre l'écran de connexion (et la reconnexion de fin de premier
// accès) et la passerelle. Implémentations HTTP et simulée.
export interface ServiceConnexion {
  connecter(d: DemandeConnexion): Promise<ReponseConnexion>
  /** 202 toujours : l'existence d'un compte n'est jamais révélée. */
  motDePasseOublie(identifiant: string): Promise<void>
  /** Lien du courriel (jeton à usage unique) : 204, 400 CNX_JETON_INVALIDE ou IAM_PASSWORD_POLICY. */
  nouveauMotDePasse(jeton: string, motDePasse: string): Promise<void>
}
