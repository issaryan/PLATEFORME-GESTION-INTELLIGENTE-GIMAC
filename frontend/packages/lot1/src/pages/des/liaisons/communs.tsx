import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError, EtatsRequete, aPermission, estApiError } from '@gimac/socle'

// Compositions communes des écrans DES-08 et DES-09 : les quatre états d'une donnée, traduits.

type Requete<R> = { isPending: boolean; isError: boolean; error: ApiError | null; data: R | undefined; refetch: () => unknown }

/** Chargement, vide (avec sa cause), erreur (identifiant de corrélation et réessai), nominal. */
export function Donnees<R>({ requete, vide, estVide, children }: { requete: Requete<R>; vide?: string; estVide?: (r: R) => boolean; children: (r: R) => ReactNode }) {
  const { t } = useTranslation()
  return (
    <EtatsRequete etat={requete} isEmpty={estVide} emptyTitle={vide ?? t('commun.vide')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {children}
    </EtatsRequete>
  )
}

/** État d'une requête de liste pour ResourceList et DataTable. */
export function etatListe<R>(q: Requete<R>, lignes: unknown[] | undefined): { state: 'loading' | 'error' | 'empty' | 'ok'; error?: { message: string; correlationId?: string } } {
  if (q.isPending) return { state: 'loading' }
  if (q.isError) return { state: 'error', error: { message: q.error?.message ?? '', correlationId: q.error?.correlationId || undefined } }
  return { state: lignes && lignes.length ? 'ok' : 'empty' }
}

/** Valeurs des détails du refus pour le message traduit ; un statut d'institution est lui-même traduit. */
function valeursDetails(t: (k: string, o?: Record<string, unknown>) => string, details: ApiError['details']): Record<string, unknown> {
  const v: Record<string, unknown> = { ...(details ?? {}) }
  if ('statut' in v) v.statut = typeof v.statut === 'string' ? t(`institution.statut.${v.statut}`, { defaultValue: v.statut }) : t('erreur.statutInconnu')
  return v
}

/** Erreur du service pour le formulaire : message traduit par son code (détails interpolés), identifiant de corrélation conservé. */
export function useErreurService() {
  const { t } = useTranslation()
  return (e: unknown): ApiError | undefined => {
    if (!e) return undefined
    if (estApiError(e)) {
      const msg = t(`erreur.${e.code}`, { ...valeursDetails(t, e.details), defaultValue: e.message || t('commun.erreur.titre') })
      return new ApiError(e.status, e.code, msg, e.correlationId, e.details)
    }
    return undefined
  }
}

/** « Mes projets » activé d'emblée pour un conducteur de projet qui ne désigne pas (D-58). */
export function mesProjetsParDefaut(roles: string[] | undefined, permissions: string[] | undefined): boolean {
  return !!roles?.includes('ROLE_CONDUCTEUR_PROJET') && !aPermission(permissions, 'HOM_PROJECT_DESIGNATE')
}
