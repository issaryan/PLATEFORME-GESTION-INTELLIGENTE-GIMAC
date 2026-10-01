import { estApiError } from '../http/errors'

// Politique des mots de passe (D-62, D-65) : la plateforme décide (MS-IAM et Keycloak) ; l'interface
// rappelle les planchers et affiche les violations renvoyées par IAM_PASSWORD_POLICY. Le contrôle local
// ne sert qu'aux services simulés.

/** Plancher non négociable de la longueur (D-62). */
export const LONGUEUR_MIN_MOT_DE_PASSE = 12

export type Violation = 'LONGUEUR' | 'MAJUSCULE' | 'MINUSCULE' | 'CHIFFRE' | 'SPECIAL' | 'IDENTIFIANT'

/** Violations d'un mot de passe au regard des planchers de la plateforme (services simulés). */
export function violationsMotDePasse(mdp: string, identifiant?: string): Violation[] {
  const v: Violation[] = []
  if (mdp.length < LONGUEUR_MIN_MOT_DE_PASSE) v.push('LONGUEUR')
  if (!/[A-Z]/.test(mdp)) v.push('MAJUSCULE')
  if (!/[a-z]/.test(mdp)) v.push('MINUSCULE')
  if (!/[0-9]/.test(mdp)) v.push('CHIFFRE')
  if (!/[^A-Za-z0-9]/.test(mdp)) v.push('SPECIAL')
  if (identifiant && mdp.toLowerCase().includes(identifiant.toLowerCase())) v.push('IDENTIFIANT')
  return v
}

/** Violations portées par une erreur IAM_PASSWORD_POLICY (details.violations), liste vide sinon. */
export function violationsDe(e: unknown): string[] {
  if (!estApiError(e) || e.code !== 'IAM_PASSWORD_POLICY') return []
  const v = e.details?.violations
  return Array.isArray(v) ? v.map(String) : []
}
