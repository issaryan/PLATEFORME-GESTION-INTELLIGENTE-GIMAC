import { useTranslation } from 'react-i18next'
import { LONGUEUR_MIN_MOT_DE_PASSE } from './politique'

// Rappel de la politique des mots de passe et violations renvoyées par IAM_PASSWORD_POLICY (D-65), communs
// à l'écran du nouveau mot de passe, au premier accès et à « Mon compte ».

// Messages du fournisseur d'identité ramenés aux violations de la plateforme.
const ALIAS: Record<string, string> = {
  invalidPasswordMinLengthMessage: 'LONGUEUR',
  invalidPasswordMinUpperCaseCharsMessage: 'MAJUSCULE',
  invalidPasswordMinLowerCaseCharsMessage: 'MINUSCULE',
  invalidPasswordMinDigitsMessage: 'CHIFFRE',
  invalidPasswordMinSpecialCharsMessage: 'SPECIAL',
  invalidPasswordNotUsernameMessage: 'IDENTIFIANT',
  invalidPasswordNotEmailMessage: 'COURRIEL',
  invalidPasswordHistoryMessage: 'HISTORIQUE',
}

/** Libellé d'une violation (code de la plateforme ou message du fournisseur d'identité), brut à défaut. */
export function useLibelleViolation() {
  const { t } = useTranslation()
  return (v: string) => t(`motDePasse.violation.${ALIAS[v] ?? v}`, { defaultValue: v, n: LONGUEUR_MIN_MOT_DE_PASSE })
}

/** Rappel de la politique, sous le champ du nouveau mot de passe. */
export function RappelPolitique({ id }: { id?: string }) {
  const { t } = useTranslation()
  return (
    <p id={id} className="gm-field-help" style={{ margin: 0 }}>
      {t('motDePasse.politique', { n: LONGUEUR_MIN_MOT_DE_PASSE })}
    </p>
  )
}

/** Violations de la politique, en liste. */
export function ListeViolations({ violations }: { violations: string[] }) {
  const libelle = useLibelleViolation()
  if (violations.length === 0) return null
  return (
    <ul style={{ margin: 0, paddingInlineStart: 'var(--gm-space-5)' }}>
      {violations.map((v) => <li key={v}>{libelle(v)}</li>)}
    </ul>
  )
}
