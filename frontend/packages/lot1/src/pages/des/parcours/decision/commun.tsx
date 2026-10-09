import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { StatusBadge, type Status } from '@gimac/design-system'
import { aPermission, usePermissions, type ResourceActionLot1 } from '@gimac/socle'
import type { Signature } from '../../../../modele/parcours'
import type { Services } from '../../../../services/ports'
import { useExecuteurActions } from '../../../../ressources/actions'
import { PERM } from '../../../../permissions'
import { useFormatDate } from '../communs'

// Briques communes des onglets « Décision » et « Mise en service » (D-56) : une action n'est proposée que
// si le service la permet (actionsPermises) ET si le jeton porte la permission ; les dialogues (confirmation,
// motif, signature FIDO2), le retour (Toast) et les refus du service sont ceux d'ExecuteurActions.

export type Action<T> = ResourceActionLot1<T, Services>

/** Permissions exigées par geste. Trois gestes n'ont pas encore de permission dédiée au backend (voir correspondance). */
export const PERMISSION_GESTE = {
  etablirPv: [PERM.pvEtablir],
  signerPvGimac: [PERM.pvSignerGimac],
  constituerDossier: [PERM.ficheChangementLier],
  referencerFiche: [PERM.ficheChangementLier],
  relireFiche: [PERM.ficheChangementLier, PERM.securiteVerifier],
  verifierAttestation: [PERM.securiteVerifier],
  archiverAttestations: [PERM.securiteVerifier],
  visaConformite: [PERM.visaConformite],
  visaDg: [PERM.visaDg],
  // Remise de la fiche de suivi : Normes et Agréments ou le conducteur (conduite de l'étape 10), comme MS-HOMOLOGATION.
  remettreFiche: [PERM.dossierVerifier, PERM.etapeConduire],
  constaterMiseEnService: [PERM.dossierVerifier],
  ouvrirServiceSupplementaire: [PERM.liaisonOuvrir],
} as const

export type Geste = keyof typeof PERMISSION_GESTE

/** Vrai si le service permet l'action et si le jeton porte l'une des permissions du geste. */
export function usePeut() {
  const permissions = usePermissions()
  return (actionsPermises: string[], action: string, geste: Geste) => actionsPermises.includes(action) && aPermission(permissions, ...PERMISSION_GESTE[geste])
}

/** Vrai si le jeton porte le geste, indépendamment de l'état (pour expliquer une action bloquée). */
export function useDetient() {
  const permissions = usePermissions()
  return (geste: Geste) => aPermission(permissions, ...PERMISSION_GESTE[geste])
}

/** Lance une action par ExecuteurActions ; toute action invalide le sujet « parcours ». */
export function useLancer() {
  const executeur = useExecuteurActions()
  return <T,>(action: Action<T>, row: T, titre: string) => executeur.lancer(action, row, titre, 'parcours')
}

/** Titre de section de carte (niveau 3), vocabulaire typographique du système de conception. */
export function SousTitre({ children }: { children: ReactNode }) {
  return <h3 className="gm-type-h3" style={{ margin: 0 }}>{children}</h3>
}

/** Valeur absente : « non renseigné » en aide de champ. */
export function Vide({ texte }: { texte?: string }) {
  const { t } = useTranslation()
  return <span className="gm-field-help">{texte ?? t('commun.nonRenseigne')}</span>
}

/** État d'une signature : signée (signataire, date, horodatage) ou attendue. */
export function EtatSignature({ signature, attente }: { signature: Signature | null; attente: string }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  if (!signature) return <StatusBadge status="neutral" label={attente} />
  return (
    <span className="gm-row">
      <StatusBadge status="ok" label={t('decision.signature.signee')} />
      <span>{t('decision.signature.par', { nom: signature.signataire, date: date(signature.signeLe) })}</span>
    </span>
  )
}

/** Ton d'un état : dictionnaires d'affichage des états du modèle hybride. */
export const TON_PV: Record<string, Status> = { NON_ETABLI: 'neutral', ETABLI: 'warn', SIGNE_GIMAC: 'info', SIGNE_PARTICIPANT: 'info', SIGNE: 'ok' }
export const TON_APPROBATION: Record<string, Status> = { EN_ATTENTE_DOSSIER: 'neutral', DOSSIER_CONSTITUE: 'info', EN_VERIFICATION: 'warn', VERIFIE_ARCHIVE: 'info', APPROUVEE: 'ok', REFUSEE: 'crit' }
export const TON_VISA: Record<string, Status> = { ATTENDU: 'warn', SIGNE_FAVORABLE: 'ok', SIGNE_DEFAVORABLE: 'crit', CADUC: 'neutral' }
export const TON_FICHE_CHANGEMENT: Record<string, Status> = { OUVERTE: 'neutral', EN_EXAMEN: 'info', APPROUVEE: 'ok', REFUSEE: 'crit' }
export const TON_FICHE_SUIVI: Record<string, Status> = { A_REMETTRE: 'neutral', REMISE: 'info', RENSEIGNEE: 'warn', CONSTATEE: 'ok' }
