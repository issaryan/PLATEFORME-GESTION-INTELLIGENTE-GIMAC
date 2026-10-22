import { useTranslation } from 'react-i18next'
import { StatusBadge, type Status } from '@gimac/design-system'
import { ApiError } from '@gimac/socle'
import type { EtatEspace } from '../modele'

// Éléments communs de l'espace sandbox (D-122) : préfixe de traduction « sbx. », refus traduits par leur code
// (problèmes RFC 9457 de SBX-ESPACE), formats de date dans la langue de l'interface.

export type MessageErreur = { title: string; text?: string; correlationId?: string }

/** Refus du service traduit par son code ; message du service à défaut de traduction. */
export function useMessageErreur() {
  const { t, i18n } = useTranslation()
  return (e: unknown): MessageErreur => {
    if (e instanceof ApiError) {
      const cle = `sbx.erreur.${e.code}`
      const connu = i18n.exists(cle)
      return { title: connu ? t(cle) : t('sbx.erreur.generique'), text: connu ? undefined : e.message || undefined, correlationId: e.correlationId || undefined }
    }
    return { title: t('sbx.erreur.generique') }
  }
}

export function useFormat() {
  const { i18n } = useTranslation()
  const langue = i18n.language
  return { instant: (iso: string | null) => (iso ? new Date(iso).toLocaleString(langue, { dateStyle: 'short', timeStyle: 'short', timeZone: 'UTC' }) + ' UTC' : '') }
}

const STATUT: Record<EtatEspace, Status> = { EN_OUVERTURE: 'info', OUVERT: 'ok', SUSPENDU: 'warn', FERME: 'neutral' }

export function BadgeEspace({ etat }: { etat: EtatEspace }) {
  const { t } = useTranslation()
  return <StatusBadge status={STATUT[etat]} label={t(`sbx.espace.etat.${etat}`)} />
}

/** État d'une liste (DataTable) depuis une requête. */
export function etatListe(q: { isPending: boolean; isError: boolean }, lignes: unknown[]): { state: 'loading' | 'error' | 'empty' | 'ok' } {
  if (q.isPending) return { state: 'loading' }
  if (q.isError) return { state: 'error' }
  return { state: lignes.length ? 'ok' : 'empty' }
}
